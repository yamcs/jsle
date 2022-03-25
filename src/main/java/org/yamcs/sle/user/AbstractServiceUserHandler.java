package org.yamcs.sle.user;

import static org.yamcs.sle.Constants.CREDENTIALS_UNUSED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.beanit.jasn1.ber.BerTag;

import org.yamcs.sle.AuthLevel;
import org.yamcs.sle.Constants;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.ParameterName;
import org.yamcs.sle.SleParameter;
import org.yamcs.sle.SleException;
import org.yamcs.sle.SleMonitor;
import org.yamcs.sle.State;
import org.yamcs.sle.StringConverter;
import org.yamcs.sle.Constants.ApplicationIdentifier;
import org.yamcs.sle.Constants.UnbindReason;

import ccsds.sle.transfer.service.bind.types.AuthorityIdentifier;
import ccsds.sle.transfer.service.bind.types.PortId;
import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.SleBindReturn;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindInvocation;
import ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import ccsds.sle.transfer.service.bind.types.VersionNumber;
import ccsds.sle.transfer.service.bind.types.SleBindReturn.Result;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuUserToProviderPdu;
import ccsds.sle.transfer.service.common.pdus.ReportRequestType;
import ccsds.sle.transfer.service.common.pdus.ReportingCycle;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportInvocation;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafUsertoProviderPdu;
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
    final Isp1Authentication auth;

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

    final protected SleAttributes attr;

    int sleVersion = 4;

    int returnTimeoutSec = 30;

    public int getVersionNumber() {
        return sleVersion;
    }

    public void setVersionNumber(int versionNumber) {
        this.sleVersion = versionNumber;
    }

    protected List<SleMonitor> monitors = new CopyOnWriteArrayList<>();

    public AbstractServiceUserHandler(Isp1Authentication auth, SleAttributes attr) {
        this.auth = auth;
        this.attr = attr;
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

    public AuthLevel getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(AuthLevel authLevel) {
        this.authLevel = authLevel;
    }

    /**
     * Set the maximum time (in seconds) to wait for a return to a call
     */
    public void setReturnTimeoutSec(int returnTimeoutSec) {
        this.returnTimeoutSec = returnTimeoutSec;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.channelHandlerContext = ctx;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        monitors.forEach(m -> m.connected());
        ctx.channel().closeFuture().addListener(cf -> {
            logger.debug("Connection {} closed", ctx.channel().remoteAddress());
            notifyDisconnected();
        });
    }

    /**
     * Get the value of an RAF parameter from the provider
     * 
     * @param parameterId
     *            one of the parameters defined in {@link ParameterName}. Note that not all of them make sense for the
     *            RAF, see the table 3-11 in the standard to see which ones make sense.
     * @return
     */
    public abstract CompletableFuture<SleParameter> getParameter(int parameterId);

    public boolean isConnected() {
        return channelHandlerContext != null && channelHandlerContext.channel().isActive();
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
     * request that the SLE service provider stop service provision and production.
     * 
     * @return
     */
    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStop(cf));
        return cf;
    }

    public CompletableFuture<Void> unbind(UnbindReason reason) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendUnbind(reason, cf));
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

    /**
     * Closes the SLE connection (does not send any unbind or stop first).
     * <p>
     */
    public void shutdown() {
        if (channelHandlerContext != null) {
            channelHandlerContext.close();
        }
    }

    // this method is not really safe because it's not called on the netty thread
    // but we use it only to verify that the binding attributes are not set after the bind has started
    protected void checkUnbound() {
        if (state != State.UNBOUND) {
            throw new IllegalStateException("This method can only be invoked in the UNBOUND state");
        }
    }

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
        sbi.setInitiatorIdentifier(new AuthorityIdentifier(attr.initiatorId.getBytes(StandardCharsets.US_ASCII)));
        if (authLevel == AuthLevel.NONE) {
            sbi.setInvokerCredentials(CREDENTIALS_UNUSED);
        } else {
            sbi.setInvokerCredentials(auth.generateCredentials());
        }
        ServiceInstanceIdentifier sii = StringConverter.parseServiceInstanceIdentifier(attr.serviceInstance);

        sbi.setServiceInstanceIdentifier(sii);
        sbi.setResponderPortIdentifier(new PortId(attr.responderPortId.getBytes(StandardCharsets.US_ASCII)));
        sbi.setVersionNumber(new VersionNumber(sleVersion));
        ApplicationIdentifier appId = getApplicationIdentifier();
        sbi.setServiceType(
                new ccsds.sle.transfer.service.bind.types.ApplicationIdentifier(appId.getId()));
        logger.debug("Sending bind request serviceInstanceIdentifier: {}, versionNumber: {}, appId: {}",
                StringConverter.toString(sii), sleVersion, appId);
        utp.setRafBindInvocation(sbi);
        channelHandlerContext.writeAndFlush(utp);
    }

    private void processBindReturn(SleBindReturn bindReturn) {
        verifyBindCredentials(bindReturn.getPerformerCredentials());

        if (state != State.BINDING) {
            peerAbort();
            return;
        }
        Result r = bindReturn.getResult();
        if (r.getNegative() != null) {
            changeState(State.UNBOUND);
            String reason = Constants.BIND_DIAGNOSTIC.get(r.getNegative().intValue());
            logger.debug("bind failed : {}", reason);
            bindingCf.completeExceptionally(new SleException("bind failed: " + reason));
        } else {
            changeState(State.READY);
            bindingCf.complete(null);
        }
    }

    protected void processBindInvocation(SleBindInvocation bindInvocation) {
        verifyBindCredentials(bindInvocation.getInvokerCredentials());
        logger.debug("ignoring bind invocation {}", bindInvocation);

    }

    protected void processUnbindInvocation(SleUnbindInvocation unbindInvocation) {
        verifyNonBindCredentials(unbindInvocation.getInvokerCredentials());
        logger.debug("ignoring unbind invocation {}", unbindInvocation);
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

    private void sendUnbind(UnbindReason reason, CompletableFuture<Void> cf) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call unbind while in state " + state));
            return;
        }
        changeState(State.UNBINDING);
        this.unbindingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        SleUnbindInvocation sui = new SleUnbindInvocation();
        sui.setInvokerCredentials(getNonBindCredentials());
        sui.setUnbindReason(new ccsds.sle.transfer.service.bind.types.UnbindReason(reason.id()));
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
        if (authLevel == AuthLevel.BIND || authLevel == AuthLevel.ALL) {
            auth.verifyCredentials(credentials);
        }
    }

    protected InvokeId getInvokeId() {
        int n = invokeId++ & Short.MAX_VALUE;
        return new InvokeId(n);
    }

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
        channelHandlerContext.executor().schedule(() -> {
            boolean completed = cf.completeExceptionally(new SleException("The return from the "
                    + "provider has not been received in the timeout period (" + returnTimeoutSec + " seconds)"));
            if (completed) {
                logger.debug("return timeout reached");
                pendingInvocations.remove(invokeId.intValue());
            }
        }, returnTimeoutSec, TimeUnit.SECONDS);
        return invokeId;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Caught exception {}", cause.getMessage());
        monitors.forEach(m -> m.exceptionCaught(cause));
    }

    protected void notifyDisconnected() {
        monitors.forEach(m -> m.disconnected());
    }

    public State getState() {
        return state;
    }

    protected abstract ApplicationIdentifier getApplicationIdentifier();

    protected abstract void processData(BerTag berTag, InputStream is) throws IOException;

}
