package org.yamcs.sle;

import java.util.stream.Collectors;

import org.yamcs.sle.Constants.DeliveryMode;
import org.yamcs.sle.Constants.FrameQuality;

import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter;

/**
 * RAF and RCF parameter
 * 
 * @author nm
 *
 */
public class SleParameter {
    private ParameterName parameterName;
    private Object parameterValue;

    public SleParameter(ParameterName parameterName, Object parameterValue) {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public SleParameter(RafGetParameter rgp) {
        ParameterName pn;
        Object pv;
        if (rgp.getParBufferSize() != null) {
            pn = ParameterName.bufferSize;
            pv = rgp.getParBufferSize().getParameterValue().longValue();
        } else if (rgp.getParDeliveryMode() != null) {
            pn = ParameterName.deliveryMode;
            pv = DeliveryMode.byId(rgp.getParDeliveryMode().getParameterValue().intValue());
        } else if (rgp.getParLatencyLimit() != null) {
            pn = ParameterName.latencyLimit;
            ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParLatencyLimit.ParameterValue ppllpv = rgp
                    .getParLatencyLimit().getParameterValue();
            pv = ppllpv.getOnline() == null ? null : ppllpv.getOnline().longValue();
        } else if (rgp.getParMinReportingCycle() != null) {
            pn = ParameterName.minReportingCycle;
            pv = rgp.getParMinReportingCycle().getParameterValue().longValue();
        } else if (rgp.getParPermittedFrameQuality() != null) {
            pn = ParameterName.permittedFrameQuality;
            pv = rgp.getParPermittedFrameQuality().getParameterValue().getRequestedFrameQuality().stream()
                    .map(slefq -> FrameQuality.byId(slefq.intValue())).collect(Collectors.toList());
        } else if (rgp.getParReportingCycle() != null) {
            pn = ParameterName.reportingCycle;
            ccsds.sle.transfer.service.raf.structures.CurrentReportingCycle c = rgp.getParReportingCycle().getParameterValue();
            pv = c.getPeriodicReportingOn() == null ? null : c.getPeriodicReportingOn().longValue();
        } else if (rgp.getParReqFrameQuality() != null) {
            pn = ParameterName.requestedFrameQuality;
            pv = FrameQuality.byId(rgp.getParReqFrameQuality().getParameterValue().intValue());
        } else if (rgp.getParReturnTimeout() != null) {
            pn = ParameterName.returnTimeoutPeriod;
            pv = rgp.getParReturnTimeout().getParameterValue().longValue();
        } else {
            throw new SleException("Unknown RafParameterGet "+rgp);
        }
        this.parameterName = pn;
        this.parameterValue = pv;
    }

    
    public SleParameter(RcfGetParameter rgp) {
        ParameterName pn;
        Object pv;
        if (rgp.getParBufferSize() != null) {
            pn = ParameterName.bufferSize;
            pv = rgp.getParBufferSize().getParameterValue().longValue();
        } else if (rgp.getParDeliveryMode() != null) {
            pn = ParameterName.deliveryMode;
            pv = DeliveryMode.byId(rgp.getParDeliveryMode().getParameterValue().intValue());
        } else if (rgp.getParLatencyLimit() != null) {
            pn = ParameterName.latencyLimit;
            ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParLatencyLimit.ParameterValue ppllpv = rgp
                    .getParLatencyLimit().getParameterValue();
            pv = ppllpv.getOnline() == null ? null : ppllpv.getOnline().longValue();
        } else if (rgp.getParMinReportingCycle() != null) {
            pn = ParameterName.minReportingCycle;
            pv = rgp.getParMinReportingCycle().getParameterValue().longValue();
        } else if (rgp.getParPermittedGvcidSet() != null) {
            pn = ParameterName.permittedGvcidSet;
            pv = rgp.getParPermittedGvcidSet().getParameterValue();
        }  else if (rgp.getParReportingCycle() != null) {
            pn = ParameterName.reportingCycle;
            ccsds.sle.transfer.service.rcf.structures.CurrentReportingCycle c = rgp.getParReportingCycle().getParameterValue();
            pv = c.getPeriodicReportingOn() == null ? null : c.getPeriodicReportingOn().longValue();
        } else if (rgp.getParReqGvcId() != null) {
            pn = ParameterName.requestedGvcid;
            pv = rgp.getParReqGvcId().getParameterValue();
        } else if (rgp.getParReturnTimeout() != null) {
            pn = ParameterName.returnTimeoutPeriod;
            pv = rgp.getParReturnTimeout().getParameterValue().longValue();
        } else {
            throw new SleException("Unknown RcfParameterGet "+rgp);
        }
        this.parameterName = pn;
        this.parameterValue = pv;
    }

    public SleParameter(CltuGetParameter cgp) {
        ParameterName pn;
        Object pv;
        if (cgp.getParAcquisitionSequenceLength() != null) {
            pn = ParameterName.acquisitionSequenceLength;
            pv = cgp.getParAcquisitionSequenceLength().getParameterValue().intValue();
        } else if(cgp.getParBitLockRequired() !=null ) {
            pn = ParameterName.bitLockRequired;
            int x = cgp.getParBitLockRequired().getParameterValue().intValue();
            pv = x == 0 ? "yes" : "no";
        } else if(cgp.getParClcwGlobalVcId() !=null ) {
            pn = ParameterName.clcwGlobalVcId;
            pv = cgp.getParClcwGlobalVcId().getParameterValue();
        } else if(cgp.getParClcwPhysicalChannel() !=null ) {
            pn = ParameterName.clcwPhysicalChannel;
            pv = cgp.getParClcwPhysicalChannel().getParameterValue();
        } else if(cgp.getParCltuIdentification() !=null ) {
            pn = ParameterName.expectedSlduIdentification;
            pv = cgp.getParCltuIdentification().getParameterValue().intValue();
        } else if(cgp.getParDeliveryMode() !=null ) {
            pn = ParameterName.deliveryMode;
            pv = DeliveryMode.byId(cgp.getParDeliveryMode().getParameterValue().intValue());
        } else if(cgp.getParEventInvocationIdentification() !=null ) {
            pn = ParameterName.expectedEventInvocationIdentification;
            pv = cgp.getParEventInvocationIdentification().getParameterValue().intValue();
        } else if(cgp.getParMaximumCltuLength() !=null ) {
            pn = ParameterName.maximumSlduLength;
            pv = cgp.getParMaximumCltuLength().getParameterValue().intValue();
        } else if(cgp.getParMinimumDelayTime() !=null ) {
            pn = ParameterName.minimumDelayTime;
            pv = cgp.getParMinimumDelayTime().getParameterValue().longValue();
        } else if(cgp.getParMinReportingCycle() !=null ) {
            pn = ParameterName.minReportingCycle;
            pv = cgp.getParMinReportingCycle().getParameterValue().intValue();
        } else if(cgp.getParModulationFrequency() !=null ) {
            pn = ParameterName.modulationFrequency;
            pv = cgp.getParModulationFrequency().getParameterValue();
        } else if(cgp.getParModulationIndex() !=null ) {
            pn = ParameterName.modulationIndex;
            pv = cgp.getParModulationIndex().getParameterValue().intValue();
        } else if(cgp.getParNotificationMode() !=null ) {
            pn = ParameterName.notificationMode;
            pv = cgp.getParNotificationMode().getParameterValue().intValue();
        } else if(cgp.getParPlop1IdleSequenceLength() !=null ) {
            pn = ParameterName.plop1IdleSequenceLength;
            pv = cgp.getParPlop1IdleSequenceLength().getParameterValue().intValue();
        } else if(cgp.getParPlopInEffect() !=null ) {
            pn = ParameterName.plopInEffect;
            int x = cgp.getParPlopInEffect().getParameterValue().intValue();
            pv = x == 0 ? "PLOP-1" : "PLOP-2";
        } else if(cgp.getParProtocolAbortMode() !=null ) {
            pn = ParameterName.protocolAbortMode;
            pv = cgp.getParProtocolAbortMode().getParameterValue().intValue();
        } else if(cgp.getParReportingCycle() !=null ) {
            pn = ParameterName.reportingCycle;
            ccsds.sle.transfer.service.cltu.structures.CurrentReportingCycle c = cgp.getParReportingCycle().getParameterValue();
            pv = c.getPeriodicReportingOn() == null ? null : c.getPeriodicReportingOn().intValue();
        } else if(cgp.getParReturnTimeout() !=null ) {
            pn = ParameterName.returnTimeoutPeriod;
            pv = cgp.getParReturnTimeout().getParameterValue();
        } else if(cgp.getParRfAvailableRequired() !=null ) {
            pn = ParameterName.rfAvailableRequired;
            pv = cgp.getParRfAvailableRequired().getParameterValue().intValue();
        } else if(cgp.getParSubcarrierToBitRateRatio() !=null ) {
            pn = ParameterName.subcarrierToBitRateRatio;
            pv = cgp.getParSubcarrierToBitRateRatio().getParameterValue().intValue();
        } else {
            throw new SleException("Unknown CltuParameterGet "+cgp);
        }
        this.parameterName = pn;
        this.parameterValue = pv;
    }

    public ParameterName getParameterName() {
        return parameterName;
    }

    public void setParameterName(ParameterName parameterName) {
        this.parameterName = parameterName;
    }

    public Object getParameterValue() {
        return parameterValue;
    }

    public void setParameterValue(Object parameterValue) {
        this.parameterValue = parameterValue;
    }
}
