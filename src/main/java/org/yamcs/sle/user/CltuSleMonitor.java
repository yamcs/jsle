package org.yamcs.sle.user;

import org.yamcs.sle.SleMonitor;

import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuTransferData;

/**
 * Receives information related to the SLE link, including status reports (if scheduled)
 * 
 * @author nm
 *
 */
public interface CltuSleMonitor extends SleMonitor {
    public void onCltuStatusReport(CltuStatusReportInvocation cltuStatusReportInvocation);

    public void onAsyncNotify(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation);

    /**
     * Called when the provider has confirmed that the CLTU has been received. This means that the CLTU is in a queue in
     * the Ground Station, not that it has been radiated.
     * 
     * @param cltuId
     */
    public void onPositiveTransfer(int cltuId);

    /**
     * Called when the provider has sent an error back that the CLTU could not be transferred.
     * @param cltuId
     * @param negativeResult
     */
    public void onNegativeTransfer(int cltuId, DiagnosticCltuTransferData negativeResult);

}
