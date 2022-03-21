package org.yamcs.sle.udpslebridge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.FrameQuality;
import org.yamcs.sle.provider.FrameSource;
import org.yamcs.sle.provider.RacfServiceProvider;

/**
 * Receives frames via UDP
 *
 */
public class UdpFrameSource implements FrameSource, Runnable {
    static Logger logger = Logger.getLogger(UdpFrameSource.class.getName());
    
    final String id;
    CopyOnWriteArrayList<RacfServiceProvider> rsps = new CopyOnWriteArrayList<RacfServiceProvider>();
    final int port;
    private DatagramSocket socket;
    Thread runner;
    
    final int maxFrameLength;
    
    private volatile boolean stopping = false;
    FrameRecorder recorder;

    public UdpFrameSource(Properties properties, String id) {
        this.id = id;
        this.port = Integer.valueOf(Util.getProperty(properties, "fsource." + id + ".port"));
        this.maxFrameLength = Integer.valueOf(properties.getProperty("fsource." + id + ".port", "1115"));
        String dataDir = properties.getProperty("fsource." + id + ".record", null);
        if (dataDir != null) {
            recorder = new FrameRecorder(dataDir);
        }
    }

    @Override
    public void startup() {
        stopping = false;
        try {
            socket = new DatagramSocket(port);
            runner = new Thread(this);
            runner.start();
        } catch (SocketException e) {
            logger.warning(id + ": cannot create datagram socket: " + e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CompletableFuture<Integer> start(RacfServiceProvider rsp, CcsdsTime start, CcsdsTime stop) {
        rsps.add(rsp);
        return CompletableFuture.completedFuture(-1);// ok
    }

    @Override
    public void run() {
        logger.info(id + ": listening for UDP frames at port " + port);
        if (recorder != null) {
            logger.info(id + ": recording frames in " + recorder.rootDir);
        }
        while (!stopping) {
            DatagramPacket datagram = new DatagramPacket(new byte[maxFrameLength], maxFrameLength);
            try {
                socket.receive(datagram);
            } catch (IOException e) {
                if (stopping) {
                    return;
                }
                logger.warning(id + ": error receiving datagram: " + e);
                continue;
            }
            int dataLinkContinuity;
            dataLinkContinuity = 0;// no frame missing
            logger.fine("received datagram of size " + datagram.getLength());
            long t = System.currentTimeMillis();
            CcsdsTime tc = CcsdsTime.fromJavaMillis(t);
            if (recorder != null) {
                try {
                    recorder.recordFrame(t, datagram.getData(), datagram.getOffset(), datagram.getLength());
                } catch (IOException e) {
                    logger.warning(id + ": error saving frame, stopping recording; " + e);
                    recorder = null;
                }
            }
            rsps.forEach(rsp -> rsp.sendFrame(tc, FrameQuality.good, dataLinkContinuity,
                    datagram.getData(), datagram.getOffset(), datagram.getLength()));
        }
    }

    @Override
    public void stop(RacfServiceProvider rsp) {
        rsps.remove(rsp);
    }

    @Override
    public void shutdown() {
        stopping = true;
        if (runner != null) {
            runner.interrupt();
            runner = null;
        }
        if (socket != null) {
            socket.close();
        }
    }


}
