package org.yamcs.sle.provider;

import static org.yamcs.sle.Constants.BER_NULL;
import static org.yamcs.sle.Constants.CREDENTIALS_UNUSED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.beanit.jasn1.ber.BerTag;
import com.beanit.jasn1.ber.types.BerInteger;

import org.yamcs.sle.AuthLevel;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.SleMonitor;
import org.yamcs.sle.State;

import ccsds.sle.transfer.service.bind.types.AuthorityIdentifier;
import ccsds.sle.transfer.service.bind.types.BindDiagnostic;
import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.SleBindReturn;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindInvocation;
import ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import ccsds.sle.transfer.service.bind.types.VersionNumber;
import ccsds.sle.transfer.service.bind.types.SleBindReturn.Result;
import ccsds.sle.transfer.service.common.pdus.DiagnosticScheduleStatusReport;
import ccsds.sle.transfer.service.common.pdus.ReportRequestType;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportInvocation;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafProviderToUserPdu;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Common class for all SLE provider services
 * 
 * @author nm
 *
 */
public abstract class AbstractServiceProviderHandler extends ChannelInboundHandlerAdapter {

    final Isp1Authentication auth;

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(AbstractServiceProviderHandler.class);
    private int invokeId = 1;
    protected CompletableFuture<Void> startingCf;

    protected volatile State state = State.UNBOUND;

    protected ChannelHandlerContext channelHandlerContext;

    protected AuthLevel authLevel = AuthLevel.ALL;

    final protected SleAttributes attr;

    int sleVersion = 2;
    protected List<SleMonitor> monitors = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> statusReportFuture;

    public int getVersionNumber() {
        return sleVersion;
    }

    public void setVersionNumber(int versionNumber) {
        this.sleVersion = versionNumber;
    }

    public AbstractServiceProviderHandler(Isp1Authentication auth, SleAttributes attr) {
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
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 4)) {
                SleScheduleStatusReportInvocation sleScheduleStatusReportInvocation = new SleScheduleStatusReportInvocation();
                sleScheduleStatusReportInvocation.decode(is, false);
                processSleScheduleStatusReportInvocation(sleScheduleStatusReportInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 2)) {
                SleStopInvocation sleStopInvocation = new SleStopInvocation();
                sleStopInvocation.decode(is, false);
                processSleStopInvocation(sleStopInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 104)) {
                SlePeerAbort peerAbortInvocation = new SlePeerAbort();
                peerAbortInvocation.decode(is, false);
                processPeerAbortInvocation(peerAbortInvocation);
            } else {
                processData(berTag, is);
            }
        } catch (IOException e) {
            logger.warn("Error decoding data", e);
            peerAbort();
        }
    }

    protected abstract void processSleStopInvocation(SleStopInvocation sleStopInvocation);

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
        ctx.channel().closeFuture().addListener(cf -> {
            logger.debug("Connection {} closed", ctx.channel().remoteAddress());
            notifyDisconnected();
        });
    }

    public boolean isConnected() {
        return channelHandlerContext.channel().isActive();
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

    private void processPeerAbortInvocation(SlePeerAbort peerAbortInvocation) {
        logger.warn("received PEER-ABORT {}", peerAbortInvocation);
        channelHandlerContext.close();
    }

    private void processBindReturn(SleBindReturn bindReturn) {
        verifyBindCredentials(bindReturn.getPerformerCredentials());
        logger.warn("ignoring bind return");
    }

    protected void processBindInvocation(SleBindInvocation bindInvocation) {
        logger.debug("processing bindInvocation {}", bindInvocation);

        verifyBindCredentials(bindInvocation.getInvokerCredentials());
        SleBindReturn.Result r = new Result();
        int version = bindInvocation.getVersionNumber().intValue();

        if (state != State.UNBOUND) {
            logger.warn("recieved bind while in state {}", state);
            r.setNegative(new BindDiagnostic(4));// already bound
        } else if (version < 2 || version > 4) {
            logger.warn("SLE version {}, not supported", version);
            r.setNegative(new BindDiagnostic(2));// version not supported
        } else {
            this.sleVersion = version;
            r.setPositive(new VersionNumber(sleVersion));
            changeState(State.READY);
        }
        SleBindReturn sbr = new SleBindReturn();
        sbr.setResult(r);
        sbr.setResponderIdentifier(new AuthorityIdentifier(attr.responderId.getBytes(StandardCharsets.US_ASCII)));
        sbr.setPerformerCredentials(getBindCredentials());
        logger.debug("sending bind return {}", sbr);

        RafProviderToUserPdu ptu = new RafProviderToUserPdu(); // we use RAF but it's the same message for all services
        ptu.setRafBindReturn(sbr);
        channelHandlerContext.writeAndFlush(ptu);
    }

    protected void processUnbindInvocation(SleUnbindInvocation unbindInvocation) {
        verifyNonBindCredentials(unbindInvocation.getInvokerCredentials());
        if (state != State.READY) {
            logger.warn("recieved bind while in state {}", state);
            peerAbort();
            return;
        }
        changeState(State.UNBOUND);
    }

    protected void processSleScheduleStatusReportInvocation(
            SleScheduleStatusReportInvocation sleScheduleStatusReportInvocation) {
        logger.debug("Received SleScheduleStatusReportInvocation {}", sleScheduleStatusReportInvocation);
        SleScheduleStatusReportReturn ssr = new SleScheduleStatusReportReturn();
        ssr.setPerformerCredentials(getNonBindCredentials());
        ssr.setInvokeId(sleScheduleStatusReportInvocation.getInvokeId());
        SleScheduleStatusReportReturn.Result result = new SleScheduleStatusReportReturn.Result();
        ReportRequestType rrt = sleScheduleStatusReportInvocation.getReportRequestType();
        if (rrt.getPeriodically() != null) {
            if (statusReportFuture != null) {
                statusReportFuture.cancel(true);
            }
            int nsec = rrt.getPeriodically().intValue();
            if (nsec > 0) {
                statusReportFuture = channelHandlerContext.executor().scheduleAtFixedRate(() -> {
                    sendStatusReport();
                }, 0, nsec, TimeUnit.SECONDS);
                result.setPositiveResult(BER_NULL);
            } else {
                DiagnosticScheduleStatusReport dssr = new DiagnosticScheduleStatusReport();
                dssr.setSpecific(new BerInteger(2));// invalidReportingCycle
                result.setNegativeResult(dssr);
            }
        } else if (rrt.getImmediately() != null) {
            channelHandlerContext.executor().schedule(() -> {
                sendStatusReport();
            }, 0, TimeUnit.SECONDS);
        } else if (rrt.getStop() != null) {
            cancelStatusReport();
            result.setPositiveResult(BER_NULL);
        }

        ssr.setResult(result);
        RafProviderToUserPdu cptu = new RafProviderToUserPdu();
        cptu.setRafScheduleStatusReportReturn(ssr);
        channelHandlerContext.writeAndFlush(cptu);
    }

    protected abstract void sendStatusReport();

    private void cancelStatusReport() {
        if (statusReportFuture != null) {
            statusReportFuture.cancel(true);
        }
    }

    private void processUnbindReturn(SleUnbindReturn unbindReturn) {
        verifyNonBindCredentials(unbindReturn.getResponderCredentials());
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

    protected Credentials getBindCredentials() {
        if (authLevel == AuthLevel.ALL || authLevel == AuthLevel.BIND) {
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

    protected void changeState(State newState) {
        this.state = newState;
        monitors.forEach(m -> m.stateChanged(newState));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Caught exception {}", cause.getMessage());
        monitors.forEach(m -> m.exceptionCaught(cause));
    }

    protected void notifyDisconnected() {
        monitors.forEach(m -> m.disconnected());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        cancelStatusReport();
    }

    protected abstract void processData(BerTag berTag, InputStream is) throws IOException;

    /**
     * Add a monitor to be notified when events happen.
     * 
     * @param monitor
     */
    public void addMonitor(SleMonitor monitor) {
        monitors.add(monitor);
    }

    public void removeMonitor(SleMonitor monitor) {
        monitors.remove(monitor);
    }

}
