package org.yamcs.sle.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import com.beanit.jasn1.ber.BerTag;
import com.beanit.jasn1.ber.types.BerType;

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
import org.yamcs.sle.GVCID;

import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfTransferDataInvocation.PrivateAnnotation;
import ccsds.sle.transfer.service.rcf.incoming.pdus.RcfGetParameterInvocation;
import ccsds.sle.transfer.service.rcf.incoming.pdus.RcfStartInvocation;
import ccsds.sle.transfer.service.rcf.incoming.pdus.RcfUserToProviderPdu;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.FrameOrNotification;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfGetParameterReturn;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfStartReturn;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfStatusReportInvocation;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfSyncNotifyInvocation;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfTransferBuffer;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfTransferDataInvocation;
import ccsds.sle.transfer.service.rcf.structures.RcfParameterName;
import ccsds.sle.transfer.service.rcf.structures.DiagnosticRcfGet;
import ccsds.sle.transfer.service.rcf.structures.LockStatusReport;
import ccsds.sle.transfer.service.rcf.structures.Notification;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Implementation for the CCSDS RECOMMENDED STANDARD FOR SLE RAF SERVICE
 * CCSDS 911.1-B-4 August 2016
 * https://public.ccsds.org/Pubs/911x1b4.pdf
 * <p>
 * This is copy and paste from {@link RafServiceUserHandler} because the RAF and RCF are almost the same but slightly so
 * different (seems like the CCSDS committee was missing from school when the lessons about code reused has been taught).
 * <p>
 * We do however provide one common RAF/RCF interface to our users highlighting the small differences in the API.
 * @author nm
 *
 */
public class RcfServiceUserHandler extends RacfServiceUserHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RcfServiceUserHandler.class);
   
    
    public RcfServiceUserHandler(Isp1Authentication auth, SleAttributes attr, DeliveryMode deliveryMode, FrameConsumer consumer) {
        super(auth, attr, deliveryMode, consumer);
        this.consumer = consumer;
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
     * Request that the SLE service provider starts sending
     * 
     * @return
     */
    public CompletableFuture<Void> start(CcsdsTime startTime, CcsdsTime stopTime, GVCID requestedGvcid) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStart(cf, startTime, stopTime, requestedGvcid));
        return cf;
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
    
    /**
     * Request that the SLE service provider starts sending data
     * 
     * @return
     */
    public CompletableFuture<Void> start(GVCID requestedGvcid) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendStart(cf, requestedGvcid));
        return cf;
    }
    
    
    private void sendStart(CompletableFuture<Void> cf, GVCID requestedGvcid) {
        sendStart(cf, null, null, requestedGvcid);
    }

    protected void processData(BerTag berTag, InputStream is) throws IOException {
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 8)) {
            RcfTransferBuffer rafTransferBuffer = new RcfTransferBuffer();
            rafTransferBuffer.decode(is, false);
            processTransferBuffer(rafTransferBuffer);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
            RcfStartReturn rafStartReturn = new RcfStartReturn();
            rafStartReturn.decode(is, false);
            processStartReturn(rafStartReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 9)) {
            RcfStatusReportInvocation rafStatusReportInvocation = new RcfStatusReportInvocation();
            rafStatusReportInvocation.decode(is, false);
            processStatusReportInvocation(rafStatusReportInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
            RcfGetParameterReturn rafGetParameterReturn = new RcfGetParameterReturn();
            rafGetParameterReturn.decode(is, false);
            processGetParameterReturn(rafGetParameterReturn);
        } else {
            logger.warn("Unexpected state berTag: {} ", berTag);
            throw new IllegalStateException();
        }
    }

    private void sendGetParameter(int parameterId, CompletableFuture<SleParameter> cf) {
        RcfUserToProviderPdu rutp = new RcfUserToProviderPdu();

        RcfGetParameterInvocation cgpi = new RcfGetParameterInvocation();
        cgpi.setInvokeId(getInvokeId(cf));
        cgpi.setInvokerCredentials(getNonBindCredentials());
        cgpi.setRcfParameter(new RcfParameterName(parameterId));
        rutp.setRcfGetParameterInvocation(cgpi);
        channelHandlerContext.writeAndFlush(rutp);
    }

    protected void sendStart(CompletableFuture<Void> cf, CcsdsTime start, CcsdsTime stop,  GVCID requestedGvcid) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call start while in state " + state));
            return;
        }
        changeState(State.STARTING);
        this.startingCf = cf;

        RcfUserToProviderPdu rutp = new RcfUserToProviderPdu();

        RcfStartInvocation rsi = new RcfStartInvocation();
        rsi.setInvokeId(new InvokeId(1));
        rsi.setStartTime(CcsdsTime.toSleConditional(start, sleVersion));
        rsi.setStopTime(CcsdsTime.toSleConditional(stop, sleVersion));
        rsi.setRequestedGvcId(requestedGvcid.toRcf());
        rsi.setInvokerCredentials(getNonBindCredentials());

        rutp.setRcfStartInvocation(rsi);
        channelHandlerContext.writeAndFlush(rutp);
    }

   

    private void processStartReturn(RcfStartReturn rcfStartReturn) {
        verifyNonBindCredentials(rcfStartReturn.getPerformerCredentials());
        if (state != State.STARTING) {
            peerAbort();
            return;
        }
        ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfStartReturn.Result r = rcfStartReturn.getResult();
        if (r.getNegativeResult() != null) {
            changeState(State.READY);
            startingCf.completeExceptionally(new SleException("received negative result to start request: "+StringConverter.toString(r.getNegativeResult())));
        } else {
            changeState(State.ACTIVE);
            startingCf.complete(null);
        }
    }

    private void processGetParameterReturn(RcfGetParameterReturn rcfGetParameterReturn) {
        verifyNonBindCredentials(rcfGetParameterReturn.getPerformerCredentials());

        CompletableFuture<SleParameter> cf = getFuture(rcfGetParameterReturn.getInvokeId());
        RcfGetParameterReturn.Result r = rcfGetParameterReturn.getResult();
        if (r.getNegativeResult() != null) {
            cf.completeExceptionally(
                    new SleException("Error getting parameter value: " + toString(r.getNegativeResult())));
        } else {
            cf.complete(new SleParameter(r.getPositiveResult()));
        }
    }

    private String toString(DiagnosticRcfGet diagnostic) {
        if (diagnostic.getCommon() != null) {
            return Constants.getCommonDiagnostic(diagnostic.getCommon().intValue());
        } else {
            int x = diagnostic.getSpecific().intValue();
            return (x == 0) ? "unknown parameter" : "unknown(" + x + ")";
        }
    }

    private void processStatusReportInvocation(RcfStatusReportInvocation rafStatusReportInvocation) {
        verifyNonBindCredentials(rafStatusReportInvocation.getInvokerCredentials());
        if (logger.isTraceEnabled()) {
            logger.trace("Received statusReport {}", rafStatusReportInvocation);
        }
        monitors.forEach(m -> ((RacfSleMonitor)m).onStatusReport(new RacfStatusReport(rafStatusReportInvocation)));
    }

    private void processTransferBuffer(RcfTransferBuffer rcfTransferBuffer) {
        for (FrameOrNotification fon : rcfTransferBuffer.getFrameOrNotification()) {
            RcfTransferDataInvocation rtdi = fon.getAnnotatedFrame();
            if (rtdi != null) {
                verifyNonBindCredentials(rtdi.getInvokerCredentials());
                CcsdsTime ert = CcsdsTime.fromSle(rtdi.getEarthReceiveTime());
                int dlc = rtdi.getDataLinkContinuity().intValue();
                PrivateAnnotation pa =rtdi.getPrivateAnnotation();
                byte[] privAnn = pa.getNotNull()==null?null:pa.getNotNull().value;
                byte[] data = rtdi.getData().value;
                AntennaId antId = new AntennaId(rtdi.getAntennaId());
                consumer.acceptFrame(ert, antId, dlc, FrameQuality.good, privAnn, data);
            }
            
            RcfSyncNotifyInvocation rsi = fon.getSyncNotification();
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
        return Constants.ApplicationIdentifier.rtnChFrames;
    }
}
