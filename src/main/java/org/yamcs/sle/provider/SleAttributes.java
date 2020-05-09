package org.yamcs.sle.provider;

/**
 * Common attributes required for specifying the SLE services and doing a connection
 * 
 * @author nm
 *
 */
public class SleAttributes {
    public final String responderPortId;
    public final String responderId;
    public final String serviceInstance;
   
    
    public SleAttributes(String responderPortId, String responderId, String serviceInstance) {
        this.responderPortId = responderPortId;
        this.responderId = responderId;
        this.serviceInstance = serviceInstance;
    }
}
