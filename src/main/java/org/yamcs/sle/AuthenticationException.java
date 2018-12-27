package org.yamcs.sle;

public class AuthenticationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String msg) {
        super(msg);
    }

    public AuthenticationException(String msg, Exception e) {
        super(msg, e);
    }
}
