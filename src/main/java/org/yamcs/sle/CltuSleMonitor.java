package org.yamcs.sle;

import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;

/**
 * Receives information related to the SLE link, including status reports (if scheduled)
 * 
 * @author nm
 *
 */
public interface CltuSleMonitor extends SleMonitor {
    public void onCltuStatusReport(CltuStatusReportInvocation cltuStatusReportInvocation);

    public void onAsyncNotify(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation);
}
