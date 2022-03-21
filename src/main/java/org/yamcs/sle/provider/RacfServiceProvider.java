package org.yamcs.sle.provider;


import org.yamcs.sle.CcsdsTime;
import org.yamcs.sle.Constants.FrameQuality;


/**
 * Common class for RAF and RCF service providers
 * @author nm
 *
 */
public abstract class RacfServiceProvider implements SleService {
    /**
     * Called by the {@link FrameSource} to send a new frame.
     * 
     * @param ert
     * @param frameQuality
     * @param dataLinkContinuity
     *            from CCSDS spec:
     *            <ul>
     *            <li>a value of ‘–1’ shall indicate that this is the first frame after the start of production;</li
     *            <li> a value of ‘0’ shall indicate that this frame is the direct successor to the last frame acquired
     *            from the space link by RAF production;</li
     *            <li> any non-zero positive value shall indicate that this frame is not the direct successor to the
     *            last frame acquired from the space link:</li
     *            <li> a non-zero positive value further indicates an estimate of the number of frames that were missed
     *            since the last frame acquired before this frame;</li>
     *            <li>a value of ‘1’ may be used if no better estimate is available.</li>
     *            </ul>
     * @param data
     *            frame data
     * @param dataOffset
     *            where in the data the buffer the frame starts
     * @param dataLength
     *            the length of the frame data
     */
    public abstract void sendFrame(CcsdsTime ert, FrameQuality frameQuality, int dataLinkContinuity, byte[] data, int dataOffset,
            int dataLength);

    /**
     * Called by the {@link FrameSource} to signal the end of data (used for offline instances)
     */
    public abstract void sendEof();
}
