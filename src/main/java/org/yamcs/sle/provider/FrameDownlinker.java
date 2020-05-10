package org.yamcs.sle.provider;

public interface FrameDownlinker {
    /**
     * Called at the beginning to initialise.
     * 
     * @param rsp
     *            - the frames should be forwarded to this
     */
    void init(RafServiceProvider rsp);

    

    /**
     * Called at SLE start to start the provision of frames
     * 
     * @return -1 if the result is successful. return greater or equal with 0 means error and the code will be inserted
     *         into the specific part of the SLE return message
     */
    int start();
    
    /**
     * Called at SLE stop
     */
    void stop();
    
    /**
     * Called when the RAF service is unbound
     */
    void shutdown();
}
