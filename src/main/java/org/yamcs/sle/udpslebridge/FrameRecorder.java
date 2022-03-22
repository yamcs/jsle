package org.yamcs.sle.udpslebridge;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Records frames in hourly files
 * <p>
 * rootDir/yyyy-mm-dd/hh
 * <p>
 * The data format is:
 * <ul>
 * <li>4 bytes length of time + frame data</li>
 * <li>8 bytes UNIX time milliseconds</li>
 * <li>4 bytes time picoseconds (currently set to 0)</li>
 * <li>frame data</li>
 * </ul>
 * Not thread safe!
 */
public class FrameRecorder {
    final Path rootDir;
    FileOutputStream currentFile;
    long currentFileStart = -1;
    long currentFileEnd = -1;

    public FrameRecorder(String rootDir) {
        this.rootDir = Paths.get(rootDir);
    }

    public void recordFrame(long time, byte[] data) throws IOException {
        recordFrame(time, data, 0, data.length);
    }

    public void recordFrame(long time, byte[] data, int offset, int length) throws IOException {
        byte[] header = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(header);
        bb.putInt(12 + length);
        bb.putLong(time);
        bb.putInt(0);// picosecond time - we set it to 0 since java doesn't make it easy to get it
        if (time < currentFileStart || time >= currentFileEnd) {
            openNewFile(time);
        }

        currentFile.write(header);
        currentFile.write(data, offset, length);

    }

    private void openNewFile(long t) throws IOException {
        closeCurrentFile();

        Date dt = new Date(t);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(dt);
        String date = sdf.format(dt);
        Path dir = rootDir.resolve(date);
        Files.createDirectories(dir);
        Path file = dir.resolve(String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)));
        currentFile = new FileOutputStream(file.toFile());
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        currentFileStart = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        currentFileEnd = cal.getTimeInMillis();
    }

    public void shutdown() {
        closeCurrentFile();
    }

    private void closeCurrentFile() {
        if (currentFile != null) {
            try {
                currentFile.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            currentFile = null;
        }
    }
}
