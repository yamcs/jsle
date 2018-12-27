package org.yamcs;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;
import org.yamcs.sle.CcsdsTime;

import io.netty.buffer.ByteBufUtil;

public class CcsdsTimeTest {
    @Test
    public void test1() {
        long t = Instant.parse("1958-01-01T00:00:00Z").toEpochMilli();
        byte[] b = CcsdsTime.getDaySegmented(t);
        assertEquals("0000000000000000", ByteBufUtil.hexDump(b));
    }
}
