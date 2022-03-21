package org.yamcs.sle.provider;

import java.util.concurrent.CompletableFuture;

import org.yamcs.sle.CcsdsTime;

public interface FrameSource {

    /**
     * Called at the beginning to create socket, open connection, etc
     */
    void startup();

    /**
     * Undo what init has done
     */
    void shutdown();


    /**
     * Called at SLE start to start the provision of frames
     * 
     * @param stop
     * @param start
     * 
     * @return -1 if the result is successful. return greater or equal with 0 means error and the code will be inserted
     *         into the specific part of the SLE return message
     */
    CompletableFuture<Integer> start(RacfServiceProvider rsp, CcsdsTime start, CcsdsTime stop);
    
    /**
     * Called at SLE stop
     */
    void stop(RacfServiceProvider rsp);

}
