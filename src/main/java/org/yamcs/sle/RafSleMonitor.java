package org.yamcs.sle;

import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStatusReportInvocation;

public interface RafSleMonitor extends SleMonitor {
    public void onRafStatusReport(RafStatusReportInvocation rafStatusReportInvocation);
}
