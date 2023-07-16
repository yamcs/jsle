package org.yamcs.jsle;

/**
 * Defines an interface that time providers must implement.
 */
public interface TimeProvider {

    /**
     * Gets the current system time, real or simulated, as a UNIX
     * time in milliseconds.
     *
     * @return the system time, as a UNIX time in milliseconds
     */
    long getSystemTime();

}
