package org.yamcs.sle.user;

/**
 * Common attributes required for specifying the SLE services and doing a connection
 * 
 * @author nm
 *
 */
public class SleAttributes {
    public final String responderPortId;
    public final String initiatorId;
    public final String serviceInstance;
   
    
    public SleAttributes(String responderPortId, String initiatorId, String serviceInstance) {
        this.responderPortId = responderPortId;
        this.initiatorId = initiatorId;
        this.serviceInstance = serviceInstance;
    }
}
