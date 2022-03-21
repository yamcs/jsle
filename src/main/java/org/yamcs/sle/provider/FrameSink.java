package org.yamcs.sle.provider;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.ForwardDuStatus;

/**
 * Responsible with sending frames out
 */
public interface FrameSink {

    /**
     * Called at startup
     */
    void startup();

    /**
     * Called at shutdown
     */
    void shutdown();

    /**
     * Called at SLE START invocation
     */
    int start(CltuServiceProvider csp);

    /**
     * Called at SLE STOP invocation
     */
    int stop(CltuServiceProvider csp);

    /**
     * uplinks the CLTU. The method should block for the duration of the uplink
     * @param cltuData
     * @return
     */
    UplinkResult uplink(byte[] cltuData);
    
    public static class UplinkResult {
        public CcsdsTime startTime;
        public CcsdsTime stopTime;
        public ForwardDuStatus cltuStatus;
    }
}
