package org.yamcs.sle.provider;

import org.yamcs.sle.Constants;
import org.yamcs.sle.Constants.DeliveryMode;
import org.yamcs.sle.Constants.NotificationMode;
import org.yamcs.sle.ParameterName;

import com.beanit.jasn1.ber.types.BerInteger;

import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuGetParameterReturn;
import ccsds.sle.transfer.service.cltu.structures.ClcwGvcId;
import ccsds.sle.transfer.service.cltu.structures.ClcwPhysicalChannel;
import ccsds.sle.transfer.service.cltu.structures.CltuDeliveryMode;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParAcquisitionSequenceLength;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParBitLockRequired;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParClcwGlobalVcId;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParClcwPhysicalChannel;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParCltuIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParDeliveryMode;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParEventInvocationIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParMaximumCltuLength;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParMinReportingCycle;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParMinimumDelayTime;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParModulationFrequency;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParModulationIndex;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParNotificationMode;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParPlop1IdleSequenceLength;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParPlopInEffect;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParProtocolAbortMode;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParReportingCycle;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParReturnTimeout;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParRfAvailableRequired;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter.ParSubcarrierToBitRateRatio;
import ccsds.sle.transfer.service.cltu.structures.CltuIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuParameterName;
import ccsds.sle.transfer.service.cltu.structures.CurrentReportingCycle;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuGetParameter;
import ccsds.sle.transfer.service.cltu.structures.EventInvocationId;
import ccsds.sle.transfer.service.cltu.structures.ModulationFrequency;
import ccsds.sle.transfer.service.cltu.structures.ModulationIndex;
import ccsds.sle.transfer.service.cltu.structures.SubcarrierDivisor;
import ccsds.sle.transfer.service.cltu.structures.TimeoutPeriod;
import ccsds.sle.transfer.service.common.pdus.ReportingCycle;
import ccsds.sle.transfer.service.common.types.Duration;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.IntUnsignedShort;

public class CltuParameters {
    ClcwGvcId clcwGvCid;
    ClcwPhysicalChannel clcwPhysicalChannel;

    int acquisitionSequenceLength = 16;
    int bitLockRequired = 0;
    int duration;
    int maxCltuLength;

    private int minReportingCycle;

    private int modulationFrequency;

    private int modulationIndex;
    NotificationMode notificationMode = NotificationMode.IMMEDIATE;

    private int plop1IdleSequenceLength;
    private int plopInEffect;
    private int protocolAbortMode;

    private int expectedCltuId;

    CurrentReportingCycle reportingCycle;
    private int expectedEventInvocationId;
    private int returnTimeoutPeriod;
    private int rfAvailableRequired;
    private int subcarrierToBitRateRatio;


    public CltuParameters() {
        clcwGvCid = new ClcwGvcId();
        clcwGvCid.setNotConfigured(Constants.BER_NULL);

        clcwPhysicalChannel = new ClcwPhysicalChannel();
        clcwPhysicalChannel.setNotConfigured(Constants.BER_NULL);

        reportingCycle = new CurrentReportingCycle();
        reportingCycle.setPeriodicReportingOn(new ReportingCycle(10));
    }

    public CltuGetParameterReturn.Result getParameter(CltuParameterName cltuPara) {
        ParameterName pname = ParameterName.byId(cltuPara.intValue());
        CltuGetParameterReturn.Result res = new CltuGetParameterReturn.Result();
        CltuGetParameter positiveResult = new CltuGetParameter();
        DiagnosticCltuGetParameter negativeResult = null;

        switch (pname) {
        case acquisitionSequenceLength:
            ParAcquisitionSequenceLength pasl = new ParAcquisitionSequenceLength();
            pasl.setParameterName(cltuPara);
            pasl.setParameterValue(new IntUnsignedShort(acquisitionSequenceLength));
            positiveResult.setParAcquisitionSequenceLength(pasl);
            break;
        case bitLockRequired:
            ParBitLockRequired pblr = new ParBitLockRequired();
            pblr.setParameterName(cltuPara);
            pblr.setParameterValue(new BerInteger(bitLockRequired));
            positiveResult.setParBitLockRequired(pblr);
            break;
        case clcwGlobalVcId:
            ParClcwGlobalVcId parClcwGlobalVcId = new ParClcwGlobalVcId();
            parClcwGlobalVcId.setParameterName(cltuPara);
            parClcwGlobalVcId.setParameterValue(clcwGvCid);
            positiveResult.setParClcwGlobalVcId(parClcwGlobalVcId);
            break;
        case clcwPhysicalChannel:
            ParClcwPhysicalChannel parClcwPhysicalChannel = new ParClcwPhysicalChannel();
            parClcwPhysicalChannel.setParameterName(cltuPara);
            parClcwPhysicalChannel.setParameterValue(clcwPhysicalChannel);
            positiveResult.setParClcwPhysicalChannel(parClcwPhysicalChannel);
            break;
        case deliveryMode:
            ParDeliveryMode parDeliveryMode = new ParDeliveryMode();
            parDeliveryMode.setParameterName(cltuPara);
            parDeliveryMode.setParameterValue(new CltuDeliveryMode(DeliveryMode.fwdOnline.id()));
            positiveResult.setParDeliveryMode(parDeliveryMode);
            break;
        case expectedSlduIdentification:
            ParCltuIdentification parCltuIdentification = new ParCltuIdentification();
            parCltuIdentification.setParameterName(cltuPara);
            parCltuIdentification.setParameterValue(new CltuIdentification(expectedCltuId));
            positiveResult.setParCltuIdentification(parCltuIdentification);
            break;
        case expectedEventInvocationIdentification:
            ParEventInvocationIdentification parEventInvocationIdentification = new ParEventInvocationIdentification();
            parEventInvocationIdentification.setParameterName(cltuPara);
            parEventInvocationIdentification
                    .setParameterValue(new EventInvocationId(expectedEventInvocationId));
            positiveResult.setParEventInvocationIdentification(parEventInvocationIdentification);
            break;
        case maximumSlduLength:
            ParMaximumCltuLength parMaximumCltuLength = new ParMaximumCltuLength();
            parMaximumCltuLength.setParameterName(cltuPara);
            parMaximumCltuLength.setParameterValue(new BerInteger(maxCltuLength));
            positiveResult.setParMaximumCltuLength(parMaximumCltuLength);
            break;
        case minimumDelayTime:
            ParMinimumDelayTime parMinimumDelayTime = new ParMinimumDelayTime();
            parMinimumDelayTime.setParameterName(cltuPara);
            parMinimumDelayTime.setParameterValue(new Duration(duration));
            positiveResult.setParMinimumDelayTime(parMinimumDelayTime);
            break;
        case minReportingCycle:
            ParMinReportingCycle parMinReportingCycle = new ParMinReportingCycle();
            parMinReportingCycle.setParameterName(cltuPara);
            parMinReportingCycle.setParameterValue(new IntPosShort(minReportingCycle));
            positiveResult.setParMinReportingCycle(parMinReportingCycle);
            break;
        case modulationFrequency:
            ParModulationFrequency parModulationFrequency = new ParModulationFrequency();
            parModulationFrequency.setParameterName(cltuPara);
            parModulationFrequency.setParameterValue(new ModulationFrequency(modulationFrequency));
            positiveResult.setParModulationFrequency(parModulationFrequency);
            break;
        case modulationIndex:
            ParModulationIndex parModulationIndex = new ParModulationIndex();
            parModulationIndex.setParameterName(cltuPara);
            parModulationIndex.setParameterValue(new ModulationIndex(modulationIndex));
            positiveResult.setParModulationIndex(parModulationIndex);
            break;
        case notificationMode:
            ParNotificationMode parNotificationMode = new ParNotificationMode();
            parNotificationMode.setParameterName(cltuPara);
            parNotificationMode.setParameterValue(new BerInteger(notificationMode.id()));
            positiveResult.setParNotificationMode(parNotificationMode);
            break;
        case plop1IdleSequenceLength:
            ParPlop1IdleSequenceLength parPlop1IdleSequenceLength = new ParPlop1IdleSequenceLength();
            parPlop1IdleSequenceLength.setParameterName(cltuPara);
            parPlop1IdleSequenceLength.setParameterValue(new IntUnsignedShort(plop1IdleSequenceLength));
            positiveResult.setParPlop1IdleSequenceLength(parPlop1IdleSequenceLength);
            break;
        case plopInEffect:
            ParPlopInEffect parPlopInEffect = new ParPlopInEffect();
            parPlopInEffect.setParameterName(cltuPara);
            parPlopInEffect.setParameterValue(new BerInteger(plopInEffect));
            positiveResult.setParPlopInEffect(parPlopInEffect);
            break;
        case protocolAbortMode:
            ParProtocolAbortMode parProtocolAbortMode = new ParProtocolAbortMode();
            parProtocolAbortMode.setParameterName(cltuPara);
            parProtocolAbortMode.setParameterValue(new BerInteger(protocolAbortMode));
            positiveResult.setParProtocolAbortMode(parProtocolAbortMode);
            break;
        case reportingCycle:
            ParReportingCycle parReportingCycle = new ParReportingCycle();
            parReportingCycle.setParameterName(cltuPara);
            parReportingCycle.setParameterValue(reportingCycle);
            positiveResult.setParReportingCycle(parReportingCycle);
            break;
        case returnTimeoutPeriod:
            ParReturnTimeout parReturnTimeout = new ParReturnTimeout();
            parReturnTimeout.setParameterName(cltuPara);
            parReturnTimeout.setParameterValue(new TimeoutPeriod(returnTimeoutPeriod));
            positiveResult.setParReturnTimeout(parReturnTimeout);
            break;
        case rfAvailableRequired:
            ParRfAvailableRequired parRfAvailableRequired = new ParRfAvailableRequired();
            parRfAvailableRequired.setParameterName(cltuPara);
            parRfAvailableRequired.setParameterValue(new BerInteger(rfAvailableRequired));
            positiveResult.setParRfAvailableRequired(parRfAvailableRequired);
            break;
        case subcarrierToBitRateRatio:
            ParSubcarrierToBitRateRatio parSubcarrierToBitRateRatio = new ParSubcarrierToBitRateRatio();
            parSubcarrierToBitRateRatio.setParameterName(cltuPara);
            parSubcarrierToBitRateRatio.setParameterValue(new SubcarrierDivisor(subcarrierToBitRateRatio));
            positiveResult.setParSubcarrierToBitRateRatio(parSubcarrierToBitRateRatio);
            break;
        default:
            negativeResult = new DiagnosticCltuGetParameter();
            negativeResult.setSpecific(new BerInteger(0)); // unknown parameter
        }
        if (negativeResult != null) {
            res.setNegativeResult(negativeResult);
        } else {
            res.setPositiveResult(positiveResult);
        }
        
        return res;
    }

    public void setExpectedCltuId(int expectedCltuId) {
        this.expectedCltuId = expectedCltuId;
    }

    public void setExpectedEventInvocationId(int expectedEventInvocationId) {
        this.expectedEventInvocationId = expectedEventInvocationId;
    }
}
