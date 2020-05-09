package org.yamcs.sle.provider;

import java.io.IOException;
import java.io.InputStream;

import org.yamcs.sle.State;
import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.RafProductionStatus;
import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Isp1Authentication;

import com.beanit.jasn1.ber.BerTag;
import com.beanit.jasn1.ber.types.BerInteger;

import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.Diagnostics;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.SpaceLinkDataUnit;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafGetParameterInvocation;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafStartInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.FrameOrNotification;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafProviderToUserPdu;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStartReturn;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStatusReportInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferBuffer;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation.PrivateAnnotation;
import ccsds.sle.transfer.service.raf.structures.AntennaId;
import ccsds.sle.transfer.service.raf.structures.CarrierLockStatus;
import ccsds.sle.transfer.service.raf.structures.DiagnosticRafStart;
import ccsds.sle.transfer.service.raf.structures.FrameSyncLockStatus;
import ccsds.sle.transfer.service.raf.structures.SymbolLockStatus;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static org.yamcs.sle.Constants.*;

public class RafServiceProviderHandler extends AbstractServiceProviderHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CltuServiceProviderHandler.class);
    private static final PrivateAnnotation NULL_ANNOTATION = new PrivateAnnotation();
    static {
        NULL_ANNOTATION.setNull(BER_NULL);
    };
    
    StatusReport statusReport;
    private AntennaId antennaId;
    
    public RafServiceProviderHandler(Isp1Authentication auth, SleAttributes attr) {
        super(auth, attr);
    }

    @Override
    protected void processData(BerTag berTag, InputStream is) throws IOException {
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0)) {
            RafStartInvocation rafStartInvocation = new RafStartInvocation();
            rafStartInvocation.decode(is, false);
            processStartInvocation(rafStartInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 6)) {
            RafGetParameterInvocation rafGetParameterInvocation = new RafGetParameterInvocation();
            rafGetParameterInvocation.decode(is, false);
            processRafParameterInvocation(rafGetParameterInvocation);
        } else {
            logger.warn("Unexpected berTag: {} ", berTag);
            throw new IllegalStateException("Unexpected berTag: " + berTag);
        }
        
    }

    private void processStartInvocation(RafStartInvocation rafStartInvocation) {
        logger.debug("Received RafStartInvocation {}", rafStartInvocation);
        RafStartReturn.Result r = new RafStartReturn.Result();

        if (state != State.READY) {
            logger.warn("wrong state {} for start invocation", state);
            DiagnosticRafStart dcs = new DiagnosticRafStart();
            dcs.setSpecific(new BerInteger(1));
            r.setNegativeResult(dcs);
        } else {
            state = State.ACTIVE;
            r.setPositiveResult(BER_NULL);
        }

        RafStartReturn rsr = new RafStartReturn();
        rsr.setResult(r);
        rsr.setInvokeId(rafStartInvocation.getInvokeId());
        rsr.setPerformerCredentials(getNonBindCredentials());

        logger.debug("Sending RafStartReturn {}", rsr);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafStartReturn(rsr);
        channelHandlerContext.writeAndFlush(rptu);
        
    }


    @Override
    protected void processSleStopInvocation(SleStopInvocation sleStopInvocation) {
        logger.debug("Received SleStopInvocation {}", sleStopInvocation);
        SleAcknowledgement ack = new SleAcknowledgement();

        ack.setCredentials(getNonBindCredentials());
        ack.setInvokeId(sleStopInvocation.getInvokeId());
        SleAcknowledgement.Result result = new SleAcknowledgement.Result();
        if (state == State.ACTIVE) {
            state = State.READY;
            result.setPositiveResult(BER_NULL);
        } else {
            logger.warn("received stop while in state {}", state);
            result.setNegativeResult(new Diagnostics(127));// other reason
        }

        ack.setResult(result);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafStopReturn(ack);
        channelHandlerContext.writeAndFlush(rptu);
    }

    @Override
    protected void sendStatusReport() {
        RafStatusReportInvocation rsri = new RafStatusReportInvocation();
        rsri.setDeliveredFrameNumber(new IntUnsignedLong(statusReport.numFramesDelivered));
        rsri.setErrorFreeFrameNumber(new IntUnsignedLong(statusReport.numErrorFreeFramesDelivered));
       
        rsri.setFrameSyncLockStatus(new FrameSyncLockStatus(statusReport.frameSyncLockStatus.getId()));
        rsri.setCarrierLockStatus(new CarrierLockStatus(statusReport.carrierLockStatus.getId()));
        rsri.setSubcarrierLockStatus(new CarrierLockStatus(statusReport.carrierLockStatus.getId()));
        rsri.setSymbolSyncLockStatus(new SymbolLockStatus(statusReport.symbolLockStatus.getId()));
        
       rsri.setProductionStatus(new ccsds.sle.transfer.service.raf.structures.RafProductionStatus(statusReport.productionStatus.getId()));
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafStatusReportInvocation(rsri);
        channelHandlerContext.writeAndFlush(rptu);
    }
    
    public void sendFrame(CcsdsTime ert, int frameQuality, int dataLinkContinuity, byte[] data) {
         RafTransferDataInvocation rtdi = new RafTransferDataInvocation();
        rtdi.setInvokerCredentials(getNonBindCredentials());
        rtdi.setEarthReceiveTime(CcsdsTime.toSle(ert, sleVersion));
        rtdi.setData(new SpaceLinkDataUnit(data));
        rtdi.setAntennaId(antennaId);
        rtdi.setDataLinkContinuity(new BerInteger(dataLinkContinuity));
        rtdi.setDeliveredFrameQuality(new ccsds.sle.transfer.service.raf.structures.FrameQuality(frameQuality));
        rtdi.setPrivateAnnotation(NULL_ANNOTATION);
        
        RafTransferBuffer rtb = new RafTransferBuffer();
        FrameOrNotification fon = new FrameOrNotification();
        fon.setAnnotatedFrame(rtdi);
        rtb.getFrameOrNotification().add(fon);
        
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafTransferBuffer(rtb);
        channelHandlerContext.writeAndFlush(rptu);
    }

    private void processRafParameterInvocation(RafGetParameterInvocation rafGetParameterInvocation) {
        // TODO Auto-generated method stub
        
    }

    static class StatusReport {
       int numErrorFreeFramesDelivered;
       int numFramesDelivered;
       LockStatus frameSyncLockStatus;
       LockStatus symbolLockStatus;
       LockStatus carrierLockStatus;
       LockStatus subcarrierLockStatus;
       RafProductionStatus productionStatus;
    }
}
