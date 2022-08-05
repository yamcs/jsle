package org.yamcs.jsle;

import org.yamcs.jsle.user.RacfStatusReport;


public interface RacfSleMonitor extends SleMonitor {
    public void onStatusReport(RacfStatusReport StatusReportInvocation);
}
