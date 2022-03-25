package org.yamcs.sle.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import com.beanit.jasn1.ber.BerTag;

import org.yamcs.sle.AntennaId;
import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.ParameterName;
import org.yamcs.sle.SleParameter;
import org.yamcs.sle.RacfSleMonitor;
import org.yamcs.sle.SleException;
import org.yamcs.sle.State;
import org.yamcs.sle.StringConverter;
import org.yamcs.sle.Constants.ApplicationIdentifier;
import org.yamcs.sle.Constants.DeliveryMode;
import org.yamcs.sle.Constants.FrameQuality;
import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.ProductionStatus;
import org.yamcs.sle.Constants.RequestedFrameQuality;

import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafGetParameterInvocation;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafStartInvocation;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafUsertoProviderPdu;
import ccsds.sle.transfer.service.raf.outgoing.pdus.FrameOrNotification;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafGetParameterReturn;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStartReturn;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStatusReportInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafSyncNotifyInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferBuffer;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;
import ccsds.sle.transfer.service.raf.structures.LockStatusReport;
import ccsds.sle.transfer.service.raf.structures.Notification;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter;
import ccsds.sle.transfer.service.raf.structures.RafParameterName;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation.PrivateAnnotation;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Implementation for the CCSDS RECOMMENDED STANDARD FOR SLE RCF SERVICE
 * CCSDS 911.2-B-3 August 2016
 * https://public.ccsds.org/Pubs/911x1b4.pdf
 * <p>
 * This class contains RAF specific implementation
 * @author nm
 *
 */
public class RafServiceUserHandler extends RacfServiceUserHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RafServiceUserHandler.class);
    int cltuId = 1;


    private RequestedFrameQuality requestedFrameQuality = RequestedFrameQuality.goodFramesOnly;

    public RafServiceUserHandler(Isp1Authentication auth, SleAttributes attr, DeliveryMode deliveryMode,
            FrameConsumer consumer) {
        super(auth, attr, deliveryMode, consumer);
        setDeliveryMode(deliveryMode);
    }

    /**
     * Get the value of an RAF parameter from the provider
     * 
     * @param parameterId
     *            one of the parameters defined in {@link ParameterName}. Note that not all of them make sense for the
     *            RAF, see the table 3-11 in the standard to see which ones make sense.
     * @return
     */
    public CompletableFuture<SleParameter> getParameter(int parameterId) {
        CompletableFuture<SleParameter> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendGetParameter(parameterId, cf));
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
     * Request that the SLE service provider starts sending
     * 
     * @return
     */
    public CompletableFuture<Void> start(CcsdsTime startTime, CcsdsTime stopTime) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStart(cf, startTime, stopTime));
        return cf;
    }

    public RequestedFrameQuality getRequestedFrameQuality() {
        return requestedFrameQuality;
    }

    /**
     * Set the requested frame quality. This has to be done before the service start.
     * 
     * @param requestedFrameQuality
     */
    public void setRequestedFrameQuality(RequestedFrameQuality requestedFrameQuality) {
        this.requestedFrameQuality = requestedFrameQuality;
    }

    /**
     * Add a monitor to be notified when events happen.
     * 
     * @param monitor
     */
    public void addMonitor(RacfSleMonitor monitor) {
        monitors.add(monitor);
    }

    public void removeMonitor(RacfSleMonitor monitor) {
        monitors.remove(monitor);
    }

    void sendStart(CompletableFuture<Void> cf) {
        sendStart(cf, null, null);
    }

    protected void processData(BerTag berTag, InputStream is) throws IOException {
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 8)) {
            RafTransferBuffer rafTransferBuffer = new RafTransferBuffer();
            rafTransferBuffer.decode(is, false);
            processTransferBuffer(rafTransferBuffer);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
            RafStartReturn rafStartReturn = new RafStartReturn();
            rafStartReturn.decode(is, false);
            processStartReturn(rafStartReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 9)) {
            RafStatusReportInvocation rafStatusReportInvocation = new RafStatusReportInvocation();
            rafStatusReportInvocation.decode(is, false);
            processStatusReportInvocation(rafStatusReportInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
            RafGetParameterReturn rafGetParameterReturn = new RafGetParameterReturn();
            rafGetParameterReturn.decode(is, false);
            processGetParameterReturn(rafGetParameterReturn);
        } else {
            logger.warn("Unexpected state berTag: {} ", berTag);
            throw new IllegalStateException();
        }
    }

    private void sendGetParameter(int parameterId, CompletableFuture<SleParameter> cf) {
        RafUsertoProviderPdu rutp = new RafUsertoProviderPdu();

        RafGetParameterInvocation cgpi = new RafGetParameterInvocation();
        cgpi.setInvokeId(getInvokeId(cf));
        cgpi.setInvokerCredentials(getNonBindCredentials());
        cgpi.setRafParameter(new RafParameterName(parameterId));
        rutp.setRafGetParameterInvocation(cgpi);
        channelHandlerContext.writeAndFlush(rutp);
    }

    protected void sendStart(CompletableFuture<Void> cf, CcsdsTime start, CcsdsTime stop) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call start while in state " + state));
            return;
        }
        changeState(State.STARTING);
        this.startingCf = cf;

        RafUsertoProviderPdu rutp = new RafUsertoProviderPdu();

        RafStartInvocation rsi = new RafStartInvocation();
        rsi.setRequestedFrameQuality(
                new ccsds.sle.transfer.service.raf.structures.RequestedFrameQuality(requestedFrameQuality.getId()));
        rsi.setInvokeId(new InvokeId(1));
        rsi.setStartTime(CcsdsTime.toSleConditional(start, sleVersion));
        rsi.setStopTime(CcsdsTime.toSleConditional(stop, sleVersion));

        rsi.setInvokerCredentials(getNonBindCredentials());

        rutp.setRafStartInvocation(rsi);
        channelHandlerContext.writeAndFlush(rutp);
    }

    private void processStartReturn(RafStartReturn rafStartReturn) {
        verifyNonBindCredentials(rafStartReturn.getPerformerCredentials());
        if (state != State.STARTING) {
            peerAbort();
            return;
        }
        ccsds.sle.transfer.service.raf.outgoing.pdus.RafStartReturn.Result r = rafStartReturn.getResult();
        if (r.getNegativeResult() != null) {
            changeState(State.READY);
            startingCf.completeExceptionally(new SleException(
                    "received negative result to start request: " + StringConverter.toString(r.getNegativeResult())));
        } else {
            changeState(State.ACTIVE);
            startingCf.complete(null);
        }
    }

    private void processGetParameterReturn(RafGetParameterReturn rafGetParameterReturn) {
        verifyNonBindCredentials(rafGetParameterReturn.getPerformerCredentials());

        CompletableFuture<RafGetParameter> cf = getFuture(rafGetParameterReturn.getInvokeId());
        RafGetParameterReturn.Result r = rafGetParameterReturn.getResult();
        if (r.getNegativeResult() != null) {
            cf.completeExceptionally(new SleException("Error getting parameter value", r.getNegativeResult()));
        } else {
            cf.complete(r.getPositiveResult());
        }
    }

    private void processStatusReportInvocation(RafStatusReportInvocation rafStatusReportInvocation) {
        verifyNonBindCredentials(rafStatusReportInvocation.getInvokerCredentials());
        if (logger.isTraceEnabled()) {
            logger.trace("Received statusReport {}", rafStatusReportInvocation);
        }
        monitors.forEach(m -> ((RacfSleMonitor) m).onStatusReport(new RacfStatusReport(rafStatusReportInvocation)));
    }

    private void processTransferBuffer(RafTransferBuffer rafTransferBuffer) {
        for (FrameOrNotification fon : rafTransferBuffer.getFrameOrNotification()) {
            RafTransferDataInvocation rtdi = fon.getAnnotatedFrame();
            if (rtdi != null) {
                verifyNonBindCredentials(rtdi.getInvokerCredentials());
                CcsdsTime ert = CcsdsTime.fromSle(rtdi.getEarthReceiveTime());
                int dlc = rtdi.getDataLinkContinuity().intValue();
                PrivateAnnotation pa =rtdi.getPrivateAnnotation();
                byte[] privAnn = pa.getNotNull()==null?null:pa.getNotNull().value;
                byte[] data = rtdi.getData().value;
                AntennaId antId = new AntennaId(rtdi.getAntennaId());
                FrameQuality fq = FrameQuality.byId(rtdi.getDeliveredFrameQuality().intValue());
                consumer.acceptFrame(ert, antId, dlc, fq, privAnn, data);
            }

            RafSyncNotifyInvocation rsi = fon.getSyncNotification();
            if (rsi != null) {
                try {
                    verifyNonBindCredentials(rsi.getInvokerCredentials());

                    Notification notif = rsi.getNotification();
                    if (notif.getLossFrameSync() != null) {
                        LockStatusReport lsr = notif.getLossFrameSync();

                        consumer.onLossFrameSync(CcsdsTime.fromSle(lsr.getTime()),
                                LockStatus.byId(lsr.getCarrierLockStatus().intValue()),
                                LockStatus.byId(lsr.getSubcarrierLockStatus().intValue()),
                                LockStatus.byId(lsr.getSymbolSyncLockStatus().intValue()));
                    } else if (notif.getProductionStatusChange() != null) {
                        consumer.onProductionStatusChange(
                                ProductionStatus.byId(notif.getProductionStatusChange().intValue()));
                    } else if (notif.getExcessiveDataBacklog() != null) {
                        consumer.onExcessiveDataBacklog();
                    } else if (notif.getEndOfData() != null) {
                        consumer.onEndOfData();
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid notification received ", e);
                    peerAbort();
                }
            }
        }

    }

    @Override
    protected ApplicationIdentifier getApplicationIdentifier() {
        return Constants.ApplicationIdentifier.rtnAllFrames;
    }
}
