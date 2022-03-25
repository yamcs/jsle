package org.yamcs.sle.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants;
import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.ParameterName;
import org.yamcs.sle.SleException;
import org.yamcs.sle.SleParameter;
import org.yamcs.sle.State;
import org.yamcs.sle.StringConverter;
import org.yamcs.sle.Constants.ApplicationIdentifier;

import com.beanit.jasn1.ber.BerTag;
import com.beanit.jasn1.ber.types.BerOctetString;

import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuGetParameterInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuStartInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuThrowEventInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuTransferDataInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuUserToProviderPdu;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuGetParameterReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuThrowEventReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuTransferDataReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuTransferDataReturn.Result;
import ccsds.sle.transfer.service.cltu.structures.CltuData;
import ccsds.sle.transfer.service.cltu.structures.CltuIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuParameterName;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuGetParameter;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuThrowEvent;
import ccsds.sle.transfer.service.cltu.structures.EventInvocationId;
import ccsds.sle.transfer.service.common.types.Duration;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.InvokeId;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static org.yamcs.sle.Constants.*;

/**
 * Implementation for the
 * CCSDS RECOMMENDED STANDARD FOR SLE FCLTU SERVICE
 * CCSDS 912.1-B-4 August 2016
 * 
 * https://public.ccsds.org/Pubs/912x1b4.pdf
 * 
 * @author nm
 *
 */
public class CltuServiceUserHandler extends AbstractServiceUserHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CltuServiceUserHandler.class);
    int eventInvocationId = 1;

    private volatile long cltuBufferAvailable;

    private AtomicInteger nextCltuId = new AtomicInteger(1);

    public CltuServiceUserHandler(Isp1Authentication auth, SleAttributes attr) {
        super(auth, attr);
    }

    /**
     * Get the value of an CLTU parameter from the provider
     * 
     * @param parameterId
     *            one of the parameters defined in {@link ParameterName}. Note that not all of them make sense for the
     *            CLTU, see the table 3-11 in the standard to see which ones make sense.
     * @return
     */
    public CompletableFuture<SleParameter> getParameter(int parameterId) {
        CompletableFuture<SleParameter> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendGetParameter(parameterId, cf));
        return cf;
    }

    public CompletableFuture<SleParameter> getParameter(ParameterName parameterName) {
        return getParameter(parameterName.id());
    }
    /**
     * Transfer a CLTU to the provider requesting immediate transmission
     * 
     * @param cltu
     * @return return the cltu id which is the identification number that will be provided back in the
     *         {@link CltuSleMonitor#onAsyncNotify(CltuAsyncNotifyInvocation)}
     */
    public int transferCltu(byte[] cltu) {
        return transferCltu(cltu, null, null, 0, true);
    }

    /**
     * Transfer a CLTU to the provider
     * 
     * @param cltu
     *            the cltu to be transferred
     * @param earliestTransmissionTime
     *            earliest transmission time. null can be used to request immediate
     *            transmission.
     * @param latestTransmissionTime
     *            latest transmission time. null can be used to impose no limit.
     * 
     * @param delayMicrosec
     * @param produceReport
     *            if set to true, the CLTU provider will send a notification upon completion of the radidation. See
     *            {@link CltuSleMonitor#onAsyncNotify(CltuAsyncNotifyInvocation)}
     * 
     * @return return the cltu id which is the identification number that will be provided back in the
     *         {@link CltuSleMonitor#onAsyncNotify(CltuAsyncNotifyInvocation)}
     * 
     */
    public int transferCltu(byte[] cltu, CcsdsTime earliestTransmissionTime,
            CcsdsTime latestTransmissionTime, long delayMicrosec, boolean produceReport) {

        if (!channelHandlerContext.channel().isActive()) {
            throw new IllegalStateException("channel is not active");
        }

        int id = nextCltuId.getAndIncrement();
        channelHandlerContext.executor().execute(() -> sendTransferData(cltu, earliestTransmissionTime,
                latestTransmissionTime, delayMicrosec, produceReport, id));
        return id;
    }

    /**
     * 
     * @param eventIdentifier
     * @param eventQualifier
     * @return
     */
    public CompletableFuture<Void> throwEvent(int eventIdentifier, byte[] eventQualifier) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendThrowEvent(eventIdentifier, eventQualifier, cf));
        return cf;
    }

    /**
     * Add a monitor to be notified when events happen.
     * 
     * @param monitor
     */
    public void addMonitor(CltuSleMonitor monitor) {
        monitors.add(monitor);
    }

    public void removeMonitor(CltuSleMonitor monitor) {
        monitors.remove(monitor);
    }

    protected void processData(BerTag berTag, InputStream is) throws IOException {
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 11)) {
            CltuTransferDataReturn cltuTransferDataReturn = new CltuTransferDataReturn();
            cltuTransferDataReturn.decode(is, false);
            processTransferDataReturn(cltuTransferDataReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
            CltuStartReturn cltuStartReturn = new CltuStartReturn();
            cltuStartReturn.decode(is, false);
            processStartReturn(cltuStartReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
            CltuGetParameterReturn cltuGetParameterReturn = new CltuGetParameterReturn();
            cltuGetParameterReturn.decode(is, false);
            processGetParameterReturn(cltuGetParameterReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 9)) {
            CltuThrowEventReturn cltuThrowEventReturn = new CltuThrowEventReturn();
            cltuThrowEventReturn.decode(is, false);
            processThrowEventReturn(cltuThrowEventReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 12)) {
            CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation = new CltuAsyncNotifyInvocation();
            cltuAsyncNotifyInvocation.decode(is, false);
            processAsyncNotifyInvocation(cltuAsyncNotifyInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 13)) {
            CltuStatusReportInvocation cltuStatusReportInvocation = new CltuStatusReportInvocation();
            cltuStatusReportInvocation.decode(is, false);
            processStatusReportInvocation(cltuStatusReportInvocation);
        } else {
            logger.warn("Unexpected state berTag: {} ", berTag);
            throw new IllegalStateException("Unexpected message berTag: "+berTag);
        }
    }

    private void sendTransferData(byte[] cltu, CcsdsTime earliestTransmissionTime,
            CcsdsTime latestTransmissionTime, long delayMicrosec, boolean produceReport, int cltuId) {
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuTransferDataInvocation ctdi = new CltuTransferDataInvocation();
        ctdi.setInvokeId(getInvokeId());

        ctdi.setCltuIdentification(new CltuIdentification(cltuId));
        ctdi.setEarliestTransmissionTime(CcsdsTime.toSleConditional(earliestTransmissionTime, sleVersion));
        ctdi.setLatestTransmissionTime(CcsdsTime.toSleConditional(latestTransmissionTime, sleVersion));
        ctdi.setDelayTime(new Duration(delayMicrosec));
        ctdi.setSlduRadiationNotification(produceReport ? SLDU_NOTIFICATION_TRUE : SLDU_NOTIFICATION_FALSE);
        ctdi.setCltuData(new CltuData(cltu));
        cutp.setCltuTransferDataInvocation(ctdi);

        ctdi.setInvokerCredentials(getNonBindCredentials());
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processTransferDataReturn(CltuTransferDataReturn cltuTransferDataReturn) {
        verifyNonBindCredentials(cltuTransferDataReturn.getPerformerCredentials());

        if (logger.isTraceEnabled()) {
            logger.trace("Received CltuTransferDataReturn {}", cltuTransferDataReturn);
        }
        this.cltuBufferAvailable = cltuTransferDataReturn.getCltuBufferAvailable().longValue();

        Result r = cltuTransferDataReturn.getResult();
        if (r.getPositiveResult() != null) {
            int cltuId = cltuTransferDataReturn.getCltuIdentification().intValue() - 1;
            monitors.forEach(m -> ((CltuSleMonitor) m).onPositiveTransfer(cltuId));
        } else if (r.getNegativeResult() != null) {
            int cltuId = cltuTransferDataReturn.getCltuIdentification().intValue();
            monitors.forEach(m -> ((CltuSleMonitor) m).onNegativeTransfer(cltuId, r.getNegativeResult()));
        } else {
            logger.error("Received CLTRU transfer data return withotu positive or negative result {}",
                    cltuTransferDataReturn);
        }
    }

    private void sendThrowEvent(int eventIdentifier, byte[] eventQualifier, CompletableFuture<Void> cf) {
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuThrowEventInvocation ctei = new CltuThrowEventInvocation();
        ctei.setInvokeId(getInvokeId(cf));
        ctei.setInvokerCredentials(getNonBindCredentials());
        ctei.setEventIdentifier(new IntPosShort(eventIdentifier));
        ctei.setEventInvocationIdentification(new EventInvocationId(eventInvocationId++));
        ctei.setEventQualifier(new BerOctetString(eventQualifier));
        cutp.setCltuThrowEventInvocation(ctei);

        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processThrowEventReturn(CltuThrowEventReturn cltuThrowEventReturn) {
        verifyNonBindCredentials(cltuThrowEventReturn.getPerformerCredentials());

        CompletableFuture<Void> cf = getFuture(cltuThrowEventReturn.getInvokeId());
        CltuThrowEventReturn.Result r = cltuThrowEventReturn.getResult();
        if (r.getNegativeResult() != null) {
            DiagnosticCltuThrowEvent dcte = r.getNegativeResult();
            String err;
            if (dcte.getCommon() != null) {
                err = Constants.getCommonDiagnostic(dcte.getCommon().intValue());
            } else {
                err = Constants.getEnumName(dcte.getSpecific().intValue(), Constants.CltuThrowEventDiagnostics.values());
            }
            cf.completeExceptionally(new SleException(err));
        } else {
            cf.complete(null);
        }

    }

    private void sendGetParameter(int parameterId, CompletableFuture<SleParameter> cf) {
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuGetParameterInvocation cgpi = new CltuGetParameterInvocation();
        cgpi.setInvokeId(getInvokeId(cf));
        cgpi.setInvokerCredentials(getNonBindCredentials());
        cgpi.setCltuParameter(new CltuParameterName(parameterId));
        cutp.setCltuGetParameterInvocation(cgpi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processGetParameterReturn(CltuGetParameterReturn cltuGetParameterReturn) {
        verifyNonBindCredentials(cltuGetParameterReturn.getPerformerCredentials());

        CompletableFuture<SleParameter> cf = getFuture(cltuGetParameterReturn.getInvokeId());
        CltuGetParameterReturn.Result r = cltuGetParameterReturn.getResult();
        if (r.getNegativeResult() != null) {
            cf.completeExceptionally(
                    new SleException("Error getting parameter value: " + toString(r.getNegativeResult())));
        } else {
            cf.complete(new SleParameter(r.getPositiveResult()));
        }
    }

    private void processAsyncNotifyInvocation(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation) {
        verifyNonBindCredentials(cltuAsyncNotifyInvocation.getInvokerCredentials());
        if (logger.isTraceEnabled()) {
            logger.trace("Received CltuAsyncNotifyInvocation {}", cltuAsyncNotifyInvocation);
        }
        monitors.forEach(m -> ((CltuSleMonitor) m).onAsyncNotify(cltuAsyncNotifyInvocation));
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
    

    protected void sendStart(CompletableFuture<Void> cf) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call start while in state " + state));
            return;
        }
        
        changeState(State.STARTING);
        this.startingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuStartInvocation csi = new CltuStartInvocation();
        csi.setFirstCltuIdentification(new CltuIdentification(nextCltuId.get()));
        csi.setInvokeId(new InvokeId(1));
        csi.setInvokerCredentials(getNonBindCredentials());

        cutp.setCltuStartInvocation(csi);
        logger.debug("Sending start request {}", cutp);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processStartReturn(CltuStartReturn cltuStartReturn) {
        verifyNonBindCredentials(cltuStartReturn.getPerformerCredentials());

        if (state != State.STARTING) {
            peerAbort();
            return;
        }
        ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn.Result r = cltuStartReturn.getResult();
        if (r.getNegativeResult() != null) {
            logger.warn("Received negative response to start request: {}", r.getNegativeResult());
            changeState(State.READY);
            startingCf.completeExceptionally(new SleException("received negative result to start request: "+StringConverter.toString(r.getNegativeResult())));
        } else {
            changeState(State.ACTIVE);
            startingCf.complete(null);
        }
    }

    private void processStatusReportInvocation(CltuStatusReportInvocation cltuStatusReportInvocation) {
        verifyNonBindCredentials(cltuStatusReportInvocation.getInvokerCredentials());

        if (logger.isTraceEnabled()) {
            logger.trace("Received CltuStatusReport {}", cltuStatusReportInvocation);
        }
        monitors.forEach(m -> ((CltuSleMonitor) m).onCltuStatusReport(cltuStatusReportInvocation));
    }

    public long getCltuBufferAvailable() {
        return cltuBufferAvailable;
    }


    @Override
    protected ApplicationIdentifier getApplicationIdentifier() {
        return Constants.ApplicationIdentifier.fwdCltu;
    }

    private String toString(DiagnosticCltuGetParameter diagnostic) {
        if (diagnostic.getCommon() != null) {
            return Constants.getCommonDiagnostic(diagnostic.getCommon().intValue());
        } else {
            int x = diagnostic.getSpecific().intValue();
            return (x == 0) ? "unknown parameter" : "unknown(" + x + ")";
        }
    }

}
