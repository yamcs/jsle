package org.yamcs.sle;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Time;
import ccsds.sle.transfer.service.common.types.TimeCCSDS;
import ccsds.sle.transfer.service.common.types.TimeCCSDSpico;

/**
 * CCSDS time storing the number of days since 1958 and the number of picoseconds in the day
 * 
 */
public class CcsdsTime implements Comparable<CcsdsTime> {
    static public final int SEC_IN_DAY = 86400;
    static public final int MS_IN_DAY = SEC_IN_DAY * 1000;
    static final int NUM_DAYS_1958_1970 = 4383;

    final private int numDays;

    final private long picosecInDay;
    final static DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    final static DateTimeFormatter FORMATTER_SEC = new DateTimeFormatterBuilder().appendInstant(0).toFormatter();

    public CcsdsTime(int numDays, long picosecInDay) {
        this.numDays = numDays;
        this.picosecInDay = picosecInDay;
    }

    /**
     * P-field is implicit (not present, defaulted to 41 hex)
     * <p>
     * T-field:
     * <ul>
     * <li>2 octets: number of days since 1958/01/01 00:00:00</li>
     * <li>4 octets: number of milliseconds of the day</li>
     * <li>2 octets: number of microseconds of the millisecond
     * (set to 0 if not used)</li>
     * </ul>
     * 
     * @param ds
     * @return
     */
    public static CcsdsTime fromCcsds(byte[] ds) {
        if (ds.length != 8) {
            throw new IllegalArgumentException("Invalid number of bytes " + ds.length + "; expected 8");
        }
        int nd = ((ds[0] & 0xFF) << 8) + (ds[1] & 0xFF);
        long millis = ((ds[2] & 0xFFl) << 24) + ((ds[3] & 0xFFl) << 16) + ((ds[4] & 0xFFl) << 8) + ((ds[5] & 0xFFl));
        long micros = ((ds[6] & 0xFFl) << 8) + ((ds[7] & 0xFFl));
        return new CcsdsTime(nd, (millis * 1000 + micros) * 1000_000);

    }

    /**
     * P-field is implicit (not present, defaulted to 42 hex)
     * <p>
     * T-field:
     * <ul>
     * <li>2 octets: number of days since 1958/01/01 00:00:00</li>
     * <li>4 octets: number of milliseconds of the day</li>
     * <li>4 octets: number of picoseconds of the millisecond
     * (set to 0 if not used)</li>
     * </ul>
     * 
     * @param ds
     * @return
     */
    public static CcsdsTime fromCcsdsPico(byte[] ds) {
        if (ds.length != 10) {
            throw new IllegalArgumentException("Invalid number of bytes " + ds.length + "; expected 10");
        }
        int nd = ((ds[0] & 0xFF) << 8) + (ds[1] & 0xFF);
        long millis = ((ds[2] & 0xFFl) << 24) + ((ds[3] & 0xFFl) << 16) + ((ds[4] & 0xFFl) << 8) + ((ds[5] & 0xFFl));
        long ps = ((ds[6] & 0xFFl) << 24) + ((ds[7] & 0xFFl) << 16) + ((ds[8] & 0xFFl) << 8) + ((ds[9] & 0xFFl));

        return new CcsdsTime(nd, millis * 1000_000_000 + ps);
    }

    /**
     * Gets the current time
     * 
     * @return the current time
     */
    static public CcsdsTime now() {
        return fromJavaMillis(System.currentTimeMillis());
    }

    /**
     * Converts a java time in milliseconds
     * 
     * @param javaTime
     * @return
     */
    static public CcsdsTime fromJavaMillis(long javaTime) {
        int numDays = (int) (javaTime / MS_IN_DAY) + NUM_DAYS_1958_1970;
        int msOfDay = (int) (javaTime % MS_IN_DAY);
        return new CcsdsTime(numDays, 1000_000_000L * msOfDay);
    }

    /**
     * Converts a java time in milliseconds
     * 
     * @param javaTime
     * @return
     */
    static public CcsdsTime fromJavaMillisPicos(long javaTime, int picos) {
        int numDays = (int) (javaTime / MS_IN_DAY) + NUM_DAYS_1958_1970;
        int msOfDay = (int) (javaTime % MS_IN_DAY);
        return new CcsdsTime(numDays, 1000_000_000L * msOfDay + picos);
    }

    /**
     * Converts a UNIX time in seconds since 1970, nanoseconds in second (such as returned by gettitmeofday)
     * 
     * @param unixSeconds seconds since 1-Jan-1970 00:00:00 (without leap seconds)
     * @param nanosec nanoseconds in second
     * @return
     */
    static public CcsdsTime fromUnix(long unixSeconds, int nanosec) {
        int numDays = (int) (unixSeconds / SEC_IN_DAY) + NUM_DAYS_1958_1970;
        long picoOfDay = (unixSeconds % SEC_IN_DAY) * 1_000_000_000_000l + 1000l * nanosec;
        return new CcsdsTime(numDays, picoOfDay);
    }

    /**
     * P-field is implicit (not present, defaulted to 41 hex)
     * <p>
     * T-field:
     * <ul>
     * <li>2 octets: number of days since 1958/01/01 00:00:00</li>
     * <li>4 octets: number of milliseconds of the day</li>
     * <li>2 octets: number of microseconds of the millisecond
     * (set to 0 if not used)</li>
     * </ul>
     **/
    public byte[] getDaySegmented() {
        byte[] r = new byte[8];
        long microsec = picosecInDay / 1000_000;
        long msOfDay = microsec / 1000;
        long microsecOfSec = microsec % 1000;

        r[0] = (byte) (numDays >> 8);
        r[1] = (byte) (numDays);
        r[2] = (byte) (msOfDay >> 24);
        r[3] = (byte) (msOfDay >> 16);
        r[4] = (byte) (msOfDay >> 8);
        r[5] = (byte) (msOfDay);
        r[6] = (byte) (microsecOfSec >> 8);
        r[7] = (byte) (microsecOfSec);

        return r;
    }

    /**
     *
     * <p>
     * 
     * T-field:
     * <ul>
     * <li>2 octets: number of days since 1958/01/01 00:00:00</li>
     * <li>4 octets: number of milliseconds of the day</li>
     * <li>4 octets: number of picoseconds of the millisecond
     * (set to 0 if not used)</li>
     * </ul>
     **/
    public byte[] getDaySegmentedPico() {
        byte[] r = new byte[10];
        long msOfDay = picosecInDay / 1000_000_000;
        long picoOfMillisec = picosecInDay % 1000_000;

        r[0] = (byte) (numDays >> 8);
        r[1] = (byte) (numDays);
        r[2] = (byte) (msOfDay >> 24);
        r[3] = (byte) (msOfDay >> 16);
        r[4] = (byte) (msOfDay >> 8);
        r[5] = (byte) (msOfDay);
        r[6] = (byte) (picoOfMillisec >> 24);
        r[7] = (byte) (picoOfMillisec >> 16);
        r[8] = (byte) (picoOfMillisec >> 8);
        r[9] = (byte) (picoOfMillisec);

        return r;
    }

    public long getPicosecInDay() {
        return picosecInDay;
    }

    public int getNumDays() {
        return numDays;
    }

    public Time toSle(int sleVersion) {
        return toSle(this, sleVersion);
    }

    /**
     * Gets the time from the SLE time class
     * 
     * @param time
     * @return
     */
    public static CcsdsTime fromSle(Time time) {
        if (time.getCcsdsPicoFormat() != null) {
            return fromCcsdsPico(time.getCcsdsPicoFormat().value);
        } else if (time.getCcsdsFormat() != null) {
            return fromCcsds(time.getCcsdsFormat().value);
        } else {
            throw new NullPointerException();
        }
    }

    public static CcsdsTime fromSle(ConditionalTime time) {
        if (time.getKnown() == null) {
            return null;
        } else {
            return fromSle(time.getKnown());
        }
    }

    public static Time toSle(CcsdsTime time, int sleVersion) {
        Time t = new Time();
        if (sleVersion > 2) {
            t.setCcsdsFormat(new TimeCCSDS(time.getDaySegmented()));
        } else {
            t.setCcsdsPicoFormat(new TimeCCSDSpico(time.getDaySegmentedPico()));
        }
        return t;
    }

    public static ConditionalTime toSleConditional(CcsdsTime time, int sleVersion) {
        if (time != null) {
            ConditionalTime ct = new ConditionalTime();
            ct.setKnown(toSle(time, sleVersion));
            return ct;
        } else {
            return Constants.COND_TIME_UNDEFINED;
        }
    }

    /**
     * Converts to java milliseconds. Note: this loses precision.
     * 
     * @return the java milliseconds since 1970.
     */
    public long toJavaMillisec() {
        return ((long) numDays - NUM_DAYS_1958_1970) * MS_IN_DAY + picosecInDay / 1000_000_000;
    }

    

    @Override
    public int compareTo(CcsdsTime o) {
        int x = Integer.compare(numDays, o.numDays);
        if (x == 0) {
            x = Long.compare(picosecInDay, o.picosecInDay);
        }
        return x;
    }
    
    /**
     * Formats the time with up to nanosecond resolution
     */
    public String toString() {
        Instant inst = Instant.ofEpochSecond(((long) numDays - NUM_DAYS_1958_1970) * SEC_IN_DAY, picosecInDay / 1000);
        return FORMATTER.format(inst);
    }
    
    /**
     * Converts to ISO8860 string with 12 digits picoseconds after dot
     * @return
     */
    public String toStringPico() {
        Instant inst = Instant.ofEpochSecond(((long) numDays - NUM_DAYS_1958_1970) * SEC_IN_DAY, picosecInDay / 1000);
        String s = FORMATTER_SEC.format(inst);
        //silly we have to remove the 'Z' from the string formatted by DateTimeFormatter
        return String.format("%s.%012dZ", s.substring(0, s.length()-1), picosecInDay % 1_000_000_000_000L);
    }
}
