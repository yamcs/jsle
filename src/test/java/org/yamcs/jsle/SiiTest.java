package org.yamcs.jsle;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.yamcs.jsle.StringConverter;

public class SiiTest {
    @Test
    public void test1() {
        String s= "sagr=SAGR.spack=SPACK.rsl-fg=RSL-FG.raf=onlt1";
        assertEquals(s, StringConverter.toString(StringConverter.parseServiceInstanceIdentifier(s)));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalid1() {
        StringConverter.parseServiceInstanceIdentifier("aa=SAGR.spack=SPACK.rsl-fg=RSL-FG.raf=onlt1");
    }
}
