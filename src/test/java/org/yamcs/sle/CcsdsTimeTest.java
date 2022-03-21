package org.yamcs.sle;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;
import org.yamcs.sle.CcsdsTime;

import io.netty.buffer.ByteBufUtil;

public class CcsdsTimeTest {
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
}
