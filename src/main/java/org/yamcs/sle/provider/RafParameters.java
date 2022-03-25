package org.yamcs.sle.provider;

import org.yamcs.sle.Constants.DeliveryMode;
import org.yamcs.sle.Constants.FrameQuality;

import java.util.ArrayList;
import java.util.List;

import org.yamcs.sle.ParameterName;

import com.beanit.jasn1.ber.types.BerInteger;

import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.raf.incoming.pdus.RafGetParameterInvocation;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafGetParameterReturn;
import ccsds.sle.transfer.service.raf.structures.CurrentReportingCycle;
import ccsds.sle.transfer.service.raf.structures.DiagnosticRafGet;
import ccsds.sle.transfer.service.raf.structures.PermittedFrameQualitySet;
import ccsds.sle.transfer.service.raf.structures.RafDeliveryMode;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParBufferSize;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParDeliveryMode;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParLatencyLimit;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParMinReportingCycle;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParPermittedFrameQuality;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParReportingCycle;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParReqFrameQuality;
import ccsds.sle.transfer.service.raf.structures.RafGetParameter.ParReturnTimeout;
import ccsds.sle.transfer.service.raf.structures.RafParameterName;
import ccsds.sle.transfer.service.raf.structures.RequestedFrameQuality;
import ccsds.sle.transfer.service.raf.structures.TimeoutPeriod;
import ccsds.sle.transfer.service.raf.outgoing.pdus.RafGetParameterReturn.Result;

public class RafParameters {
    DeliveryMode deliveryMode;
    int minReportingCycle;
    CurrentReportingCycle reportingCycle;
    int returnTimeoutPeriod;
    FrameQuality frameQuality;
    List<FrameQuality> permittedFrameQuality;
    ParLatencyLimit.ParameterValue latencyLimit;
    private int bufferSize;

    public RafParameters() {
        permittedFrameQuality = new ArrayList<>();
        latencyLimit = new ParLatencyLimit.ParameterValue();

    }

    public RafGetParameterReturn processGetInvocation(RafGetParameterInvocation cltuGetParameterInvocation) {
        RafGetParameterReturn r = new RafGetParameterReturn();
        r.setInvokeId(cltuGetParameterInvocation.getInvokeId());
        
        RafParameterName pn = cltuGetParameterInvocation.getRafParameter();
        ParameterName pname = ParameterName.byId(cltuGetParameterInvocation.getRafParameter().intValue());
        Result res = new Result();
        RafGetParameter positiveResult = new RafGetParameter();
        DiagnosticRafGet negativeResult = null;

        switch (pname) {
        case deliveryMode:
            ParDeliveryMode parDeliveryMode = new ParDeliveryMode();
            parDeliveryMode.setParameterName(pn);
            parDeliveryMode.setParameterValue(new RafDeliveryMode(deliveryMode.id()));
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
        case permittedFrameQuality:
            ParPermittedFrameQuality parPermittedFrameQuality = new ParPermittedFrameQuality();
            parPermittedFrameQuality.setParameterName(pn);
            PermittedFrameQualitySet pfqs = new PermittedFrameQualitySet();
            List<RequestedFrameQuality> l =pfqs.getRequestedFrameQuality();
            permittedFrameQuality.forEach(fq -> l.add(new RequestedFrameQuality(fq.id())));
            parPermittedFrameQuality.setParameterValue(pfqs);
            positiveResult.setParPermittedFrameQuality(parPermittedFrameQuality);
            break;
        case reportingCycle:
            ParReportingCycle parReportingCycle = new ParReportingCycle();
            parReportingCycle.setParameterName(pn);
            parReportingCycle.setParameterValue(reportingCycle);
            positiveResult.setParReportingCycle(parReportingCycle);
            break;
        case requestedFrameQuality:
            ParReqFrameQuality parReqFrameQuality = new ParReqFrameQuality();
            parReqFrameQuality.setParameterName(pn);
            parReqFrameQuality.setParameterValue(new BerInteger(frameQuality.id()));
            positiveResult.setParReqFrameQuality(parReqFrameQuality);
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
            negativeResult = new DiagnosticRafGet();
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
