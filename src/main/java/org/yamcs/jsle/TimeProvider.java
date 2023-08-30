package org.yamcs.jsle;

/**
 * Defines an interface that time providers must implement.
 */
public interface TimeProvider {

    /**
     * Gets the current system time as a <Code>CcsdsTime</code>.
     *
     * @return the system time, as a CCSDS time
     */
    CcsdsTime getSystemTime();

}
