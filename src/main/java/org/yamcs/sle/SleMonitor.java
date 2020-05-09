package org.yamcs.sle;

/**
 * Receives notifications related to the SLE link
 * @author nm
 *
 */
public interface SleMonitor {
    /**
     * called when the connection is established
     */
    public void connected();
    /**
     * called when the connection is lost
     */
    public void disconnected();
    
    /**
     * Called when the state changes
     * @param newState
     */
    public void stateChanged(State newState);
    
    /**
     * Called when an exception is encountered
     * @param t
     */
    public void exceptionCaught(Throwable t);
}
