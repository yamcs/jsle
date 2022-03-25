package org.yamcs.sle.udpslebridge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.CltuThrowEventDiagnostics;
import org.yamcs.sle.Constants.ForwardDuStatus;
import org.yamcs.sle.provider.CltuParameters;
import org.yamcs.sle.provider.CltuServiceProvider;
import org.yamcs.sle.provider.FrameSink;

import io.netty.buffer.ByteBufUtil;

public class UdpFrameSink implements FrameSink {
    static Logger logger = Logger.getLogger(UdpFrameSource.class.getName());

    final String hostname;
    final int port;
    final int bitrate;
    InetAddress address;
    DatagramSocket socket;
    String id;
    CltuParameters cltuParameters;

    public UdpFrameSink(String hostname, int port, int bitrate) {
        this.hostname = hostname;
        this.port = port;
        this.bitrate = bitrate;
    }

    public UdpFrameSink(Properties properties, String id) {
        this.hostname = Util.getProperty(properties, "fsink." + id + ".host");
        this.port = Integer.valueOf(Util.getProperty(properties, "fsink." + id + ".port"));
        this.bitrate = Integer.valueOf(properties.getProperty("fsink." + id + ".bitrate", "10000"));
    }

    @Override
    public void startup() {
        try {
            address = InetAddress.getByName(hostname);
            socket = new DatagramSocket();
        } catch (IOException e) {
            logger.warning(e.toString());
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public UplinkResult uplink(byte[] cltuData) {
        UplinkResult ur = new UplinkResult();

        DatagramPacket dtg = new DatagramPacket(cltuData, cltuData.length, address, port);
        ur.startTime = CcsdsTime.now();
        long durationNs = cltuData.length * 8L * 1000_000_000L / bitrate;
        int millis = (int) (durationNs / 1000_000);
        int nanos = (int) (durationNs % 1000_000);

        try {
            Thread.sleep(millis, nanos);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            ur.cltuStatus = ForwardDuStatus.interrupted;
            return ur;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Sending UDP CLTU " + ByteBufUtil.hexDump(cltuData));
        }

        try {
            socket.send(dtg);
            ur.cltuStatus = ForwardDuStatus.radiated;
            ur.stopTime = CcsdsTime.now();
            return ur;
        } catch (IOException e) {
            logger.warning("Error sending datagram" + e);
            ur.cltuStatus = ForwardDuStatus.interrupted;
            return ur;
        }
    }
    @Override
    public CltuThrowEventDiagnostics throwEvent(int evId, byte[] eventQualifier) {
        String evq = new String(eventQualifier);
        logger.info("Received throw event id: " + evId + " qualifier: " + evq);
        if (evId > 4) {
            return CltuThrowEventDiagnostics.noSuchEvent;
        }
        // TODO change bitrate?
        return null;// ok
    }

    @Override
    public int start(CltuServiceProvider csp) {
        this.cltuParameters = csp.getParameters();
        // TODO set the bitrate in the cltuParameters or the other way around?
        return -1;// ok
    }

    @Override
    public int stop(CltuServiceProvider csp) {
        return -1; // ok
    }

    @Override
    public void shutdown() {
        if (socket != null) {
            socket.close();
        }
    }

}
