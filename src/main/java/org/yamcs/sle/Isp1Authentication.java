package org.yamcs.sle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;

import com.beanit.jasn1.ber.ReverseByteArrayOutputStream;
import com.beanit.jasn1.ber.types.BerInteger;
import com.beanit.jasn1.ber.types.BerOctetString;
import com.beanit.jasn1.ber.types.string.BerVisibleString;

import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.isp1.credentials.HashInput;
import ccsds.sle.transfer.service.isp1.credentials.ISP1Credentials;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class Isp1Authentication {
    // this is configured as authenticationDelay
    public static final int DEFAULT_MAX_DELTA_RCV_TIME_SEC = 180;// 3 min
    public enum HashAlgorithm {
        SHA1, SHA256
    };

    // this will cause passwords to be printed in the log files
    boolean debugAuth = false;

    final BerVisibleString myUsername;
    final BerVisibleString peerUsername;
    final BerOctetString myPass;
    final BerOctetString peerPass;
    MessageDigest digest;

    final SecureRandom random;
    int bufferSize = 128;
    // used for verifying the remote credentials, the difference in time between the received credentials and the
    // current time
    private long maxDeltaRcvTimeMillis = DEFAULT_MAX_DELTA_RCV_TIME_SEC * 1000;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Isp1Handler.class);

    public Isp1Authentication(String myUsername, byte[] myPass, String peerUsername, byte[] peerPass,
            String hashAlgorithm) {
        super();
        this.myUsername = new BerVisibleString(myUsername);
        this.peerUsername = new BerVisibleString(peerUsername);
        this.myPass = new BerOctetString(myPass);
        this.peerPass = new BerOctetString(peerPass);
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            digest = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public ISP1Credentials generateIsp1Credentials() {
        BerInteger rn = new BerInteger(random.nextInt() & Integer.MAX_VALUE); // zero out the sign bit
        BerOctetString time = new BerOctetString(CcsdsTime.now().getDaySegmented());
        byte[] theProtected = getTheProtected(myUsername, myPass, rn, time);

        ISP1Credentials cred = new ISP1Credentials();
        cred.setRandomNumber(rn);
        cred.setTheProtected(new BerOctetString(theProtected));
        cred.setTime(time);

        return cred;
    }

    byte[] getTheProtected(BerVisibleString username, BerOctetString pass, BerInteger rn, BerOctetString time) {
        HashInput hi = new HashInput();
        hi.setPassWord(pass);
        hi.setRandomNumber(rn);
        hi.setUserName(username);
        hi.setTime(time);
        ReverseByteArrayOutputStream rbaos = new ReverseByteArrayOutputStream(128, true);
        try {
            hi.encode(rbaos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return digest.digest(rbaos.getArray());
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

    public long getMaxDeltaRcvTime() {
        return maxDeltaRcvTimeMillis;
    }

    /**
     * Set the maximum delta between local time and received credential time used in the
     * {@link #verifyCredentials(Credentials)}.
     * 
     * @param maxDeltaRcvTime
     *            delta time in milliseconds
     */
    public void setMaxDeltaRcvTime(long maxDeltaRcvTime) {
        this.maxDeltaRcvTimeMillis = maxDeltaRcvTime;
    }

    /**
     * Verify credentials and throw an exception if they are not correct.
     * This method uses {@link System#currentTimeMillis()} to get the time in milliseconds and compare it with the
     * received time and {@link #maxDeltaRcvTimeMillis}.
     * 
     * @param credentials
     *            the credentials to be verified
     * @throws AuthenticationException
     *             in case the credentials do not match
     */
    public void verifyCredentials(Credentials credentials) throws AuthenticationException {
        ISP1Credentials cr = new ISP1Credentials();
        BerOctetString bos = credentials.getUsed();
        if (bos == null) {
            throw new AuthenticationException("Provider did not provide credentials");
        }
        try {
            cr.decode(new ByteArrayInputStream(bos.value));
        } catch (Exception e) {
            throw new AuthenticationException("Cannot decode provider's credentials", e);
        }
        CcsdsTime tokenTime = CcsdsTime.fromCcsds(cr.getTime().value);
        long currentTime = System.currentTimeMillis();
        if (logger.isTraceEnabled()) {
            logger.trace("Current time: {} token time:  {} maxDeltaRcvTimeMillis: {}",
                    Instant.ofEpochMilli(currentTime), tokenTime, maxDeltaRcvTimeMillis);
        }
        if (currentTime - tokenTime.toJavaMillisec() > maxDeltaRcvTimeMillis) {
            throw new AuthenticationException("Received provider's credentials are too old");
        }
        if (debugAuth) {
            logger.debug("verifying credentials {} against ({}, {})", cr, peerUsername, peerPass);
        }

        byte[] rcv = getTheProtected(peerUsername, peerPass, cr.getRandomNumber(), cr.getTime());
        if (!Arrays.equals(rcv, cr.getTheProtected().value)) {
            throw new AuthenticationException(
                    "Received provider's credentials are not correct (hash does not match computed hash)");
        }
    }

    public String getMyUsername() {
        return myUsername.toString();
    }

    /**
     * if set to true, authentication messages containing passwords will be logged at DEBUG level.
     * <p>
     * Do not forget to switch off!
     * 
     * @param debugAuth
     */
    public void debugAuth(boolean debugAuth) {
        this.debugAuth = debugAuth;
    }
}
