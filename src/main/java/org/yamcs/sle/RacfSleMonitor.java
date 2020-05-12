package org.yamcs.sle;

import org.yamcs.sle.user.RacfStatusReport;


public interface RacfSleMonitor extends SleMonitor {
    public void onStatusReport(RacfStatusReport StatusReportInvocation);
}
