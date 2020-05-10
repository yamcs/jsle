package org.yamcs.sle.provider;

import org.yamcs.sle.Constants.ApplicationIdentifier;


public interface ServiceInitializer {
    ServiceInitResult getServiceInstance(String initiatorId, String responderPortId, ApplicationIdentifier appId,
            String serviceInstanceIdentifier);

    public static class ServiceInitResult {
        public boolean success;
        
        //the name is used for logging
        public String name;
        //if success = false, gives the diagnostic to send in the bind return
        public int diagnostic;
        public SleService sleService;
    }

}
