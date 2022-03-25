package org.yamcs.sle.provider;

import org.yamcs.sle.Constants.DeliveryMode;
import org.yamcs.sle.Constants.FrameQuality;

import java.util.ArrayList;
import java.util.List;

import org.yamcs.sle.ParameterName;

import com.beanit.jasn1.ber.types.BerInteger;

import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParBufferSize;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParLatencyLimit;
import ccsds.sle.transfer.service.rcf.incoming.pdus.RcfGetParameterInvocation;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfGetParameterReturn;
import ccsds.sle.transfer.service.rcf.structures.CurrentReportingCycle;
import ccsds.sle.transfer.service.rcf.structures.DiagnosticRcfGet;
import ccsds.sle.transfer.service.rcf.structures.GvcIdSet;
import ccsds.sle.transfer.service.rcf.structures.RcfDeliveryMode;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParDeliveryMode;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParMinReportingCycle;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParPermittedGvcidSet;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParReportingCycle;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParReqGvcId;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter.ParReturnTimeout;
import ccsds.sle.transfer.service.rcf.structures.RcfParameterName;
import ccsds.sle.transfer.service.rcf.structures.RequestedGvcId;
import ccsds.sle.transfer.service.rcf.structures.TimeoutPeriod;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfGetParameterReturn.Result;

public class RcfParameters {
    DeliveryMode deliveryMode;
    int minReportingCycle;
    CurrentReportingCycle reportingCycle;
    int returnTimeoutPeriod;
    FrameQuality frameQuality;
    List<FrameQuality> permittedFrameQuality;
    ParLatencyLimit.ParameterValue latencyLimit;
    RequestedGvcId requestedGvcId;
    GvcIdSet gvcIdSet;
    int bufferSize;

    public RcfParameters() {
        permittedFrameQuality = new ArrayList<>();
        latencyLimit = new ParLatencyLimit.ParameterValue();
        requestedGvcId = new RequestedGvcId();
        gvcIdSet = new GvcIdSet();
    }

    public RcfGetParameterReturn processGetInvocation(RcfGetParameterInvocation cltuGetParameterInvocation) {
        RcfGetParameterReturn r = new RcfGetParameterReturn();
        r.setInvokeId(cltuGetParameterInvocation.getInvokeId());
        
        RcfParameterName pn = cltuGetParameterInvocation.getRcfParameter();
        ParameterName pname = ParameterName.byId(cltuGetParameterInvocation.getRcfParameter().intValue());
        Result res = new Result();
        RcfGetParameter positiveResult = new RcfGetParameter();
        DiagnosticRcfGet negativeResult = null;

        switch (pname) {
        case deliveryMode:
            ParDeliveryMode parDeliveryMode = new ParDeliveryMode();
            parDeliveryMode.setParameterName(pn);
            parDeliveryMode.setParameterValue(new RcfDeliveryMode(deliveryMode.id()));
            positiveResult.setParDeliveryMode(parDeliveryMode);
            break;
        case latencyLimit:
            ParLatencyLimit parLatencyLimit = new ParLatencyLimit();
            parLatencyLimit.setParameterName(pn);
            parLatencyLimit.setParameterValue(latencyLimit);
            positiveResult.setParLatencyLimit(parLatencyLimit);
            break;
        case minReportingCycle:
            ParMinReportingCycle parMinReportingCycle = new ParMinReportingCycle();
            parMinReportingCycle.setParameterName(pn);
            parMinReportingCycle.setParameterValue(new IntPosShort(minReportingCycle));
            positiveResult.setParMinReportingCycle(parMinReportingCycle);
            break;
        case permittedGvcidSet:
            ParPermittedGvcidSet parPermittedGvcidSet = new ParPermittedGvcidSet();
            parPermittedGvcidSet.setParameterName(pn);
            parPermittedGvcidSet.setParameterValue(gvcIdSet);
            positiveResult.setParPermittedGvcidSet(parPermittedGvcidSet);
            break;
        case reportingCycle:
            ParReportingCycle parReportingCycle = new ParReportingCycle();
            parReportingCycle.setParameterName(pn);
            parReportingCycle.setParameterValue(reportingCycle);
            positiveResult.setParReportingCycle(parReportingCycle);
            break;
        case requestedGvcid:
            ParReqGvcId parReqGvcId = new ParReqGvcId();
            parReqGvcId.setParameterName(pn);
            parReqGvcId.setParameterValue(requestedGvcId);
            positiveResult.setParReqGvcId(parReqGvcId);
            break;
        case returnTimeoutPeriod:
            ParReturnTimeout parReturnTimeout = new ParReturnTimeout();
            parReturnTimeout.setParameterName(pn);
            parReturnTimeout.setParameterValue(new TimeoutPeriod(returnTimeoutPeriod));
            positiveResult.setParReturnTimeout(parReturnTimeout);
            break;
        case bufferSize:
            ParBufferSize parBufferSize = new ParBufferSize();
            parBufferSize.setParameterName(pn);
            parBufferSize.setParameterValue(new IntPosShort(bufferSize));
            positiveResult.setParBufferSize(parBufferSize);
            break;
        default:
            negativeResult = new DiagnosticRcfGet();
            negativeResult.setSpecific(new BerInteger(0)); // unknown parameter
        }
        if (negativeResult != null) {
            res.setNegativeResult(negativeResult);
        } else {
            res.setPositiveResult(positiveResult);
        }
        
        r.setResult(res);
        return r;
    }
}
