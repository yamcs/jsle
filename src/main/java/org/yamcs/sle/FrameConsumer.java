package org.yamcs.sle;

import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.RafProductionStatus;

import ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;

/**
 * consumes frames and receives notifications from the RAF service
 * 
 * @author nm
 *
 */
public interface FrameConsumer {

    /**
     * Transfer frame received from the service provider
     * 
     * @param rtdi
     */
    void acceptFrame(RafTransferDataInvocation rtdi);

    /**
     * producer signals that data has been discarded due to excessive backlog
     */
    void onExcessiveDataBacklog();

    /**
     * the service provider signals that the status of RAF production has changed:
     * 
     * @param productionStatusChange
     */
    void onProductionStatusChange(RafProductionStatus productionStatusChange);

    /**
     * the service provide signals that the delivery of frames has been interrupted because
     * the frame synchronisation process is not able to synchronise to the stream of frames
     * from the space link.
     * 
     */
    void onLossFrameSync(CcsdsTime time, LockStatus carrier, LockStatus subcarrier, LockStatus symbolSync);

    /**
     * the service provider signals that the space link session has ended and all available
     * frames have been delivered or all RAFs meeting the user selected delivery criteria
     * have been sent
     */
    void onEndOfData();

    

}
