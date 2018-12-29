package org.yamcs.sle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.openmuc.jasn1.ber.ReverseByteArrayOutputStream;
import org.openmuc.jasn1.ber.types.BerInteger;
import org.openmuc.jasn1.ber.types.BerOctetString;
import org.openmuc.jasn1.ber.types.string.BerVisibleString;

import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.isp1.credentials.HashInput;
import ccsds.sle.transfer.service.isp1.credentials.ISP1Credentials;

public class Isp1Authentication {
    public enum HashAlgorithm {
        SHA1, SHA256
    };

    final BerVisibleString myUsername;
    final String peerUsername;
    final BerOctetString myPass;
    final byte[] peerPass;
    MessageDigest digest;

    final SecureRandom random;
    int bufferSize = 128;

    public Isp1Authentication(String myUsername, byte[] myPass, String peerUsername, byte[] peerPass,
            String hashAlgorithm) {
        super();
        this.myUsername = new BerVisibleString(myUsername);
        this.peerUsername = peerUsername;
        this.myPass = new BerOctetString(myPass);
        this.peerPass = peerPass;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            digest = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public ISP1Credentials generateIsp1Credentials() {
        int rn = random.nextInt() & Integer.MAX_VALUE; // zero out the sign bit
        byte[] time = CcsdsTime.getDaySegmented(System.currentTimeMillis());
        BerOctetString berTime = new BerOctetString(time);
        BerInteger berRn = new BerInteger(rn);

        HashInput hi = new HashInput();
        hi.setPassWord(myPass);
        hi.setRandomNumber(berRn);
        hi.setUserName(myUsername);
        hi.setTime(berTime);
        ReverseByteArrayOutputStream rbaos = new ReverseByteArrayOutputStream(128, true);
        try {
            hi.encode(rbaos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] theProtected = digest.digest(rbaos.getArray());

        ISP1Credentials cred = new ISP1Credentials();
        cred.setRandomNumber(berRn);
        cred.setTheProtected(new BerOctetString(theProtected));
        cred.setTime(berTime);

        return cred;
    }

    public Credentials generateCredentials() {
        Credentials c = new Credentials();
        ISP1Credentials cr = generateIsp1Credentials();
        ReverseByteArrayOutputStream rbaos = new ReverseByteArrayOutputStream(bufferSize, true);
        try {
            cr.encode(rbaos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        c.setUsed(new BerOctetString(rbaos.getArray()));

        return c;
    }

}
