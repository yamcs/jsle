package org.yamcs.sle;

import java.util.HashMap;
import java.util.Map;

import org.openmuc.jasn1.ber.types.BerNull;

import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.SlduStatusNotification;

public class Constants {
    public final static int APP_ID_FWD_CLTU = 16;

    public final static Map<Integer, String> BIND_DIAGNOSTIC = new HashMap<>();
    static {
        BIND_DIAGNOSTIC.put(0, "access denied");
        BIND_DIAGNOSTIC.put(1, "service type not supported");
        BIND_DIAGNOSTIC.put(2, "version not supported");
        BIND_DIAGNOSTIC.put(3, "no such service instance");
        BIND_DIAGNOSTIC.put(4, "already bound");
        BIND_DIAGNOSTIC.put(5, "si not accessible to this initiator");
        BIND_DIAGNOSTIC.put(6, "inconsistent service type");
        BIND_DIAGNOSTIC.put(7, "invalid time");
        BIND_DIAGNOSTIC.put(8, "out of service");
        BIND_DIAGNOSTIC.put(127, "other reason");
    }

    final static BerNull BER_NULL = new BerNull();
    final static SlduStatusNotification SLDU_NOTIFICATION_TRUE = new SlduStatusNotification(0);
    final static SlduStatusNotification SLDU_NOTIFICATION_FALSE = new SlduStatusNotification(1);
    final static ConditionalTime COND_TIME_UNDEFINED = new ConditionalTime();
    static {
        COND_TIME_UNDEFINED.setUndefined(BER_NULL);
    }

    final static Credentials CREDENTIALS_UNUSED = new Credentials();
    static {
        CREDENTIALS_UNUSED.setUnused(BER_NULL);
    }

    static class DeliveryMode {
        final static int rtnTimelyOnline = 0;
        final static int rtnCompleteOnline = 1;
        final static int rtnOffline = 2;
        final static int fwdOnline = 3;
        final static int fwdOffline = 4;
    }
    
    static class ParameterName {
        public static final int acquisitionSequenceLength = 201;
        public static final int apidList = 2;
        public static final int bitLockRequired = 3;
        public static final int blockingTimeoutPeriod = 0;
        public static final int blockingUsage = 1;
        public static final int bufferSize = 4;
        public static final int clcwGlobalVcId = 202;
        public static final int clcwPhysicalChannel = 203;
        public static final int copCntrFramesRepetition = 300;
        public static final int deliveryMode = 6;
        public static final int directiveInvocation = 7;
        public static final int directiveInvocationOnline = 108;
        public static final int expectedDirectiveIdentification = 8;
        public static final int expectedEventInvocationIdentification = 9;
        public static final int expectedSlduIdentification = 10;
        public static final int fopSlidingWindow = 11;
        public static final int fopState = 12;
        public static final int latencyLimit = 15;
        public static final int mapList = 16;
        public static final int mapMuxControl = 17;
        public static final int mapMuxScheme = 18;
        public static final int maximumFrameLength = 19;
        public static final int maximumPacketLength = 20;
        public static final int maximumSlduLength = 21;
        public static final int minimumDelayTime = 204;
        public static final int minReportingCycle = 301;
        public static final int modulationFrequency = 22;
        public static final int modulationIndex = 23;
        public static final int notificationMode = 205;
        public static final int permittedControlWordTypeSet = 101;
        public static final int permittedFrameQuality = 302;
        public static final int permittedGvcidSet = 24;
        public static final int permittedTcVcidSet = 102;
        public static final int permittedTransmissionMode = 107;
        public static final int permittedUpdateModeSet = 103;
        public static final int plop1IdleSequenceLength = 206;
        public static final int plopInEffect = 25;
        public static final int protocolAbortMode = 207;
        public static final int reportingCycle = 26;
        public static final int requestedControlWordType = 104;
        public static final int requestedFrameQuality = 27;
        public static final int requestedGvcid = 28;
        public static final int requestedTcVcid = 105;
        public static final int requestedUpdateMode = 106;
        public static final int returnTimeoutPeriod = 29;
        public static final int rfAvailable = 30;
        public static final int rfAvailableRequired = 31;
        public static final int segmentHeader = 32;
        public static final int sequCntrFramesRepetition = 303;
        public static final int subcarrierToBitRateRatio = 34;
        public static final int throwEventOperation = 304;
        public static final int timeoutType = 35;
        public static final int timerInitial = 36;
        public static final int transmissionLimit = 37;
        public static final int transmitterFrameSequenceNumber = 38;
        public static final int vcMuxControl = 39;
        public static final int vcMuxScheme = 40;
        public static final int virtualChannel = 41;
    }
}
