package org.yamcs.sle;

import static org.yamcs.sle.Constants.CREDENTIALS_UNUSED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import com.beanit.jasn1.ber.BerTag;
import org.yamcs.sle.Constants.ApplicationIdentifier;
import org.yamcs.sle.Constants.ServiceAgreement;
import org.yamcs.sle.Constants.ServicePackage;

import ccsds.sle.transfer.service.bind.types.AuthorityIdentifier;
import ccsds.sle.transfer.service.bind.types.PortId;
import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.SleBindReturn;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindInvocation;
import ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import ccsds.sle.transfer.service.bind.types.UnbindReason;
import ccsds.sle.transfer.service.bind.types.VersionNumber;
import ccsds.sle.transfer.service.bind.types.SleBindReturn.Result;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuUserToProviderPdu;
import ccsds.sle.transfer.service.common.pdus.ReportRequestType;
import ccsds.sle.transfer.service.common.pdus.ReportingCycle;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportInvocation;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.common.types.Time;
import ccsds.sle.transfer.service.common.types.TimeCCSDS;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafUsertoProviderPdu;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceAttribute;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Common class for all SLE services
 * 
 * @author nm
 *
 */
public abstract class AbstractServiceUserHandler extends ChannelInboundHandlerAdapter {
    static public enum State {
        UNBOUND, BINDING, READY, STARTING, ACTIVE, STOPPING, UNBINDING
    };

    static public enum AuthLevel {
        NONE, BIND, ALL;
    }

    protected String serviceAgreementName = "SAGR";
    protected String servicePackageName = "SPACK";

    final Isp1Authentication auth;
    final String initiatorId;
    final String responderPortId;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractServiceUserHandler.class);
    private int invokeId = 1;
    private CompletableFuture<Void> bindingCf;
    protected CompletableFuture<Void> startingCf;
    private CompletableFuture<Void> stoppingCf;
    private CompletableFuture<Void> unbindingCf;

    // the state is only updated from the netty thread. We make it volatile such that we can monitor it from everywhere;
    protected volatile State state = State.UNBOUND;

    protected ChannelHandlerContext channelHandlerContext;

    Map<Integer, CompletableFuture<?>> pendingInvocations = new HashMap<>();

    protected AuthLevel authLevel = AuthLevel.ALL;
   
    int versionNumber = 2;
    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    protected List<SleMonitor> monitors = new CopyOnWriteArrayList<>();

    public AbstractServiceUserHandler(Isp1Authentication auth, String responderPortId, String initiatorId) {
        this.initiatorId = initiatorId;
        this.responderPortId = responderPortId;
        this.auth = auth;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (logger.isTraceEnabled()) {
            logger.trace("received message: {}", msg);
        }
        try {
            InputStream is = new ByteBufInputStream((ByteBuf) msg);
            BerTag berTag = new BerTag();
            berTag.decode(is);
            if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 100)) {
                SleBindInvocation bindInvocation = new SleBindInvocation();
                bindInvocation.decode(is, false);
                processBindInvocation(bindInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 101)) {
                SleBindReturn bindReturn = new SleBindReturn();
                bindReturn.decode(is, false);
                processBindReturn(bindReturn);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 102)) {
                SleUnbindInvocation unbindInvocation = new SleUnbindInvocation();
                unbindInvocation.decode(is, false);
                processUnbindInvocation(unbindInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 103)) {
                SleUnbindReturn unbindReturn = new SleUnbindReturn();
                unbindReturn.decode(is, false);
                processUnbindReturn(unbindReturn);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 104)) {
                SlePeerAbort peerAbortInvocation = new SlePeerAbort();
                peerAbortInvocation.decode(is, false);
                processPeerAbortInvocation(peerAbortInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 3)) {
                SleAcknowledgement stopReturn = new SleAcknowledgement();
                stopReturn.decode(is, false);
                processStopReturn(stopReturn);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 5)) {
                SleScheduleStatusReportReturn scheduleStatusReportReturn = new SleScheduleStatusReportReturn();
                scheduleStatusReportReturn.decode(is, false);
                processScheduleStatusReportReturn(scheduleStatusReportReturn);
            } else {
                processData(berTag, is);
            }
        } catch (IOException e) {
            logger.warn("Error decoding data", e);
            peerAbort();
        }
    }

    protected abstract void processData(BerTag berTag, InputStream is) throws IOException;

    public AuthLevel getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(AuthLevel authLevel) {
        this.authLevel = authLevel;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.channelHandlerContext = ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        monitors.forEach(m -> m.connected());
        ctx.channel().closeFuture().addListener(cf -> notifyDisconnected());
    }

    public String getServiceAgreementName() {
        return serviceAgreementName;
    }

    public void setServiceAgreementName(String serviceAgreementName) {
        checkUnbound();
        this.serviceAgreementName = serviceAgreementName;
    }

    public String getServicePackageName() {
        return servicePackageName;
    }

    public void setServicePackageName(String servicePackageName) {
        checkUnbound();
        this.servicePackageName = servicePackageName;
    }

    /**
     * Establish an association with the provider
     * 
     * @return
     */
    public CompletableFuture<Void> bind() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendBind(cf));
        return cf;
    }

    /**
     * Request that the SLE service provider starts sending data
     * 
     * @return
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStart(cf));
        return cf;
    }

    /**
     * request that the SLE service provider stop service provision and production.
     * 
     * @return
     */
    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStop(cf));
        return cf;
    }

    public CompletableFuture<Void> unbind() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendUnbind(cf));
        return cf;
    }

    /**
     * Send a request to the server to ask for a periodic status report.
     * 
     * @param intervalSec
     *            - the cycle on which the periodic report shall be sent
     * @return
     */
    public CompletableFuture<Void> schedulePeriodicStatusReport(int intervalSec) {
        ReportRequestType rrt = new ReportRequestType();
        rrt.setPeriodically(new ReportingCycle(intervalSec));
        CompletableFuture<Void> cf = new CompletableFuture<Void>();
        channelHandlerContext.executor().execute(() -> sendScheduleStatusReport(rrt, cf));
        return cf;
    }

    /**
     * Send a request to stop the periodic status report.
     * 
     * @return
     */
    public CompletableFuture<Void> stopPeriodicStatusReport() {
        ReportRequestType rrt = new ReportRequestType();
        rrt.setStop(Constants.BER_NULL);
        CompletableFuture<Void> cf = new CompletableFuture<Void>();
        channelHandlerContext.executor().execute(() -> sendScheduleStatusReport(rrt, cf));
        return cf;
    }
    
    public void shutdown() {
        channelHandlerContext.close();
    }

    // this method is not really safe because it's not called on the netty thread
    // but we use it only to verify that the binding attributes are not set after the bind has started
    protected void checkUnbound() {
        if (state != State.UNBOUND) {
            throw new IllegalStateException("This method can only be invoked in the UNBOUND state");
        }
    }

    
    abstract void sendStart(CompletableFuture<Void> cf);

    private void processPeerAbortInvocation(SlePeerAbort peerAbortInvocation) {
        logger.warn("received PEER-ABORT {}", peerAbortInvocation);
        channelHandlerContext.close();
    }

    private void sendBind(CompletableFuture<Void> cf) {
        if (state != State.UNBOUND) {
            cf.completeExceptionally(new SleException("Cannot call bind while in state " + state));
            return;
        }
        changeState(State.BINDING);
        this.bindingCf = cf;

        RafUsertoProviderPdu utp = new RafUsertoProviderPdu();

        SleBindInvocation sbi = new SleBindInvocation();
        sbi.setInitiatorIdentifier(new AuthorityIdentifier(initiatorId.getBytes(StandardCharsets.US_ASCII)));
        if (authLevel == AuthLevel.NONE) {
            sbi.setInvokerCredentials(CREDENTIALS_UNUSED);
        } else {
            sbi.setInvokerCredentials(auth.generateCredentials());
        }
        ServiceInstanceIdentifier sii = getServiceInstanceIdentifier();

        sbi.setServiceInstanceIdentifier(sii);
        sbi.setResponderPortIdentifier(new PortId(responderPortId.getBytes(StandardCharsets.US_ASCII)));
        sbi.setVersionNumber(new VersionNumber(versionNumber));
        ApplicationIdentifier appId = getApplicationIdentifier();
        sbi.setServiceType(
                new ccsds.sle.transfer.service.bind.types.ApplicationIdentifier(appId.getId()));
        logger.info("Sending bind request serviceInstanceIdentifier: {}, versionNumber: {}, appId: {}",
                StringConverter.toString(sii), versionNumber, appId);
        utp.setRafBindInvocation(sbi);
        channelHandlerContext.writeAndFlush(utp);
    }

    protected abstract ApplicationIdentifier getApplicationIdentifier();

    private void processBindReturn(SleBindReturn bindReturn) {
        verifyBindCredentials(bindReturn.getPerformerCredentials());

        if (state != State.BINDING) {
            peerAbort();
            return;
        }
        Result r = bindReturn.getResult();
        if (r.getNegative() != null) {
            changeState(State.UNBOUND);
            bindingCf.completeExceptionally(
                    new SleException("bind failed: " + Constants.BIND_DIAGNOSTIC.get(r.getNegative().intValue())));
        } else {
            changeState(State.READY);
            bindingCf.complete(null);
        }
    }

    protected void processBindInvocation(SleBindInvocation bindInvocation) {
        verifyBindCredentials(bindInvocation.getInvokerCredentials());
        // TODO
    }

    protected void processUnbindInvocation(SleUnbindInvocation unbindInvocation) {
        verifyNonBindCredentials(unbindInvocation.getInvokerCredentials());
        // TODO
    }

    private void sendStop(CompletableFuture<Void> cf) {
        if (state != State.ACTIVE) {
            cf.completeExceptionally(new SleException("Cannot call stop while in state " + state));
            return;
        }
        changeState(State.STOPPING);
        this.stoppingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        SleStopInvocation ssi = new SleStopInvocation();
        ssi.setInvokerCredentials(getNonBindCredentials());
        ssi.setInvokeId(getInvokeId());

        cutp.setCltuStopInvocation(ssi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    protected void processStopReturn(SleAcknowledgement stopReturn) {
        verifyNonBindCredentials(stopReturn.getCredentials());

        if (state != State.STOPPING) {
            peerAbort();
            return;
        }
        ccsds.sle.transfer.service.common.pdus.SleAcknowledgement.Result r = stopReturn.getResult();
        if (r.getNegativeResult() != null) {
            changeState(State.ACTIVE);
            stoppingCf.completeExceptionally(new SleException("stop failed", r));
        } else {
            changeState(State.READY);
            stoppingCf.complete(null);
        }
    }

    private void sendUnbind(CompletableFuture<Void> cf) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call unbind while in state " + state));
            return;
        }
        changeState(State.UNBINDING);
        this.unbindingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        SleUnbindInvocation sui = new SleUnbindInvocation();
        sui.setInvokerCredentials(getNonBindCredentials());
        sui.setUnbindReason(new UnbindReason(127));
        cutp.setCltuUnbindInvocation(sui);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processUnbindReturn(SleUnbindReturn unbindReturn) {
        verifyNonBindCredentials(unbindReturn.getResponderCredentials());

        if (state != State.UNBINDING) {
            peerAbort();
            return;
        }

        changeState(State.UNBOUND);
        unbindingCf.complete(null);
    }

    private void sendScheduleStatusReport(ReportRequestType reportRequestType, CompletableFuture<Void> cf) {
        SleScheduleStatusReportInvocation sssri = new SleScheduleStatusReportInvocation();
        sssri.setInvokerCredentials(getNonBindCredentials());
        sssri.setInvokeId(getInvokeId(cf));
        sssri.setReportRequestType(reportRequestType);
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        cutp.setCltuScheduleStatusReportInvocation(sssri);
        channelHandlerContext.writeAndFlush(cutp);

    }

    private void processScheduleStatusReportReturn(SleScheduleStatusReportReturn sheduleStatusReportReturn) {
        verifyNonBindCredentials(sheduleStatusReportReturn.getPerformerCredentials());

        CompletableFuture<Void> cf = getFuture(sheduleStatusReportReturn.getInvokeId());
        SleScheduleStatusReportReturn.Result r = sheduleStatusReportReturn.getResult();
        if (r.getNegativeResult() != null) {
            cf.completeExceptionally(new SleException("error scheduling status report", r.getNegativeResult()));
        } else {
            cf.complete(null);
        }
    }

    protected void peerAbort() {
        // TODO Auto-generated method stub

    }

    protected Credentials getNonBindCredentials() {
        if (authLevel == AuthLevel.ALL) {
            return auth.generateCredentials();
        } else {
            return CREDENTIALS_UNUSED;
        }
    }

    protected void verifyNonBindCredentials(Credentials credentials) {
        if (authLevel == AuthLevel.ALL) {
            auth.verifyCredentials(credentials);
        }
    }

    protected void verifyBindCredentials(Credentials credentials) {
        if (authLevel == AuthLevel.BIND) {
            auth.verifyCredentials(credentials);
        }
    }

    protected InvokeId getInvokeId() {
        int n = invokeId++ & Short.MAX_VALUE;
        return new InvokeId(n);
    }

    protected ServiceInstanceIdentifier getServiceInstanceIdentifier() {
        ServiceInstanceIdentifier sii = new ServiceInstanceIdentifier();
        List<ServiceInstanceAttribute> l = sii.getServiceInstanceAttribute();
        l.add(ServiceAgreement.sagr.getServiceInstanceAttribute(serviceAgreementName));
        l.add(ServicePackage.spack.getServiceInstanceAttribute(servicePackageName));
        l.add(getServiceFunctionalGroup());
        l.add(getServiceNameIdentifier());
        return sii;
    }

    protected abstract ServiceInstanceAttribute getServiceFunctionalGroup();

    protected abstract ServiceInstanceAttribute getServiceNameIdentifier();

    @SuppressWarnings("unchecked")
    protected <T> CompletableFuture<T> getFuture(InvokeId invokeId) {
        CompletableFuture<T> cf = (CompletableFuture<T>) pendingInvocations.remove(invokeId.intValue());
        if (cf == null) {
            String msg = "Received invokeid " + invokeId.intValue() + " for which I have no pending invocation";
            logger.warn(msg);
            peerAbort();
            throw new SleException(msg);
        }
        return cf;
    }

    protected void changeState(State newState) {
        this.state = newState;
        monitors.forEach(m -> m.stateChanged(newState));
    }

    protected InvokeId getInvokeId(CompletableFuture<?> cf) {
        InvokeId invokeId = getInvokeId();
        pendingInvocations.put(invokeId.intValue(), cf);
        return invokeId;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        monitors.forEach(m -> m.exceptionCaught(cause));
        logger.warn("Caught exception {}", cause.getMessage());
    }

    protected void notifyDisconnected() {
        monitors.forEach(m-> m.disconnected());
    }

    protected static ConditionalTime getConditionalTime(CcsdsTime time) {
        if (time != null) {
            ConditionalTime ct = new ConditionalTime();
            ct.setKnown(getTime(time));
            return ct;
        } else {
            return Constants.COND_TIME_UNDEFINED;
        }
    }

    static protected Time getTime(CcsdsTime time) {
        Time t = new Time();
        t.setCcsdsFormat(new TimeCCSDS(time.getDaySegmented()));
        return t;
    }

}
