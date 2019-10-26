package org.yamcs.sle;

/**
 * Common attributes required for specifying the SLE services and doing a connection
 * 
 * @author nm
 *
 */
public class SleAttributes {
    final String responderPortId;
    final String initiatorId;
    final String serviceInstance;
    
    public SleAttributes(String responderPortId, String initiatorId, String serviceInstance) {
        this.responderPortId = responderPortId;
        this.initiatorId = initiatorId;
        this.serviceInstance = serviceInstance;
    }
}
