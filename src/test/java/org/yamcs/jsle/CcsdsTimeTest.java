package org.yamcs.jsle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.ByteBufUtil;

public class CcsdsTimeTest {

    private TimeProvider savedProvider;

    /**
     * Saves the current time provider prior to each test.
     */
    @Before
    public void setup() {
        savedProvider = CcsdsTime.getTimeProvider();
    }

    /**
     * Restores the time provider to the default after testing.
     */
    @After
    public void cleanup() {
        CcsdsTime.setTimeProvider(savedProvider);
    }

    @Test
    public void test1() {
        long t = Instant.parse("1958-01-01T00:00:00Z").toEpochMilli();
        byte[] b = CcsdsTime.fromJavaMillis(t).getDaySegmented();
        assertEquals("0000000000000000", ByteBufUtil.hexDump(b));
        
        assertEquals(t, CcsdsTime.fromJavaMillis(t).toJavaMillisec());
    }
    
    @Test
    public void test2() {
        String hex = "5764045eb6d00061";
        
        CcsdsTime t = CcsdsTime.fromCcsds(ByteBufUtil.decodeHexDump(hex));
        assertEquals(hex, ByteBufUtil.hexDump(t.getDaySegmented()));
    }
    
    @Test
    public void test3() {
        CcsdsTime t = CcsdsTime.fromUnix(3601, 3_000_000);
        assertEquals("1970-01-01T01:00:01.003Z", t.toString());
    }
    
    @Test
    public void test4() {
        CcsdsTime t = CcsdsTime.fromUnix(1588711800, 1_000_000);
        assertEquals("2020-05-05T20:50:00.001Z", t.toString());
    }
    
    @Test
    public void test5() {
        CcsdsTime t = new CcsdsTime(1, 301123456789012L);
        assertEquals("1958-01-02T00:05:01.123456789012Z", t.toStringPico());
    }

    /**
     * Tests that the default time provider gives the current system time.
     */
    @Test
    public void testDefaultProvider() {
        CcsdsTime.setTimeProvider(new DefaultTimeProvider());
        CcsdsTime time1 = CcsdsTime.fromJavaMillis(System.currentTimeMillis());
        CcsdsTime now = CcsdsTime.now();
        CcsdsTime time2 = CcsdsTime.fromJavaMillis(System.currentTimeMillis());
        assertTrue(time1.compareTo(now) <= 0);
        assertTrue(now.compareTo(time2) <= 0);
    }

    /**
     * Tests that the time provider can be overridden by one providing
     * a simulated time.
     */
    @Test
    public void testSimulatedTimeProvider() {
        MockTimeProvider mockProvider = new MockTimeProvider();
        CcsdsTime.setTimeProvider(mockProvider);
        CcsdsTime fixedTime = CcsdsTime.fromJavaMillis(12345);
        mockProvider.setSystemTime(12345);
        CcsdsTime now = CcsdsTime.now();
        assertEquals(0, fixedTime.compareTo(now));
    }

    /**
     * Implements a time provider that returns a time set by
     * a method call.
     */
    private static class MockTimeProvider implements TimeProvider {

        private long time;

        @Override
        public long getSystemTime() {
            return time;
        }

        /**
         * Sets the time that should be returned.
         *
         * @param time a time in UNIX milliseconds
         */
        public void setSystemTime(long time) {
            this.time = time;
        }

    }
}
