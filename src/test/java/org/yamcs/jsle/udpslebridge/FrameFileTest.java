package org.yamcs.jsle.udpslebridge;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Random;

import org.junit.Test;
import org.yamcs.jsle.udpslebridge.FrameFileIterator;
import org.yamcs.jsle.udpslebridge.FrameRecorder;
import org.yamcs.jsle.udpslebridge.TimestampedFrameData;

public class FrameFileTest {
    static Random rand = new Random();

    @Test
    public void test1() throws Exception {
        Path tmp = Files.createTempDirectory("fr");
        FrameRecorder fr = new FrameRecorder(tmp.toString());
        byte[] data0 = getRandomData(100);
        Instant t0 = Instant.parse("2022-03-20T01:23:01Z");
        fr.recordFrame(t0, data0);

        byte[] data1 = getRandomData(100);
        Instant t1 = Instant.parse("2022-03-20T01:23:01Z");
        fr.recordFrame(t1, data1);

        byte[] data2 = getRandomData(123);
        Instant t2 = Instant.parse("2022-04-20T02:23:01Z");
        fr.recordFrame(t2, data2);
        fr.shutdown();

        Path p = tmp.resolve("2022-03-20").resolve("01");
        assertTrue(Files.exists(p));
        assertEquals(2 * 116, Files.size(p));

        byte[] buf = new byte[2 * 116];

        try (FileInputStream is = new FileInputStream(p.toFile())) {
            is.read(buf);
        }
        ByteBuffer bb = ByteBuffer.wrap(buf);
        assertEquals(112, bb.getInt());// length
        assertEquals(t0.toEpochMilli(), bb.getLong()); // time millis
        assertEquals(0, bb.getInt()); // time picos
        byte[] datar = new byte[data0.length];
        bb.get(datar); // data
        assertArrayEquals(data0, datar);

        assertEquals(112, bb.getInt());// length
        assertEquals(t1.toEpochMilli(), bb.getLong()); // time millis
        assertEquals(0, bb.getInt()); // time picos

        Path p1 = tmp.resolve("2022-04-20").resolve("02");
        assertTrue(Files.exists(p1));
        assertEquals(139, Files.size(p1));

        FrameFileIterator it0 = new FrameFileIterator(tmp.toString(), t0.toEpochMilli() - 3602_000,
                t0.toEpochMilli() - 3600_000);
        assertFalse(it0.hasNext());

        FrameFileIterator it1 = new FrameFileIterator(tmp.toString(), t2.toEpochMilli() + 3600_000,
                t2.toEpochMilli() + 3602_000);
        assertFalse(it1.hasNext());

        FrameFileIterator it2 = new FrameFileIterator(tmp.toString(), t2.toEpochMilli() + 1, t2.toEpochMilli() + 2);
        assertFalse(it2.hasNext());

        FrameFileIterator it3 = new FrameFileIterator(tmp.toString(), t2.toEpochMilli(), t2.toEpochMilli() + 2);
        assertTrue(it3.hasNext());
        TimestampedFrameData tfd = it3.next();
        assertEquals(t2.toEpochMilli(), tfd.timeMillis);
        assertArrayEquals(data2, tfd.data);
        assertFalse(it3.hasNext());

        FrameFileIterator it4 = new FrameFileIterator(tmp.toString(), t0.toEpochMilli(), t2.toEpochMilli() + 2);
        assertTrue(it4.hasNext());
        TimestampedFrameData tfd0 = it4.next();
        assertEquals(t0.toEpochMilli(), tfd0.timeMillis);
        assertArrayEquals(data0, tfd0.data);
        assertTrue(it4.hasNext());

        TimestampedFrameData tfd1 = it4.next();
        assertEquals(t1.toEpochMilli(), tfd1.timeMillis);
        assertArrayEquals(data1, tfd1.data);
        assertTrue(it4.hasNext());

        TimestampedFrameData tfd2 = it4.next();
        assertEquals(t2.toEpochMilli(), tfd2.timeMillis);
        assertArrayEquals(data2, tfd2.data);
        assertFalse(it4.hasNext());

        rmDir(tmp);
    }

    private byte[] getRandomData(int length) {
        byte[] data = new byte[length];
        rand.nextBytes(data);
        return data;
    }

    void rmDir(Path p) throws IOException {
        Files.walk(p).sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

    }
}
