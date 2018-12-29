package org.yamcs.sle;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.openmuc.jasn1.ber.BerTag;
import org.openmuc.jasn1.ber.types.BerOctetString;

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
import ccsds.sle.transfer.service.cltu.structures.CltuData;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter;
import ccsds.sle.transfer.service.cltu.structures.CltuIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuParameterName;
import ccsds.sle.transfer.service.cltu.structures.EventInvocationId;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Duration;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.InvokeId;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static org.yamcs.sle.Constants.*;

public class CltuServiceUserHandler extends AbstractServiceUserHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Isp1Handler.class);
    int cltuId = 1;
    int eventInvocationId = 1;
    
    private volatile long cltuBufferAvailable;
    
    public CltuServiceUserHandler(Isp1Authentication auth, String responderPortId, String initiatorId) {
        super(auth, responderPortId, initiatorId);
    }

    protected void processData(BerTag berTag, InputStream is) throws IOException {
        System.out.println("berTga: "+berTag);
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 11)) {
            CltuTransferDataReturn cltuTransferDataReturn = new CltuTransferDataReturn();
            cltuTransferDataReturn.decode(is, false);
            processTransferDataReturn(cltuTransferDataReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
            CltuStartReturn cltuStartReturn = new CltuStartReturn();
            cltuStartReturn.decode(is, false);
            processStartReturn(cltuStartReturn);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 3)) {
            SleAcknowledgement cltuStopReturn = new SleAcknowledgement();
            cltuStopReturn.decode(is, false);
            processStopReturn(cltuStopReturn);
        }  else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
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
            throw new IllegalStateException();
        }
    }
    
    public void transferCltu(byte[] cltu) {
        transferCltu(cltu, COND_TIME_UNDEFINED, COND_TIME_UNDEFINED, 0, false);
    }
    
    public void transferCltu(byte[] cltu, ConditionalTime earliestTransmissionTime,
            ConditionalTime latestTransmissionTime, long delayMicrosec, boolean produceReport) {
        channelHandlerContext.executor().execute(() -> sendTransferData(cltu, earliestTransmissionTime, latestTransmissionTime, delayMicrosec, produceReport));
    }
    
    private void sendTransferData(byte[] cltu, ConditionalTime earliestTransmissionTime,
            ConditionalTime latestTransmissionTime, long delayMicrosec, boolean produceReport) {
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuTransferDataInvocation ctdi = new CltuTransferDataInvocation();
        ctdi.setInvokeId(getInvokeId());
        
        ctdi.setCltuIdentification(getCltuId());
        ctdi.setEarliestTransmissionTime(earliestTransmissionTime);
        ctdi.setLatestTransmissionTime(latestTransmissionTime);
        ctdi.setDelayTime(new Duration(delayMicrosec));
        ctdi.setSlduRadiationNotification(produceReport?SLDU_NOTIFICATION_TRUE:SLDU_NOTIFICATION_FALSE);
        ctdi.setCltuData(new CltuData(cltu));
        cutp.setCltuTransferDataInvocation(ctdi);
        
        ctdi.setInvokerCredentials(getNonBindCredentials());
        channelHandlerContext.writeAndFlush(cutp);
    }
    
    
    private CltuIdentification getCltuId() {
        return new CltuIdentification(cltuId++);
    }

    private void processTransferDataReturn(CltuTransferDataReturn cltuTransferDataReturn) {
        if(logger.isTraceEnabled()) {
            logger.trace("Received CltuTransferDataReturn {}", cltuTransferDataReturn);
        }
        this.cltuBufferAvailable = cltuTransferDataReturn.getCltuBufferAvailable().longValue();
    }

    public CompletableFuture<Void> throwEvent(int eventIdentifier, byte[] eventQualifier) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendThrowEvent(eventIdentifier, eventQualifier, cf));
        return cf;
    }
    private void sendThrowEvent(int eventIdentifier, byte[] eventQualifier, CompletableFuture<Void> cf) {
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuThrowEventInvocation ctei = new CltuThrowEventInvocation();
        ctei.setInvokeId(getInvokeId(cf));
        ctei.setInvokerCredentials(getNonBindCredentials());
        ctei.setEventIdentifier(new IntPosShort(eventIdentifier));
        ctei.setEventInvocationIdentification(new EventInvocationId(eventInvocationId++));
        ctei.setEventQualifier(new BerOctetString(eventQualifier));
        
        channelHandlerContext.writeAndFlush(cutp);
    }
    
    private void processThrowEventReturn(CltuThrowEventReturn cltuThrowEventReturn) {
        CompletableFuture<Void> cf = getFuture(cltuThrowEventReturn.getInvokeId());
        CltuThrowEventReturn.Result r = cltuThrowEventReturn.getResult();
        if(r.getNegativeResult()!=null) {
            cf.completeExceptionally(new SleException("error getting parameter", r.getNegativeResult()));
        } else {
            cf.complete(null);
        }

    }
 

    public CompletableFuture<CltuGetParameter> getParameter(int parameterId) {
        CompletableFuture<CltuGetParameter> cf = new CompletableFuture<>();
        channelHandlerContext.executor().execute(() -> sendGetParameter(parameterId, cf));
        return cf;
    }
   
    private void sendGetParameter(int parameterId, CompletableFuture<CltuGetParameter> cf) {
        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuGetParameterInvocation cgpi = new CltuGetParameterInvocation();
        cgpi.setInvokeId(getInvokeId(cf));
        cgpi.setInvokerCredentials(getNonBindCredentials());
        cgpi.setCltuParameter(new CltuParameterName(parameterId));
        cutp.setCltuGetParameterInvocation(cgpi);
        channelHandlerContext.writeAndFlush(cutp);
    }
    
   
    private void processGetParameterReturn(CltuGetParameterReturn cltuGetParameterReturn) {
        CompletableFuture<CltuGetParameter> cf = getFuture(cltuGetParameterReturn.getInvokeId());
        CltuGetParameterReturn.Result r = cltuGetParameterReturn.getResult();
        if(r.getNegativeResult()!=null) {
            cf.completeExceptionally(new SleException("error getting parameter", r.getNegativeResult()));
        } else {
            cf.complete(r.getPositiveResult());
        }
    }

    

    private void processAsyncNotifyInvocation(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation) {
        if(logger.isTraceEnabled()) {
            logger.trace("Received CltuAsyncNotifyInvocation {}", cltuAsyncNotifyInvocation);
        }
        System.out.println("received: CltuAsyncNotifyInvocation"+cltuAsyncNotifyInvocation);
     
        // TODO Auto-generated method stub

    }

    protected void sendStart(CompletableFuture<Void> cf) {
        if (state != State.READY) {
            cf.completeExceptionally(new SleException("Cannot call start while in state " + state));
            return;
        }
        state = State.STARTING;
        this.startingCf = cf;

        CltuUserToProviderPdu cutp = new CltuUserToProviderPdu();

        CltuStartInvocation csi = new CltuStartInvocation();
        csi.setFirstCltuIdentification(new CltuIdentification(1));
        csi.setInvokeId(new InvokeId(1));
        csi.setInvokerCredentials(getNonBindCredentials());

        cutp.setCltuStartInvocation(csi);
        channelHandlerContext.writeAndFlush(cutp);
    }

    private void processStartReturn(CltuStartReturn cltuStartReturn) {
        if (state != State.STARTING) {
            peerAbort();
            return;
        }
        ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn.Result r = cltuStartReturn.getResult();
        if (r.getNegativeResult() != null) {
            startingCf.completeExceptionally(new SleException("failed to start", r.getNegativeResult()));
            state = State.READY;
        } else {
            startingCf.complete(null);
            state = State.ACTIVE;
        }
    }
    private void processStatusReportInvocation(CltuStatusReportInvocation cltuStatusReportInvocation) {
        if(logger.isTraceEnabled()) {
            logger.trace("Received CltuStatusReport {}", cltuStatusReportInvocation);
        }
        System.out.println(Instant.now()+": received cltuStatusReport "+cltuStatusReportInvocation);
    }

    public long getCltuBufferAvailable() {
        return cltuBufferAvailable;
    }
}
