package org.yamcs.sle.udpslebridge;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.FrameQuality;
import org.yamcs.sle.provider.FrameSource;
import org.yamcs.sle.provider.RacfServiceProvider;

public class FileFrameSource implements FrameSource {
    final String dataDir;
    final String id;

    static Logger logger = Logger.getLogger(UdpFrameSource.class.getName());

    Map<RacfServiceProvider, Replayer> replayers = new ConcurrentHashMap<>();

    public FileFrameSource(Properties properties, String id) {
        this.id = id;
        this.dataDir = Util.getProperty(properties, "fsource." + id + ".data");
    }

    @Override
    public void startup() {
        logger.info(id + ": replaying files from " + dataDir);
    }

    @Override
    public void shutdown() {
        for (Replayer r : replayers.values()) {
            r.stop();
        }
    }

    @Override
    public void stop(RacfServiceProvider rsp) {
        Replayer r = replayers.get(rsp);
        if (r != null) {
            r.stop();
        }
    }

    @Override
    public CompletableFuture<Integer> start(RacfServiceProvider rsp, CcsdsTime start, CcsdsTime stop) {
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        if (start == null || stop == null) {
            cf.complete(4);// missing time value
        } else {
            Iterator<TimestampedFrameData> it = new FrameFileIterator(dataDir, start.toJavaMillisec(),
                    stop.toJavaMillisec());
            Replayer r = new Replayer(cf, rsp, it);
            logger.info(id + ": starting replay of [" + start + ", " + stop + ")");
            new Thread(r).start();
        }
        return cf;
    }

    class Replayer implements Runnable {
        final Iterator<TimestampedFrameData> it;
        final RacfServiceProvider rsp;
        final CompletableFuture<Integer> cf;
        volatile boolean stopping = false;
        long count = 0;

        public Replayer(CompletableFuture<Integer> cf, RacfServiceProvider rsp, Iterator<TimestampedFrameData> it) {
            this.it = it;
            this.rsp = rsp;
            this.cf = cf;
        }

        @Override
        public void run() {
            cf.complete(-1);// no error
            while (it.hasNext() && !stopping) {
                TimestampedFrameData tfd = it.next();
                rsp.sendFrame(CcsdsTime.fromJavaMillisPicos(tfd.timeMillis, tfd.timePicos), FrameQuality.good, 0,
                        tfd.data, 0, tfd.data.length);
                count++;
            }
            logger.info(id + ": replay finished, sent " + count + " frames; sending EOF");
            rsp.sendEof();
        }

        void stop() {
            stopping = true;
        }
    }

}
