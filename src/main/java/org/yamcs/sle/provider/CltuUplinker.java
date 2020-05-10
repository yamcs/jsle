package org.yamcs.sle.provider;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.ForwardDuStatus;

public interface CltuUplinker {
    
    /**
     * Called at SLE start to start
     * 
     * @return -1 if the result is successful. return greater or equal with 0 means error and the code will be inserted
     *         into the specific part of the SLE start return message
     */
    int start();
    
    /**
     * Called at SLE stop
     */
    void stop();
    
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
