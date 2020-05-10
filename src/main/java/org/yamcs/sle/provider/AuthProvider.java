package org.yamcs.sle.provider;

import org.yamcs.sle.Isp1Authentication;

public interface AuthProvider {
    Isp1Authentication getAuth(String initiatorId);
}
