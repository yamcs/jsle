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
    /** service agreement name */
    final String sagr;
    /** service package name */
    final String spack;

    /** service functional group name */
    final String sfg;

    /** service instance number */
    final int sinst;

    public SleAttributes(String responderPortId, String initiatorId, String sagr,
            String spack, String sfg, int sinst) {
        this.responderPortId = responderPortId;
        this.initiatorId = initiatorId;
        this.sagr = sagr;
        this.spack = spack;
        this.sfg = sfg;
        this.sinst = sinst;
    }
    
   
}
