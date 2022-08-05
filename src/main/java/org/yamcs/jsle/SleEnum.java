package org.yamcs.jsle;

/**
 * SLE enumerations have an id which is encoded on the wire.
 */
public interface SleEnum {
    public int id();

    public String name();

}