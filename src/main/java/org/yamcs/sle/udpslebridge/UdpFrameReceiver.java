package org.yamcs.sle.udpslebridge;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Logger;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.FrameQuality;
import org.yamcs.sle.provider.FrameDownlinker;
import org.yamcs.sle.provider.RacfServiceProvider;

public class UdpFrameReceiver implements FrameDownlinker, Runnable {
    static Logger logger = Logger.getLogger(UdpFrameReceiver.class.getName());
    
    RacfServiceProvider rsp;
    int port;
    private DatagramSocket socket;
    Thread runner;
    
    int maxFrameLength;
    boolean first = true;
    
    private volatile boolean stopping = false;

    public UdpFrameReceiver(int port, int maxFrameLength) {
        this.port = port;
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    public void init(RacfServiceProvider rsp) {
        this.rsp = rsp;

    }

    @Override
    public void run() {
        logger.info("Listening for UDP frames at port "+ port);
        while (!stopping) {
            DatagramPacket datagram = new DatagramPacket(new byte[maxFrameLength], maxFrameLength);
            try {
                socket.receive(datagram);
            } catch (IOException e) {
                if (stopping) {
                    return;
                }
                logger.warning("Error receiving datagram: " + e);
                continue;
            }
            int dataLinkContinuity;
            if(first) {
                dataLinkContinuity = -1;
                first = false;
            } else {
                dataLinkContinuity = 0;//no frame missing
            }
            System.out.println("sending datagram of size "+datagram.getLength());
            rsp.sendFrame(CcsdsTime.now(), FrameQuality.good, dataLinkContinuity, datagram.getData(), datagram.getOffset(),
                    datagram.getLength());
        }
    }

    @Override
    public int start() {
        stopping = false;
        first = true;
        try {
            socket = new DatagramSocket(port);
            runner = new Thread(this);
            runner.start();
        } catch (SocketException e) {
            logger.warning("Cannot create datagram socket: " + e);
            return 1;// unable to comply
        }
        // -1 means ok
        return -1;
    }

    @Override
    public void stop() {
        stopping = true;
        if (runner != null) {
            runner.interrupt();
            runner = null;
        }
        if (socket != null) {
            socket.close();
        }
    }
    
    @Override
    public void shutdown() {
        stop();
    }
}
