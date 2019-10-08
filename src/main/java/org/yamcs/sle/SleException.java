package org.yamcs.sle;

import com.beanit.jasn1.ber.types.BerType;

/**
 * Generic exception thrown from the jsle classes.
 * 
 * They can carry a asn.1 message as received from the peer.
 * 
 * @author nm
 *
 */
public class SleException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    BerType data;
    public SleException(String message, BerType data) {
        super(message);
        this.data = data;
    }
    public SleException(String message) {
        super(message);
    }
    
    public String toString() {
        return getMessage() + (data!=null?": "+data.toString():""); 
    }
}
