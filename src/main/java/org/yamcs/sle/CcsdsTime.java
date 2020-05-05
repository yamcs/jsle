package org.yamcs.sle;

import java.time.Instant;

import ccsds.sle.transfer.service.common.types.Time;

/**
 * CCSDS time storing the number of days since 1958 and the number of picoseconds in the day
 * 
 */
public class CcsdsTime {
    static final int SEC_IN_DAY = 86400;
    static final int MS_IN_DAY = SEC_IN_DAY*1000;
    static final int NUM_DAYS_1958_1970 = 4383;

    final private int numDays;
   
    final private long picosecInDay;

 
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
     * @return
     */
    static public CcsdsTime now() {
        return fromJavaMillisec(System.currentTimeMillis());
       // return fromJavaMillisec(1000*(System.currentTimeMillis()/1000));
    }

    /**
     * Converts a java time in milliseconds
     * 
     * @param javaTime
     * @return
     */
    static public CcsdsTime fromJavaMillisec(long javaTime) {
        int numDays = (int) (javaTime / MS_IN_DAY) + NUM_DAYS_1958_1970;
        int msOfDay = (int) (javaTime % MS_IN_DAY);
        return new CcsdsTime(numDays, 1000_000_000L * msOfDay);
    }
    

    /**
     * Converts a UNIX time in seconds since 1970, picoseconds in second
     * 
     * @param javaTime
     * @return
     */
    static public CcsdsTime fromUnix(long unixSeconds, int picoSec) {
        int numDays = (int) (unixSeconds / SEC_IN_DAY) + NUM_DAYS_1958_1970;
        long picoOfDay = (unixSeconds % SEC_IN_DAY) + picoSec;
        return new CcsdsTime(numDays, picoOfDay);
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
        long microsec = picosecInDay/1000_000;
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

    public long getPicosecInDay() {
        return picosecInDay;
    }
    public int getNumDays() {
        return numDays;
    }

    /**
     * Converts to java milliseconds. Note: this loses precision.
     * 
     * @return the java milliseconds since 1970. 
     */
    public long toJavaMillisec() {
        return ((long) numDays - NUM_DAYS_1958_1970) * MS_IN_DAY + picosecInDay / 1000_000_000;
    }
    
    public String toString() {
        return Instant.ofEpochSecond(((long) numDays - NUM_DAYS_1958_1970) * SEC_IN_DAY, picosecInDay/1000).toString();
    }
    
    
}
