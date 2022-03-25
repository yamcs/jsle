package org.yamcs.sle.user;

import java.util.concurrent.CompletableFuture;

import org.yamcs.sle.Isp1Authentication;
import org.yamcs.sle.ParameterName;
import org.yamcs.sle.SleParameter;
import org.yamcs.sle.RacfSleMonitor;
import org.yamcs.sle.Constants.DeliveryMode;

/**
 * Implementation for the CCSDS RECOMMENDED STANDARD FOR SLE RAF SERVICE
 * CCSDS 911.1-B-4 August 2016 https://public.ccsds.org/Pubs/911x1b4.pdf
 * <p>
 * and
 * <p>
 * CCSDS 911.1-B-4 August 2016 https://public.ccsds.org/Pubs/911x1b4.pdf
 * <p>
 * The RAF and RCF service are almost the same but slightly so different
 * 
 * <p>
 * We do however provide one common RAF/RCF interface to our users highlighting the small differences in the API.
 * 
 * @author nm
 *
 */
public abstract class RacfServiceUserHandler extends AbstractServiceUserHandler {
    FrameConsumer consumer;
    private DeliveryMode deliveryMode;

    public RacfServiceUserHandler(Isp1Authentication auth, SleAttributes attr, DeliveryMode deliveryMode,
            FrameConsumer consumer) {
        super(auth, attr);
        this.consumer = consumer;
        setDeliveryMode(deliveryMode);
    }

    /**
     * Get the value of an RAF parameter from the provider
     * 
     * @param parameterId
     *            one of the parameters defined in {@link ParameterName}. Note that not all of them make sense for the
     *            RAF, see the table 3-11 in the standard to see which ones make sense.
     * @return
     */
    public abstract CompletableFuture<SleParameter> getParameter(int parameterId);

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        checkUnbound();
        this.deliveryMode = deliveryMode;
    }

    /**
     * Add a monitor to be notified when events happen.
     * 
     * @param monitor
     */
    public void addMonitor(RacfSleMonitor monitor) {
        monitors.add(monitor);
    }

    public void removeMonitor(RacfSleMonitor monitor) {
        monitors.remove(monitor);
    }
}
