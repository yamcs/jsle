package org.yamcs.jsle.provider;

import org.yamcs.jsle.Isp1Authentication;

public interface AuthProvider {
    Isp1Authentication getAuth(String initiatorId);
}
