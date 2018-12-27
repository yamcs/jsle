package org.yamcs.sle;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Date;

import org.junit.Test;
import org.yamcs.sle.CcsdsTime;

import io.netty.buffer.ByteBufUtil;

public class CcsdsTimeTest {
    @Test
    public void test1() {
        long t = Instant.parse("1958-01-01T00:00:00Z").toEpochMilli();
        byte[] b = CcsdsTime.fromJavaMillisec(t).getDaySegmented();
        assertEquals("0000000000000000", ByteBufUtil.hexDump(b));
        
        assertEquals(t, CcsdsTime.fromJavaMillisec(t).toJavaMillisec());
    }
    
    @Test
    public void test2() {
        String hex = "5764045eb6d00061";
        
        CcsdsTime t = CcsdsTime.fromCcsds(ByteBufUtil.decodeHexDump(hex));
        assertEquals(hex, ByteBufUtil.hexDump(t.getDaySegmented()));
    }
}
