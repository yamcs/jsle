package org.yamcs.sle.provider;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.ForwardDuStatus;

public interface CltuConsumer {
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
