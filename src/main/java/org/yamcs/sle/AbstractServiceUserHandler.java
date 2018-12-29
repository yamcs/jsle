package org.yamcs.sle;

import static org.yamcs.sle.Constants.CREDENTIALS_UNUSED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.openmuc.jasn1.ber.BerTag;
import org.openmuc.jasn1.ber.types.BerObjectIdentifier;
import org.openmuc.jasn1.ber.types.string.BerVisibleString;

import ccsds.sle.transfer.service.bind.types.ApplicationIdentifier;
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
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.service.instance.id.OidValues;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceAttribute;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public abstract class AbstractServiceUserHandler extends ChannelInboundHandlerAdapter {
    static public enum State {
        UNBOUND, BINDING, READY, STARTING, ACTIVE, STOPPING, UNBINDING
    };

    static public enum AuthLevel {
        NONE, BIND, ALL;
    }

    final Isp1Authentication auth;
    final String initiatorId;
    final String responderPortId;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractServiceUserHandler.class);
    private int invokeId = 1;
    private CompletableFuture<Void> bindingCf;
    protected CompletableFuture<Void> startingCf;
    private CompletableFuture<Void> stoppingCf;
    private CompletableFuture<Void> unbindingCf;

    protected State state = State.UNBOUND;

    protected ChannelHandlerContext channelHandlerContext;

    Map<Integer, CompletableFuture<?>> pendingInvocations = new HashMap<>();

    protected AuthLevel authLevel = AuthLevel.ALL;

    public AbstractServiceUserHandler(Isp1Authentication auth, String responderPortId, String initiatorId) {
        this.initiatorId = initiatorId;
        this.responderPortId = responderPortId;
        this.auth = auth;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("received message " + ByteBufUtil.hexDump(buf));
        if (logger.isTraceEnabled()) {
            logger.trace("received message: {}", msg);
        }
        try {
            InputStream is = new ByteBufInputStream((ByteBuf) msg);
            BerTag berTag = new BerTag();
            berTag.decode(is);
            if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 101)) {
                SleBindReturn bindReturn = new SleBindReturn();
                bindReturn.decode(is, false);
                processBindReturn(bindReturn);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 103)) {
                SleUnbindReturn unbindReturn = new SleUnbindReturn();
                unbindReturn.decode(is, false);
                processUnbindReturn(unbindReturn);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 104)) {
                SlePeerAbort peerAbortInvocation = new SlePeerAbort();
                peerAbortInvocation.decode(is, false);
                processPeerAbortInvocation(peerAbortInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 5)) {
                SleScheduleStatusReportReturn cltuScheduleStatusReportReturn = new SleScheduleStatusReportReturn();
                cltuScheduleStatusReportReturn.decode(is, false);
                processCltuScheduleStatusReportReturn(cltuScheduleStatusReportReturn);
            } else {
                processData(berTag, is);
            }
        } catch (IOException e) {
            logger.warn("Error decoding data", e);
            peerAbort();
        }
    }

    protected abstract void processData(BerTag berTag, InputStream is) throws IOException;

    private void processPeerAbortInvocation(SlePeerAbort peerAbortInvocation) {
        logger.warn("received PEER-ABORT {}", peerAbortInvocation);
        System.out.println("------------ received PEER-ABORT");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.channelHandlerContext = ctx;
    }

    public CompletableFuture<Void> bind() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendBind(cf));
        return cf;
    }

    public CompletableFuture<Void> start() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStart(cf));
        return cf;
    }

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

    abstract void sendStart(CompletableFuture<Void> cf);

    private void sendBind(CompletableFuture<Void> cf) {
        if (state != State.UNBOUND) {
            cf.completeExceptionally(new SleException("Cannot call bind while in state " + state));
            return;
        }
        state = State.BINDING;
        this.bindingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        SleBindInvocation sbi = new SleBindInvocation();
        sbi.setInitiatorIdentifier(new AuthorityIdentifier(initiatorId.getBytes(StandardCharsets.US_ASCII)));
        if (authLevel == AuthLevel.NONE) {
            sbi.setInvokerCredentials(CREDENTIALS_UNUSED);
        } else {
            sbi.setInvokerCredentials(auth.generateCredentials());
        }
        sbi.setServiceInstanceIdentifier(getServiceInstanceIdentifier());
        sbi.setResponderPortIdentifier(new PortId(responderPortId.getBytes(StandardCharsets.US_ASCII)));
        sbi.setVersionNumber(new VersionNumber(2));
        sbi.setServiceType(new ApplicationIdentifier(Constants.APP_ID_FWD_CLTU));
        cutp.setCltuBindInvocation(sbi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processBindReturn(SleBindReturn bindReturn) {
        if (state != State.BINDING) {
            peerAbort();
            return;
        }
        Result r = bindReturn.getResult();
        if (r.getNegative() != null) {
            state = State.UNBOUND;
            bindingCf.completeExceptionally(
                    new SleException("bind failed: " + Constants.BIND_DIAGNOSTIC.get(r.getNegative().intValue())));
        } else {
            state = State.READY;
            bindingCf.complete(null);
        }
    }

    private void sendStop(CompletableFuture<Void> cf) {
        if (state != State.ACTIVE) {
            cf.completeExceptionally(new SleException("Cannot call stop while in state " + state));
            return;
        }
        state = State.STOPPING;
        this.stoppingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        SleStopInvocation ssi = new SleStopInvocation();
        ssi.setInvokerCredentials(getNonBindCredentials());
        ssi.setInvokeId(getInvokeId());

        cutp.setCltuStopInvocation(ssi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    protected void processStopReturn(SleAcknowledgement stopReturn) {
        if (state != State.STOPPING) {
            peerAbort();
            return;
        }
        ccsds.sle.transfer.service.common.pdus.SleAcknowledgement.Result r = stopReturn.getResult();
        if (r.getNegativeResult() != null) {
            state = State.ACTIVE;
            stoppingCf.completeExceptionally(new SleException("stop failed", r));
        } else {
            state = State.READY;
            stoppingCf.complete(null);
        }
    }

    private void sendUnbind(CompletableFuture<Void> cf) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call unbind while in state " + state));
            return;
        }
        state = State.UNBINDING;
        this.unbindingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        SleUnbindInvocation sui = new SleUnbindInvocation();
        sui.setInvokerCredentials(getNonBindCredentials());
        sui.setUnbindReason(new UnbindReason(127));
        cutp.setCltuUnbindInvocation(sui);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processUnbindReturn(SleUnbindReturn unbindReturn) {
        if (state != State.UNBINDING) {
            peerAbort();
            return;
        }
        // ccsds.sle.transfer.service.bind.types.SleUnbindReturn.Result r = cltuUnbindReturn.getResult();

        state = State.UNBOUND;
        unbindingCf.complete(null);
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

    private void sendScheduleStatusReport(ReportRequestType reportRequestType, CompletableFuture<Void> cf) {
        SleScheduleStatusReportInvocation sssri = new SleScheduleStatusReportInvocation();
        sssri.setInvokerCredentials(getNonBindCredentials());
        sssri.setInvokeId(getInvokeId(cf));
        sssri.setReportRequestType(reportRequestType);
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();
        cutp.setCltuScheduleStatusReportInvocation(sssri);
        channelHandlerContext.writeAndFlush(cutp);

    }

    private void processCltuScheduleStatusReportReturn(SleScheduleStatusReportReturn cltuScheduleStatusReportReturn) {
        CompletableFuture<Void> cf = getFuture(cltuScheduleStatusReportReturn.getInvokeId());
        SleScheduleStatusReportReturn.Result r = cltuScheduleStatusReportReturn.getResult();
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

    protected InvokeId getInvokeId() {
        int n = invokeId++ & Short.MAX_VALUE;
        return new InvokeId(n);
    }

    private static ServiceInstanceIdentifier getServiceInstanceIdentifier() {
        ServiceInstanceIdentifier sii = new ServiceInstanceIdentifier();
        List<ServiceInstanceAttribute> l = sii.getServiceInstanceAttribute();
        l.add(getServiceInstanceAttribute(OidValues.sagr, "SAGR"));
        l.add(getServiceInstanceAttribute(OidValues.spack, "SPACK"));
        l.add(getServiceInstanceAttribute(OidValues.fslFg, "FSL-FG"));
        l.add(getServiceInstanceAttribute(OidValues.cltu, "cltu1"));
        return sii;
    }

    private static ServiceInstanceAttribute getServiceInstanceAttribute(BerObjectIdentifier id, String value) {
        ServiceInstanceAttribute sia = new ServiceInstanceAttribute();
        ServiceInstanceAttribute.SEQUENCE sias = new ServiceInstanceAttribute.SEQUENCE();
        sias.setIdentifier(id);
        sias.setSiAttributeValue(new BerVisibleString(value));
        sia.getSEQUENCE().add(sias);
        return sia;
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

    protected InvokeId getInvokeId(CompletableFuture<?> cf) {
        InvokeId invokeId = getInvokeId();
        pendingInvocations.put(invokeId.intValue(), cf);
        return invokeId;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        System.out.println("in exception caughtt " + cause);
        cause.printStackTrace();

        ctx.fireExceptionCaught(cause);
    }
}
