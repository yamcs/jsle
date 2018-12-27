package org.yamcs.sle;

public class CcsdsTime {
    static final int MS_IN_DAY = 86400_000;
    static final int NUM_DAYS_1958_1970 = 4383;

    /**
     * CCSDS DAY segmented time without the P-field
     *
     * bytes 0-1 : number of days since 1-Jan-1958
     * bytes 2-5 : millisecond of day
     * bytes 6-7 : sub-millisecond of day
     *
     * Note that the sub-millisecond relays on System.nanoTime so it's not accurate but should be incrementing
     * 
     * @return
     */
    public static byte[] getDaySegmented(long javaTime) {
        byte[] r = new byte[8];
        System.out.println("javaTime: "+javaTime);
        int numDays = (int) (javaTime / MS_IN_DAY) + NUM_DAYS_1958_1970;
        System.out.println("javaTime: "+javaTime+" numDays: "+numDays);
        int msOfDay = (int) (javaTime % MS_IN_DAY);
        //TODO fix microseconds with Java 9.
        int microsec = 0;
        r[0] = (byte) (numDays >> 8);
        r[1] = (byte) (numDays);
        r[2] = (byte) (msOfDay >> 24);
        r[3] = (byte) (msOfDay >> 16);
        r[4] = (byte) (msOfDay >> 8);
        r[5] = (byte) (msOfDay);
        r[6] = (byte) (microsec >> 8);
        r[7] = (byte) (microsec);

        return r;
    }

}
