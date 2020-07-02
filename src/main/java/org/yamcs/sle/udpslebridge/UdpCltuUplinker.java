package org.yamcs.sle.udpslebridge;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.ForwardDuStatus;
import org.yamcs.sle.provider.CltuUplinker;

import io.netty.buffer.ByteBufUtil;

public class UdpCltuUplinker implements CltuUplinker {
    static Logger logger = Logger.getLogger(UdpFrameReceiver.class.getName());

    String hostname;
    int port;
    int bitrate;
    InetAddress address;
    DatagramSocket socket;

    public UdpCltuUplinker(String hostname, int port, int bitrate) {
        this.hostname = hostname;
        this.port = port;
        this.bitrate = bitrate;
    }

    @Override
    public int start() {
        try {
            address = InetAddress.getByName(hostname);
            socket = new DatagramSocket();
        } catch (IOException e) {
            logger.warning(e.toString());
            return 1;// unable to comply
        }
        return -1;// ok
    }

    @Override
    public UplinkResult uplink(byte[] cltuData) {
        UplinkResult ur = new UplinkResult();

        DatagramPacket dtg = new DatagramPacket(cltuData, cltuData.length, address, port);
        ur.startTime = CcsdsTime.now();
        long durationNs = cltuData.length * 8 * 1000_000_000 / bitrate;
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
    public void stop() {
        if (socket != null) {
            socket.close();
        }
    }

}
