package org.yamcs.sle.provider;

import java.io.IOException;
import java.io.InputStream;

import org.yamcs.sle.State;

import com.beanit.jasn1.ber.BerTag;

/**
 * This interface has to be implemented by specific service providers.
 * <p>
 * It is used by the {@link SleProvider} to delegate the execution of operation in ACTIVE and READY (i.e. after bind
 * took place) SLE states.
 * 
 * @author nm
 *
 */
public interface SleService {
    /**
     * Called at the beginning. to initialize the service.
     * 
     * @param provider
     */
    void init(SleProvider provider);

    /**
     * Called each time a message is received from the peer (SLE user) which cannot be processed by the
     * {@link SleProvider}.
     * <p>
     * The messages are not necessary valid. The service can always call the {@link SleProvider#peerAbort()} operation
     * on the provider if it thinks the SLE user went awry.
     */
    void processData(BerTag berTag, InputStream is) throws IOException;

    void sendStatusReport();

    /**
     * Returns the service state.
     * <p>
     * Should be one of {@link State#READY} or {@link State#ACTIVE}
     * 
     * @return the service state.
     */
    State getState();

    /**
     * Called by the {@link SleProvider} when the connection has been abruptly terminated. The Service has to do
     * necessary cleanup operation.
     */
    void abort();

    /**
     * Called by the {@link SleProvider} when the user sent an UNBIND request. The provider will verify that the service
     * is in the {@link State#READY} when calling this method.
     */
    void unbind();

}
