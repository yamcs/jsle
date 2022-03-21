package org.yamcs.sle.udpslebridge;

public class TimestampedFrameData {
    final long timeMillis;
    final int timePicos;
    final byte[] data;

    public TimestampedFrameData(long timeMillis, int timePicos, byte[] data) {
        this.timeMillis = timeMillis;
        this.timePicos = timePicos;
        this.data = data;
    }

}
