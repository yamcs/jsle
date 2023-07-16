package org.yamcs.jsle;

/**
 * Implements a time provider that uses the time from the operating system.
 */
public class DefaultTimeProvider implements TimeProvider {

    @Override
    public long getSystemTime() {
        return System.currentTimeMillis();
    }

}
