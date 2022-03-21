package org.yamcs.sle.udpslebridge;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Calendar;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TimeZone;

/**
 * 
 * Iterates over frame files recorded by {@link FrameRecorder}
 */
public class FrameFileIterator implements Iterator<TimestampedFrameData> {

    FileInputStream currentFile = null;
    final Calendar cal;
    final Path rootDir;
    final long start;
    final long stop;
    TimestampedFrameData currentFrame;

    public FrameFileIterator(String rootDir, long start, long stop) {
        cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(start);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        this.rootDir = Paths.get(rootDir);
        this.start = start;
        this.stop = stop;
    }

    @Override
    public boolean hasNext() {
        while (true) {
            if (currentFile == null) {
                searchNextFile();
                if (currentFile == null) {
                    return false;
                }
            }
            currentFrame = readFrame();
            if (currentFrame == null) {// end of file
                closeCurrentFile();
                continue;
            }

            if (currentFrame.timeMillis < start) {
                continue;
            } else if (currentFrame.timeMillis >= stop) {
                closeCurrentFile();
                return false;
            } else {
                return true;
            }
        }
    }

    private void closeCurrentFile() {
        try {
            if (currentFile != null) {
                currentFile.close();
                currentFile = null;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    TimestampedFrameData readFrame() {
        byte[] header = new byte[16];
        try {
            int n = currentFile.read(header);
            if (n == -1) {
                return null;
            } else if (n != header.length) {
                throw new UncheckedIOException(new IOException("Invalid file: short header read"));
            }

            ByteBuffer bb = ByteBuffer.wrap(header);
            int length = bb.getInt();
            long timeMillis = bb.getLong();
            int timePicos = bb.getInt();
            byte[] data = new byte[length - 12];
            n = currentFile.read(data);
            if (n != length - 12) {
                throw new UncheckedIOException(new IOException("Invalid file: short data read"));
            }
            return new TimestampedFrameData(timeMillis, timePicos, data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public TimestampedFrameData next() {
        if (currentFrame == null) {
            throw new NoSuchElementException();
        }
        return currentFrame;
    }

    private void searchNextFile() {
        try {
            closeCurrentFile();

            Path p = null;
            while (p == null) {
                Path dir = searchNextDay();
                if (dir == null) {
                    return;
                }
                if (cal.getTimeInMillis() >= stop) {
                    return;
                }
                p = searchNextHour(dir);
            }
            currentFile = new FileInputStream(p.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // searches in the current day a file.
    // if not found the calendar will be advanced to the next day (unless it is past the stop time)
    private Path searchNextHour(Path dir) {
        Path file = null;
        int h = cal.get(Calendar.HOUR);
        while (h < 24) {
            file = dir.resolve(String.format("%02d", h));
            if (Files.exists(file)) {
                cal.add(Calendar.HOUR, 1);
                return file;
            } else {
                file = null;
                h++;
                cal.set(Calendar.HOUR, h);
                if (cal.getTimeInMillis() >= stop) {
                    break;
                }
            }
        }
        return null;
    }

    Path searchNextDay() {
        while (true) {
            Path dir = rootDir.resolve(getDir(cal));

            if (Files.exists(dir)) {
                return dir;
            } else {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR, 0);
                if (cal.getTimeInMillis() >= stop) {
                    return null;
                }
            }
        }
    }

    String getDir(Calendar cal) {
        return String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

}
