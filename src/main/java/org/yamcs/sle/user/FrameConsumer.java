package org.yamcs.sle.user;

import org.yamcs.sle.AntennaId;
import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.FrameQuality;
import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.ProductionStatus;


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
     * <p>For RCF the Frame Quality will always be set to good
     */
    void acceptFrame(CcsdsTime ert, AntennaId antennaId, int dataLinkContinuity,
            FrameQuality frameQuality, byte[] privAnn, byte[] data);

    /**
     * producer signals that data has been discarded due to excessive backlog
     */
    void onExcessiveDataBacklog();

    /**
     * the service provider signals that the status of RAF production has changed:
     * 
     * @param productionStatusChange
     */
    void onProductionStatusChange(ProductionStatus productionStatusChange);

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
