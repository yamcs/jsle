package org.yamcs.sle.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.State;
import org.yamcs.sle.provider.FrameSink.UplinkResult;

import com.beanit.jasn1.ber.BerTag;
import com.beanit.jasn1.ber.types.BerInteger;

import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuGetParameterInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuStartInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuThrowEventInvocation;
import ccsds.sle.transfer.service.cltu.incoming.pdus.CltuTransferDataInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuGetParameterReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuProviderToUserPdu;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuThrowEventReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuTransferDataReturn;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuTransferDataReturn.Result;
import ccsds.sle.transfer.service.cltu.structures.BufferSize;
import ccsds.sle.transfer.service.cltu.structures.CltuIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuLastOk;
import ccsds.sle.transfer.service.cltu.structures.CltuLastProcessed;
import ccsds.sle.transfer.service.cltu.structures.CltuLastProcessed.CltuProcessed;
import ccsds.sle.transfer.service.cltu.structures.CltuNotification;
import ccsds.sle.transfer.service.cltu.structures.CltuStatus;
import ccsds.sle.transfer.service.cltu.structures.CurrentReportingCycle;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuStart;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuThrowEvent;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuTransferData;
import ccsds.sle.transfer.service.cltu.structures.NumberOfCltusProcessed;
import ccsds.sle.transfer.service.cltu.structures.NumberOfCltusRadiated;
import ccsds.sle.transfer.service.cltu.structures.NumberOfCltusReceived;
import ccsds.sle.transfer.service.cltu.structures.ProductionStatus;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.Diagnostics;
import ccsds.sle.transfer.service.common.types.InvokeId;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static org.yamcs.sle.Constants.*;

/**
 * Implementation for the
 * CCSDS RECOMMENDED STANDARD FOR SLE FCLTU SERVICE
 * CCSDS 912.1-B-4 August 2016
 * <p>
 * https://public.ccsds.org/Pubs/912x1b4.pdf
 * 
 * <p>
 * 
 * This class implements queueing for CLTUs and it relies on a {@link FrameSink} to uplink the CLTUs one by one.
 * 
 * <p>
 * Due to the java 8 limitations the uplink of the timed CLTUs (those with a defined earliestTransmissionTime) is not
 * very precise - with the current implementation it will be probably sent 1-2 milliseconds later than the time
 * specified.
 * If better precision is required, this has to be changed to send the CLTU a few milliseconds in
 * advance together with the time and the {@link FrameSink} should take care of the exact transmission start.
 * <p>
 * In any case the {@link FrameSink} has to return the time when the CLTU has been eventually radiated.
 * 
 */
public class CltuServiceProvider implements SleService {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CltuServiceProvider.class);

    FrameSink cltuUplinker;
    private final CltuServiceState cltuStatusReport = new CltuServiceState();

    PriorityQueue<TimedQueuedCltu> timedCltus = new PriorityQueue<TimedQueuedCltu>();
    BlockingQueue<QueuedCltu> cltus = new LinkedBlockingQueue<QueuedCltu>();
    private long minimumDelayTimeMicrosec;
    private final CltuParameters cltuParameters = new CltuParameters();

    int expectedCltuId;
    State state;

    final static QueuedCltu SIGNAL_TQC = new QueuedCltu();
    SleProvider sleProvider;

    Thread queueRunner;
    int sleVersion;

    public CltuServiceProvider(FrameSink frameSink) {
        this.cltuUplinker = frameSink;
    }

    @Override
    public void init(SleProvider server) {
        this.sleProvider = server;
        this.sleVersion = server.getVersionNumber();
        this.state = State.READY;
    }

    @Override
    public void processData(BerTag berTag, InputStream is) throws IOException {
        if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 10)) {
            CltuTransferDataInvocation cltuTransferDataInvocation = new CltuTransferDataInvocation();
            cltuTransferDataInvocation.decode(is, false);
            processTransferDataInvocation(cltuTransferDataInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0)) {
            CltuStartInvocation cltuStartInvocation = new CltuStartInvocation();
            cltuStartInvocation.decode(is, false);
            processStartInvocation(cltuStartInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 6)) {
            CltuGetParameterInvocation cltuGetParameterInvocation = new CltuGetParameterInvocation();
            cltuGetParameterInvocation.decode(is, false);
            processGetParameterInvocation(cltuGetParameterInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 8)) {
            CltuThrowEventInvocation cltuThrowEventInvocation = new CltuThrowEventInvocation();
            cltuThrowEventInvocation.decode(is, false);
            processThrowEventInvocation(cltuThrowEventInvocation);
        } else if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 2)) {
            SleStopInvocation sleStopInvocation = new SleStopInvocation();
            sleStopInvocation.decode(is, false);
            processSleStopInvocation(sleStopInvocation);
        } else {
            logger.warn("Unexpected berTag: {} ", berTag);
            throw new IllegalStateException("Unexpected berTag: " + berTag);
        }
    }

    private void processStartInvocation(CltuStartInvocation cltuStartInvocation) {
        logger.debug("Received CltuStartInvocation {}", cltuStartInvocation);
        if (state != State.READY) {
            logger.warn("wrong state {} for start invocation", state);
            sendNegativeStartReturn(cltuStartInvocation.getInvokeId(), 1);
            return;
        }
        int x = cltuUplinker.start(this);
        if (x > 0) {
            logger.warn("Cltu uplinker returned error {}", x);
            sendNegativeStartReturn(cltuStartInvocation.getInvokeId(), 1);
            return;
        }

        state = State.ACTIVE;
        cltuStatusReport.prodStatus = CltuProductionStatus.operational;
        CltuStartReturn.Result.PositiveResult pr = new CltuStartReturn.Result.PositiveResult();
        pr.setStartRadiationTime(CcsdsTime.toSle(CcsdsTime.now(), sleVersion));
        pr.setStopRadiationTime(COND_TIME_UNDEFINED);

        CltuStartReturn.Result r = new CltuStartReturn.Result();
        r.setPositiveResult(pr);
        expectedCltuId = cltuStartInvocation.getFirstCltuIdentification().intValue();
        queueRunner = new Thread(() -> runUplinkQueue());
        queueRunner.start();

        CltuStartReturn csr = new CltuStartReturn();
        csr.setResult(r);
        csr.setInvokeId(cltuStartInvocation.getInvokeId());
        csr.setPerformerCredentials(sleProvider.getNonBindCredentials());

        logger.debug("Sending CltuStartReturn {}", csr);
        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuStartReturn(csr);
        sleProvider.sendMessage(cptu);
    }

    private void sendNegativeStartReturn(InvokeId invokeId, int diagnostic) {
        DiagnosticCltuStart dcs = new DiagnosticCltuStart();
        dcs.setSpecific(new BerInteger(diagnostic));

        CltuStartReturn.Result r = new CltuStartReturn.Result();
        r.setNegativeResult(dcs);
        CltuStartReturn csr = new CltuStartReturn();
        csr.setResult(r);
        csr.setInvokeId(invokeId);
        csr.setPerformerCredentials(sleProvider.getNonBindCredentials());

        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuStartReturn(csr);
        sleProvider.sendMessage(cptu);
    }

    private void processTransferDataInvocation(CltuTransferDataInvocation cltuTransferDataInvocation) {
        logger.debug("Received CltuTransferDataInvocation {}", cltuTransferDataInvocation);
        sleProvider.verifyNonBindCredentials(cltuTransferDataInvocation.getInvokerCredentials());
        CcsdsTime ett = CcsdsTime.fromSle(cltuTransferDataInvocation.getEarliestTransmissionTime());
        QueuedCltu qc = (ett == null) ? new QueuedCltu() : new TimedQueuedCltu(ett);
        CcsdsTime now = CcsdsTime.now();
        cltuStatusReport.numCltuReceived++;

        int result = -1;

        qc.cltuId = cltuTransferDataInvocation.getCltuIdentification().intValue();
        qc.latestTransmissionTime = CcsdsTime.fromSle(cltuTransferDataInvocation.getLatestTransmissionTime());
        qc.delayTimeMicrosec = cltuTransferDataInvocation.getDelayTime().longValue();
        qc.report = cltuTransferDataInvocation.getSlduRadiationNotification().intValue() == 1;
        qc.cltuData = cltuTransferDataInvocation.getCltuData().value;
        qc.queued = now;

        if (qc.latestTransmissionTime != null && qc.latestTransmissionTime.compareTo(now) < 0) {
            result = 5;// late sdlu
        } else if (ett != null && qc.latestTransmissionTime != null && ett.compareTo(qc.latestTransmissionTime) > 0) {
            result = 3; // inconsistentTimeRange
        } else if (qc.delayTimeMicrosec < minimumDelayTimeMicrosec) {
            result = 6;// invalid delay time
        } else if (qc.cltuId != expectedCltuId) {
            result = 2; // out of seq
        } else if (qc.cltuData.length > cltuStatusReport.cltuBufferAvailable) {
            result = 1; // unable to store
        }

        CltuTransferDataReturn ctdr = new CltuTransferDataReturn();
        ctdr.setPerformerCredentials(sleProvider.getNonBindCredentials());
        ctdr.setInvokeId(cltuTransferDataInvocation.getInvokeId());
        ctdr.setCltuBufferAvailable(new BufferSize(cltuStatusReport.cltuBufferAvailable));

        Result r = new Result();
        if (result < 0) {// ok
            r.setPositiveResult(BER_NULL);
            if (qc instanceof TimedQueuedCltu) {
                timedCltus.add((TimedQueuedCltu) qc);
                cltus.add(SIGNAL_TQC); // to wake up the thread
            } else {
                cltus.add(qc);
            }
            cltuParameters.setExpectedCltuId(++expectedCltuId);
        } else {
            DiagnosticCltuTransferData d = new DiagnosticCltuTransferData();
            d.setSpecific(new BerInteger(result));
            r.setNegativeResult(d);
        }
        ctdr.setCltuIdentification(new CltuIdentification(expectedCltuId));
        ctdr.setResult(r);
        logger.debug("Sending CltuTransferDataReturn {}", ctdr);
        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuTransferDataReturn(ctdr);
        sleProvider.sendMessage(cptu);
    }

    protected void processSleStopInvocation(SleStopInvocation sleStopInvocation) {
        logger.debug("Received SleStopInvocation {}", sleStopInvocation);
        SleAcknowledgement ack = new SleAcknowledgement();

        ack.setCredentials(sleProvider.getNonBindCredentials());
        ack.setInvokeId(sleStopInvocation.getInvokeId());
        SleAcknowledgement.Result result = new SleAcknowledgement.Result();
        if (state == State.ACTIVE) {
            queueRunner.interrupt();
            state = State.READY;
            cltuStatusReport.prodStatus = CltuProductionStatus.configured;

            int x = cltuUplinker.stop(this);
            if (x > 0) {
                logger.warn("Cltu uplinker returned error {}", x);
                result.setNegativeResult(new Diagnostics(x));
            } else {
                result.setPositiveResult(BER_NULL);
            }
        } else {
            logger.warn("received stop while in state {}", state);
            result.setNegativeResult(new Diagnostics(127));// other reason
        }

        ack.setResult(result);
        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuStopReturn(ack);
        sleProvider.sendMessage(cptu);
    }

    private void processThrowEventInvocation(CltuThrowEventInvocation cltuThrowEventInvocation) {
        logger.debug("Received CltuThrowEventInvocation {}", cltuThrowEventInvocation);
        int evId = cltuThrowEventInvocation.getEventIdentifier().intValue();
        byte[] eventQualifier = cltuThrowEventInvocation.getEventQualifier().value;
        CltuThrowEventReturn.Result res = new CltuThrowEventReturn.Result();
        
        CltuThrowEventDiagnostics cted = cltuUplinker.throwEvent(evId, eventQualifier);
        if (cted == null) {
            res.setPositiveResult(BER_NULL);
        } else {
            DiagnosticCltuThrowEvent diag = new DiagnosticCltuThrowEvent();
            diag.setSpecific(new BerInteger(cted.id()));
            res.setNegativeResult(diag);
        }

        CltuThrowEventReturn ret = new CltuThrowEventReturn();
        ret.setPerformerCredentials(sleProvider.getNonBindCredentials());
        ret.setInvokeId(cltuThrowEventInvocation.getInvokeId());
        ret.setEventInvocationIdentification(cltuThrowEventInvocation.getEventInvocationIdentification());
        ret.setResult(res);
        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuThrowEventReturn(ret);
        sleProvider.sendMessage(cptu);
    }

    private void processGetParameterInvocation(CltuGetParameterInvocation cltuGetParameterInvocation) {
        logger.debug("Received CltuGetParameterInvocation {}", cltuGetParameterInvocation);
        CltuGetParameterReturn ret = new CltuGetParameterReturn();
        ret.setPerformerCredentials(sleProvider.getNonBindCredentials());
        ret.setInvokeId(cltuGetParameterInvocation.getInvokeId());
        CltuGetParameterReturn.Result res = cltuParameters.getParameter(cltuGetParameterInvocation.getCltuParameter());

        ret.setResult(res);

        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuGetParameterReturn(ret);
        sleProvider.sendMessage(cptu);
    }

    /**
     * Called when a CLTU has been radiated
     * 
     * @param cltuId
     * @param startTime
     * @param stopTime
     *            - can be null of the cltu has not been successfully radiated
     * @param cltuStatus
     */
    private void cltuRadiated(QueuedCltu qc, UplinkResult ur) {
        cltuStatusReport.cltuLastProcessedId = qc.cltuId;
        cltuStatusReport.cltuLastProcessedRadiationTime = ur.startTime;
        cltuStatusReport.cltuLastProcesseStatus = ur.cltuStatus;
        cltuStatusReport.numCltuProcessed++;

        if (ur.cltuStatus == ForwardDuStatus.radiated) {
            cltuStatusReport.cltuLastOkId = qc.cltuId;
            cltuStatusReport.cltuLastOkTime = ur.stopTime;
            cltuStatusReport.numCltuRadiated++;
        }
        cltuStatusReport.cltuBufferAvailable += qc.cltuData.length;

        CltuNotification cn = new CltuNotification();
        cn.setCltuRadiated(BER_NULL);
        sendCltuAsyncNotify(cn);
    }

    private void sendCltuAsyncNotify(CltuNotification cltuNotification) {
        CltuAsyncNotifyInvocation cani = new CltuAsyncNotifyInvocation();

        // invoker-credentials
        cani.setInvokerCredentials(sleProvider.getNonBindCredentials());
        // notification-type
        cani.setCltuNotification(cltuNotification);

        // cltu-last-processed
        cani.setCltuLastProcessed(getCltuLastProcessed(cltuStatusReport.cltuLastProcessedId,
                cltuStatusReport.cltuLastProcessedRadiationTime, cltuStatusReport.cltuLastProcesseStatus));

        cani.setCltuLastOk(getCltuLastOk(cltuStatusReport.cltuLastOkId, cltuStatusReport.cltuLastOkTime));

        // production-status
        cani.setProductionStatus(new ProductionStatus(cltuStatusReport.prodStatus.getId()));

        // uplink-status
        cani.setUplinkStatus(
                new ccsds.sle.transfer.service.cltu.structures.UplinkStatus(cltuStatusReport.uplinkStatus.getId()));

        logger.debug("Sending CltuAsyncNotifyInvocation {}", cani);
        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuAsyncNotifyInvocation(cani);
        sleProvider.sendMessage(cptu);
    }

    @Override
    public void sendStatusReport() {
        CltuStatusReportInvocation csri = new CltuStatusReportInvocation();

        // invoker-credentials
        csri.setInvokerCredentials(sleProvider.getNonBindCredentials());

        // cltu-last-processed

        csri.setCltuLastProcessed(getCltuLastProcessed(cltuStatusReport.cltuLastProcessedId,
                cltuStatusReport.cltuLastProcessedRadiationTime, cltuStatusReport.cltuLastProcesseStatus));

        // cltu-last-OK
        CltuLastOk cltuLastOk = getCltuLastOk(cltuStatusReport.cltuLastOkId, cltuStatusReport.cltuLastOkTime);
        csri.setCltuLastOk(cltuLastOk);

        // production-status
        csri.setCltuProductionStatus(new ProductionStatus(cltuStatusReport.prodStatus.getId()));

        // uplink-status
        csri.setUplinkStatus(
                new ccsds.sle.transfer.service.cltu.structures.UplinkStatus(cltuStatusReport.uplinkStatus.getId()));
        csri.setNumberOfCltusReceived(new NumberOfCltusReceived(cltuStatusReport.numCltuReceived));
        csri.setNumberOfCltusProcessed(new NumberOfCltusProcessed(cltuStatusReport.numCltuProcessed));
        csri.setNumberOfCltusRadiated(new NumberOfCltusRadiated(cltuStatusReport.numCltuRadiated));
        // cltu-buffer-available
        csri.setCltuBufferAvailable(new BufferSize(cltuStatusReport.cltuBufferAvailable));

        logger.debug("Sending CltuStatusReportInvocation {}", csri);

        CltuProviderToUserPdu cptu = new CltuProviderToUserPdu();
        cptu.setCltuStatusReportInvocation(csri);
        sleProvider.sendMessage(cptu);
    }

    CltuLastProcessed getCltuLastProcessed(int cltuId, CcsdsTime time, ForwardDuStatus cltuStatus) {
        CltuLastProcessed cltuLastProcessed = new CltuLastProcessed();
        if (cltuId == -1) {
            cltuLastProcessed.setNoCltuProcessed(BER_NULL);
        } else {
            CltuProcessed cltuProcessed = new CltuProcessed();
            cltuProcessed.setCltuIdentification(new CltuIdentification(cltuId));
            cltuProcessed.setRadiationStartTime(CcsdsTime.toSleConditional(time, sleVersion));
            cltuProcessed.setCltuStatus(new CltuStatus(cltuStatus.getId()));
            cltuLastProcessed.setCltuProcessed(cltuProcessed);
        }
        return cltuLastProcessed;
    }

    CltuLastOk getCltuLastOk(int cltuId, CcsdsTime time) {
        CltuLastOk cltuLastOk = new CltuLastOk();
        if (cltuId == -1) {
            cltuLastOk.setNoCltuOk(BER_NULL);
        } else {
            CltuLastOk.CltuOk cltuOk = new CltuLastOk.CltuOk();
            cltuOk.setCltuIdentification(new CltuIdentification(cltuId));
            cltuOk.setRadiationStopTime(CcsdsTime.toSle(time, sleVersion));
            cltuLastOk.setCltuOk(cltuOk);
        }
        return cltuLastOk;
    }

    @Override
    public void abort() {
        if (queueRunner != null) {
            queueRunner.interrupt();
        }
    }

    @Override
    public void unbind() {
        // nothing to do
    }

    /*
     * This runs in a separated thread
     */
    private void runUplinkQueue() {
        while (state == State.ACTIVE) {
            CcsdsTime now = CcsdsTime.now();
            long timeToSleep = Long.MAX_VALUE;

            TimedQueuedCltu tqc = timedCltus.poll();
            if (tqc != null) {
                timeToSleep = delta(tqc.earliestTransmissionTime, now);
                if (timeToSleep < 0) {
                    uplinkCltu(tqc);
                    continue;
                }
            }
            QueuedCltu qc;
            try {
                qc = cltus.poll(timeToSleep, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return;
            }
            if (qc == null) {
                uplinkCltu(tqc);
            } else if (qc == SIGNAL_TQC) {// this means that a new Timed CLTU has been put in the TimedQueuedCltu
                timedCltus.add(tqc);
            } else {
                uplinkCltu(qc);
            }
        }
    }

    // returns t1-t2 in milliseconds
    private long delta(CcsdsTime t1, CcsdsTime t2) {
        return CcsdsTime.MS_IN_DAY * (t1.getNumDays() - t2.getNumDays())
                + (t1.getPicosecInDay() - t2.getPicosecInDay()) / 1000_000_000;
    }

    private void uplinkCltu(QueuedCltu qc) {
        UplinkResult ur = cltuUplinker.uplink(qc.cltuData);
        cltuRadiated(qc, ur);
    }

    static class CltuServiceState {
        int cltuLastProcessedId = -1;
        CcsdsTime cltuLastProcessedRadiationTime;
        ForwardDuStatus cltuLastProcesseStatus;

        int cltuLastOkId = -1;
        CcsdsTime cltuLastOkTime;

        CltuProductionStatus prodStatus = CltuProductionStatus.configured;
        UplinkStatus uplinkStatus = UplinkStatus.uplinkStatusNotAvailable;
        int numCltuReceived;
        int numCltuProcessed;
        int numCltuRadiated;
        int cltuBufferAvailable = 1024 * 1024;
    }

    static class QueuedCltu {
        int cltuId;
        CcsdsTime queued;

        CcsdsTime latestTransmissionTime;
        long delayTimeMicrosec;
        boolean report;
        byte[] cltuData;
    }

    static class TimedQueuedCltu extends QueuedCltu implements Comparable<TimedQueuedCltu> {
        CcsdsTime earliestTransmissionTime;

        TimedQueuedCltu(CcsdsTime earliestTransmissionTime) {
            this.earliestTransmissionTime = earliestTransmissionTime;
        }

        @Override
        public int compareTo(TimedQueuedCltu o) {
            return this.earliestTransmissionTime.compareTo(o.earliestTransmissionTime);
        }
    }

    @Override
    public State getState() {
        return state;
    }

    public int getExpectedCltuId() {
        return expectedCltuId;
    }

    public int getExpectedEventInvocationId() {
        return 0;// TODO
    }

    public CurrentReportingCycle getReportingCycle() {
        return null;
    }

    public CltuParameters getParameters() {
        return cltuParameters;
    }

}
