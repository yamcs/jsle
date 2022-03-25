package org.yamcs.sle.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.yamcs.sle.State;
import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.ProductionStatus;
import org.yamcs.sle.CcsdsTime;

import com.beanit.jasn1.ber.BerTag;
import com.beanit.jasn1.ber.types.BerInteger;
import com.beanit.jasn1.ber.types.BerOctetString;

import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.Diagnostics;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.common.types.SpaceLinkDataUnit;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafGetParameterInvocation;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafStartInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.FrameOrNotification;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafProviderToUserPdu;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStartReturn;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStatusReportInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafSyncNotifyInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferBuffer;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation.PrivateAnnotation;
import ccsds.sle.transfer.service.raf.structures.AntennaId;
import ccsds.sle.transfer.service.raf.structures.CarrierLockStatus;
import ccsds.sle.transfer.service.raf.structures.DiagnosticRafStart;
import ccsds.sle.transfer.service.raf.structures.FrameSyncLockStatus;
import ccsds.sle.transfer.service.raf.structures.SymbolLockStatus;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfProviderToUserPdu;
import ccsds.sle.transfer.service.raf.structures.Notification;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static org.yamcs.sle.Constants.*;

public class RafServiceProvider extends RacfServiceProvider {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CltuServiceProvider.class);
    private static final PrivateAnnotation NULL_ANNOTATION = new PrivateAnnotation();
    static {
        NULL_ANNOTATION.setNull(BER_NULL);
    };

    StatusReport statusReport = new StatusReport();
    private AntennaId antennaId;
    SleProvider provider;
    State state;
    int sleVersion;
    FrameSource frameDownlinker;
    private final RafParameters rafParameters = new RafParameters();
    

    public RafServiceProvider(FrameSource frameDownlinker) {
        this.frameDownlinker = frameDownlinker;
        this.antennaId = new AntennaId();
        antennaId.setLocalForm(new BerOctetString("jsle-bridge".getBytes()));
    }

    @Override
    public void init(SleProvider provider) {
        this.provider = provider;
        this.state = State.READY;
        this.sleVersion = provider.getVersionNumber();
    }

    @Override
    public void processData(BerTag berTag, InputStream is) throws IOException {
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0)) {
            RafStartInvocation rafStartInvocation = new RafStartInvocation();
            rafStartInvocation.decode(is, false);
            processStartInvocation(rafStartInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 6)) {
            RafGetParameterInvocation rafGetParameterInvocation = new RafGetParameterInvocation();
            rafGetParameterInvocation.decode(is, false);
            processRafParameterInvocation(rafGetParameterInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 2)) {
            SleStopInvocation sleStopInvocation = new SleStopInvocation();
            sleStopInvocation.decode(is, false);
            processSleStopInvocation(sleStopInvocation);
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
            sendNegativeStartResponse(rafStartInvocation.getInvokeId(), 1);
            return;
        }
        CcsdsTime start = CcsdsTime.fromSle(rafStartInvocation.getStartTime());
        CcsdsTime stop = CcsdsTime.fromSle(rafStartInvocation.getStopTime());

        CompletableFuture<Integer> cf = frameDownlinker.start(this, start, stop);
        cf.thenAccept(res -> {
            if (res >= 0) {
                logger.warn("frame downlinker returned error {} when starting", res);
                sendNegativeStartResponse(rafStartInvocation.getInvokeId(), res);
                return;
            }

            state = State.ACTIVE;
            r.setPositiveResult(BER_NULL);

            RafStartReturn rsr = new RafStartReturn();
            rsr.setResult(r);
            rsr.setInvokeId(rafStartInvocation.getInvokeId());
            rsr.setPerformerCredentials(provider.getNonBindCredentials());

            logger.debug("Sending RafStartReturn {}", rsr);
            RafProviderToUserPdu rptu = new RafProviderToUserPdu();
            rptu.setRafStartReturn(rsr);
            provider.sendMessage(rptu);
        });
    }

    private void sendNegativeStartResponse(InvokeId invokeId, int diagnostic) {
        RafStartReturn.Result r = new RafStartReturn.Result();
        DiagnosticRafStart dcs = new DiagnosticRafStart();
        dcs.setSpecific(new BerInteger(diagnostic));
        r.setNegativeResult(dcs);
        RafStartReturn rsr = new RafStartReturn();
        rsr.setPerformerCredentials(provider.getNonBindCredentials());

        logger.debug("Sending RafStartReturn {}", rsr);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafStartReturn(rsr);
        provider.sendMessage(rptu);
    }

    protected void processSleStopInvocation(SleStopInvocation sleStopInvocation) {
        logger.debug("Received SleStopInvocation {}", sleStopInvocation);
        SleAcknowledgement ack = new SleAcknowledgement();

        ack.setCredentials(provider.getNonBindCredentials());
        ack.setInvokeId(sleStopInvocation.getInvokeId());
        SleAcknowledgement.Result result = new SleAcknowledgement.Result();
        if (state == State.ACTIVE) {
            state = State.READY;
            result.setPositiveResult(BER_NULL);
            frameDownlinker.stop(this);
        } else {
            logger.warn("received stop while in state {}", state);
            result.setNegativeResult(new Diagnostics(127));// other reason
        }

        ack.setResult(result);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafStopReturn(ack);
        provider.sendMessage(rptu);
    }

    @Override
    public void sendStatusReport() {
        RafStatusReportInvocation rsri = new RafStatusReportInvocation();
        rsri.setInvokerCredentials(provider.getNonBindCredentials());

        rsri.setDeliveredFrameNumber(new IntUnsignedLong(statusReport.numFramesDelivered));
        rsri.setErrorFreeFrameNumber(new IntUnsignedLong(statusReport.numErrorFreeFramesDelivered));

        rsri.setFrameSyncLockStatus(new FrameSyncLockStatus(statusReport.frameSyncLockStatus.getId()));
        rsri.setCarrierLockStatus(new CarrierLockStatus(statusReport.carrierLockStatus.getId()));
        rsri.setSubcarrierLockStatus(new CarrierLockStatus(statusReport.carrierLockStatus.getId()));
        rsri.setSymbolSyncLockStatus(new SymbolLockStatus(statusReport.symbolLockStatus.getId()));

        rsri.setProductionStatus(new ccsds.sle.transfer.service.raf.structures.RafProductionStatus(
                statusReport.productionStatus.getId()));
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafStatusReportInvocation(rsri);
        provider.sendMessage(rptu);
    }

    /**
     * Called by the {@link FrameSource} to send a new frame.
     * 
     * @param ert
     * @param frameQuality
     * @param dataLinkContinuity
     *            from CCSDS spec:
     *            <ul>
     *            <li>a value of ‘–1’ shall indicate that this is the first frame after the start of production;</li
     *            <li> a value of ‘0’ shall indicate that this frame is the direct successor to the last frame acquired
     *            from the space link by RAF production;</li
     *            <li> any non-zero positive value shall indicate that this frame is not the direct successor to the
     *            last frame acquired from the space link:</li
     *            <li> a non-zero positive value further indicates an estimate of the number of frames that were missed
     *            since the last frame acquired before this frame;</li>
     *            <li>a value of ‘1’ may be used if no better estimate is available.</li>
     *            </ul>
     * @param data
     *            frame data
     * @param dataOffset
     *            where in the data the buffer the frame starts
     * @param dataLength
     *            the length of the frame data
     */
    public void sendFrame(CcsdsTime ert, FrameQuality frameQuality, int dataLinkContinuity, byte[] data, int dataOffset,
            int dataLength) {
        RafTransferDataInvocation rtdi = new RafTransferDataInvocation();
        rtdi.setInvokerCredentials(provider.getNonBindCredentials());
        rtdi.setEarthReceiveTime(CcsdsTime.toSle(ert, sleVersion));
        if (dataOffset != 0 || data.length != dataLength) {
            data = Arrays.copyOfRange(data, dataOffset, dataOffset + dataLength);
        }
        rtdi.setData(new SpaceLinkDataUnit(data));
        rtdi.setAntennaId(antennaId);
        rtdi.setDataLinkContinuity(new BerInteger(dataLinkContinuity));
        rtdi.setDeliveredFrameQuality(new ccsds.sle.transfer.service.raf.structures.FrameQuality(frameQuality.id()));
        rtdi.setPrivateAnnotation(NULL_ANNOTATION);

        RafTransferBuffer rtb = new RafTransferBuffer();
        FrameOrNotification fon = new FrameOrNotification();
        fon.setAnnotatedFrame(rtdi);
        rtb.getFrameOrNotification().add(fon);

        logger.trace("Sending RafTransferBuffer {}", rtb);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafTransferBuffer(rtb);
        provider.sendMessage(rptu);
    }

    public void sendEof() {
        RafSyncNotifyInvocation rsni = new RafSyncNotifyInvocation();
        rsni.setInvokerCredentials(provider.getNonBindCredentials());

        Notification notif = new Notification();
        notif.setEndOfData(BER_NULL);
        rsni.setNotification(notif);

        RafTransferBuffer rtb = new RafTransferBuffer();
        FrameOrNotification fon = new FrameOrNotification();
        fon.setSyncNotification(rsni);
        rtb.getFrameOrNotification().add(fon);

        logger.trace("Sending RafTransferBuffer(eof) {}", rtb);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafTransferBuffer(rtb);
        provider.sendMessage(rptu);
    }

    private void processRafParameterInvocation(RafGetParameterInvocation rafGetParameterInvocation) {
        logger.debug("Received RafGetParameterInvocation {}", rafGetParameterInvocation);
        RafProviderToUserPdu rptu = new RafProviderToUserPdu();
        rptu.setRafGetParameterReturn(rafParameters.processGetInvocation(rafGetParameterInvocation));

        provider.sendMessage(rptu);
    }

    static class StatusReport {
        int numErrorFreeFramesDelivered;
        int numFramesDelivered;
        LockStatus frameSyncLockStatus = LockStatus.inLock;
        LockStatus symbolLockStatus = LockStatus.inLock;
        LockStatus carrierLockStatus = LockStatus.inLock;
        LockStatus subcarrierLockStatus = LockStatus.inLock;
        ProductionStatus productionStatus = ProductionStatus.running;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void abort() {
        // do nothing
    }

    @Override
    public void unbind() {
        // do nothing
    }
}
