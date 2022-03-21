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
import com.beanit.jasn1.ber.types.BerType;

import org.yamcs.sle.AuthLevel;
import org.yamcs.sle.Constants.ApplicationIdentifier;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.SleMonitor;
import org.yamcs.sle.State;
import org.yamcs.sle.StringConverter;
import org.yamcs.sle.provider.ServiceInitializer.ServiceInitResult;
import org.yamcs.sle.udpslebridge.SleUdpBridge;

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
 * This class provides the common SLE functionality across all services.
 * <p>
 * It implements SLE BIND and UNBIND operations and also takes over the schedule reports activation/deactivation.
 * <p>
 * When BIND is called, it relies first on the {@link AuthProvider} to obtain user/name passwords information for the
 * new connection and then on the {@link ServiceInitializer} for obtaining a {@link SleService}.
 * It then it forwards all the subsequent operations to the newly obtained service.
 * <p>
 * 
 * In order to use it
 * <ol>
 * <li>create a Netty pipeline with this class at the end of the pipeline</li>
 * <li>make an implementation for {@link AuthProvider} that provides authentication data</li>
 * <li>make an implementation for {@link ServiceInitializer} which based on some configuration creates one of the
 * {@link CltuServiceProvider} or {@link RafServiceProvider} (or maybe others in the future).
 * </li>
 * <li>make an implementation of {@link FrameSink} which uplinks the CLTUs</li>
 * <li>make an implementation of {@link FrameSource} which provides telemetry frames</li>
 * </ol>
 * 
 * <p>
 * See the {@link SleUdpBridge} for an example on how this class is used to create a SLE to UDP bridge.
 *
 * 
 * @author nm
 *
 */
public class SleProvider extends ChannelInboundHandlerAdapter {

    Isp1Authentication auth;

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(SleProvider.class);
    private int invokeId = 1;
    protected CompletableFuture<Void> startingCf;

    protected ChannelHandlerContext channelHandlerContext;

    protected AuthLevel authLevel = AuthLevel.ALL;

    final String responderId;

    // sleService !=null -> bound
    SleService sleService = null;

    int sleVersion = 2;
    protected List<SleMonitor> monitors = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> statusReportFuture;
    final ServiceInitializer serviceInitializer;

    private AuthProvider authProvider;

    public int getVersionNumber() {
        return sleVersion;
    }

    public void setVersionNumber(int versionNumber) {
        this.sleVersion = versionNumber;
    }

    public SleProvider(AuthProvider authProvider, String responderId, ServiceInitializer serviceInitializer) {
        this.authProvider = authProvider;
        this.responderId = responderId;
        this.serviceInitializer = serviceInitializer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.trace("received message: {}", msg);
        try {
            InputStream is = new ByteBufInputStream((ByteBuf) msg);
            BerTag berTag = new BerTag();
            berTag.decode(is);

            logger.trace("berTag: {}", berTag);

            if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 100)) {
                SleBindInvocation bindInvocation = new SleBindInvocation();
                bindInvocation.decode(is, false);
                processBindInvocation(bindInvocation);
            } else if (sleService == null) {
                logger.warn("Unexpected (bertag: {}) message received while not bound, aborting", berTag);
                peerAbort();
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 102)) {
                SleUnbindInvocation unbindInvocation = new SleUnbindInvocation();
                unbindInvocation.decode(is, false);
                processUnbindInvocation(unbindInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 4)) {
                SleScheduleStatusReportInvocation sleScheduleStatusReportInvocation = new SleScheduleStatusReportInvocation();
                sleScheduleStatusReportInvocation.decode(is, false);
                processSleScheduleStatusReportInvocation(sleScheduleStatusReportInvocation);
            } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 104)) {
                SlePeerAbort peerAbortInvocation = new SlePeerAbort();
                peerAbortInvocation.decode(is, false);
                processPeerAbortInvocation(peerAbortInvocation);
            } else {
                sleService.processData(berTag, is);
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

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.channelHandlerContext = ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        monitors.forEach(m -> m.connected());
    }

    public boolean isConnected() {
        return channelHandlerContext != null && channelHandlerContext.channel().isActive();
    }

    public void shutdown() {
        channelHandlerContext.close();
    }

    private void processPeerAbortInvocation(SlePeerAbort peerAbortInvocation) {
        logger.warn("received PEER-ABORT {}", peerAbortInvocation);
        channelHandlerContext.close();
    }

    protected void processBindInvocation(SleBindInvocation bindInvocation) {
        logger.debug("processing bindInvocation {}", bindInvocation);

        if (sleService != null) {
            logger.warn("recieved bind while already bound");
            sendNegativeBindResult(4);// already bound
            return;
        }

        String initiatorId = bindInvocation.getInitiatorIdentifier().toString();
        auth = authProvider.getAuth(initiatorId);
        if (auth == null) {
            logger.warn("Cannot obtain authentication information for initiator-identifier='{}'", initiatorId);
            sendNegativeBindResult(0);// access denied
            return;
        }

        verifyBindCredentials(bindInvocation.getInvokerCredentials());

        int version = bindInvocation.getVersionNumber().intValue();
        if (version < 2 || version > 4) {
            logger.warn("SLE version {}, not supported", version);
            sendNegativeBindResult(2); // version not supported
            return;
        }

        String sii = StringConverter.toString(bindInvocation.getServiceInstanceIdentifier());
        ApplicationIdentifier appId = ApplicationIdentifier.byId(bindInvocation.getServiceType().intValue());
        String responderPortId = bindInvocation.getResponderPortIdentifier().toString();
        ServiceInitResult sir = serviceInitializer.getServiceInstance(initiatorId, responderPortId, appId, sii);
        if (!sir.success) {
            logger.warn("Cannot get a service instance for initiatorId={}, responderPortId={}, appId={}, sii={}",
                    initiatorId, responderPortId, appId, sii);
            sendNegativeBindResult(sir.diagnostic);
            return;
        }

        this.sleVersion = version;
        sleService = sir.sleService;
        sleService.init(this);

        changeState(State.READY);

        SleBindReturn.Result r = new Result();
        r.setPositive(new VersionNumber(sleVersion));

        SleBindReturn sbr = new SleBindReturn();
        sbr.setResult(r);
        sbr.setResponderIdentifier(new AuthorityIdentifier(responderId.getBytes(StandardCharsets.US_ASCII)));
        sbr.setPerformerCredentials(getBindCredentials());

        logger.debug("sending bind return {}", sbr);
        RafProviderToUserPdu ptu = new RafProviderToUserPdu(); // we use RAF but it's the same message for all services
        ptu.setRafBindReturn(sbr);
        channelHandlerContext.writeAndFlush(ptu);
    }

    private void sendNegativeBindResult(int diagnostic) {
        SleBindReturn sbr = new SleBindReturn();
        SleBindReturn.Result r = new Result();
        r.setNegative(new BindDiagnostic(diagnostic));
        sbr.setResult(r);
        sbr.setResponderIdentifier(new AuthorityIdentifier(responderId.getBytes(StandardCharsets.US_ASCII)));
        sbr.setPerformerCredentials(getBindCredentials());
        logger.debug("sending bind return {}", sbr);

        RafProviderToUserPdu ptu = new RafProviderToUserPdu(); // we use RAF but it's the same message for all services
        ptu.setRafBindReturn(sbr);
        channelHandlerContext.writeAndFlush(ptu);
    }

    protected void processUnbindInvocation(SleUnbindInvocation unbindInvocation) {
        verifyNonBindCredentials(unbindInvocation.getInvokerCredentials());
        if (sleService == null || sleService.getState() != State.READY) {
            logger.warn("recieved unbind while not in READY state");
            peerAbort();
            return;
        }

        changeState(State.UNBOUND);

        SleUnbindReturn.Result r = new SleUnbindReturn.Result();
        r.setPositive(BER_NULL);

        SleUnbindReturn usbr = new SleUnbindReturn();
        usbr.setResponderCredentials(getNonBindCredentials());
        usbr.setResult(r);

        logger.debug("sending unbind return {}", usbr);
        RafProviderToUserPdu ptu = new RafProviderToUserPdu(); // we use RAF but it's the same message for all services
        ptu.setRafUnbindReturn(usbr);
        channelHandlerContext.writeAndFlush(ptu);

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
            cancelStatusReport();
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

    private void sendStatusReport() {
        if (sleService != null) {
            sleService.sendStatusReport();
        } else {
            cancelStatusReport();
        }
    }

    private void cancelStatusReport() {
        if (statusReportFuture != null) {
            statusReportFuture.cancel(true);
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

    protected Credentials getBindCredentials() {
        if ((auth != null) && (authLevel == AuthLevel.ALL || authLevel == AuthLevel.BIND)) {
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
        monitors.forEach(m -> m.stateChanged(newState));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.warn("Caught exception {}", cause.getMessage());
        monitors.forEach(m -> m.exceptionCaught(cause));
    }

    protected void notifyDisconnected() {
        monitors.forEach(m -> m.disconnected());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Connection {} closed", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
        cancelStatusReport();
        if (sleService != null) {
            sleService.abort();
        }
        notifyDisconnected();
    }

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

    public void sendMessage(BerType msg) {
        channelHandlerContext.writeAndFlush(msg);
    }

    /**
     * Checks if the netty outgoing buffer is full.
     * <p>
     * The onlineTimely RAF service should drop messages if the buffer is full.
     * <p>
     * The offline RAF service should back pressure the producer.
     * <p>
     * Note that even if the buffer is full, Netty will accept messages sent with {@link #sendMessage(BerType)}.
     * <p>
     * If the connection is completely stuck, the ISP1 heartbeat should detect the condition and close it.
     * 
     * @return
     */
    public boolean isWritable() {
        return channelHandlerContext.channel().isWritable();
    }
}
