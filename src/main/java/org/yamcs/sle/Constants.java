package org.yamcs.sle;

import java.util.HashMap;
import java.util.Map;

import com.beanit.jasn1.ber.types.BerNull;

import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.SlduStatusNotification;

/**
 * This class encodes as static members and subclasses with static members a lot of the constants defined in the SLE
 * standards (sometimes not part of ASN1 but just as comments).
 * 
 * @author nm
 *
 */
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

    public final static Map<Integer, String> DIAGNOSTIC = new HashMap<>();
    static {
        DIAGNOSTIC.put(100, "duplicate invokeId");
        DIAGNOSTIC.put(127, "other reason");
    }

    public final static String[] CLTU_START_DIAGNOSTICS_SPECIFIC = {
            "outOfService", "unableToComply", "productionTimeExpired", "invalidCltuId"
    };

    public final static String[] RAF_START_DIAGNOSTICS_SPECIFIC = {
            "outOfService", "unableToComply", "invalidStartTime", "invalidStopTime", "missingTimeValue"
    };
    public final static String[] CLTU_TRANSFER_DIAGNOSTICS_SPECIFIC = {
       "unableToProcess", "unableToStore", "outOfSequence", "inconsistentTimeRange", "invalidTime", "lateSldu", "invalidDelayTime", "cltuError"
    };

    

    public final static BerNull BER_NULL = new BerNull();
    public final static SlduStatusNotification SLDU_NOTIFICATION_TRUE = new SlduStatusNotification(0);
    public final static SlduStatusNotification SLDU_NOTIFICATION_FALSE = new SlduStatusNotification(1);

    public final static ConditionalTime COND_TIME_UNDEFINED = new ConditionalTime();
    static {
        COND_TIME_UNDEFINED.setUndefined(BER_NULL);
    }

    public final static Credentials CREDENTIALS_UNUSED = new Credentials();
    static {
        CREDENTIALS_UNUSED.setUnused(BER_NULL);
    }

    public static enum ApplicationIdentifier {
        rtnAllFrames(0), rtnInsert(1), rtnChFrames(2),
        // rtnChFrames includes rtnMcFrames and rtnVcFrames
        rtnChFsh(3),
        // rtnChFsh includes rtnMcFsh and rtnVcFsh
        rtnChOcf(4),
        // rtnChOcf includes rtnMcOcf and rtnVcOcf
        rtnBitstr(5), // -- AOS
        rtnSpacePkt(6), fwdAosSpacePkt(7), fwdAosVca(8), fwdBitstr(9), fwdProtoVcdu(10),
        //
        fwdInsert(11), fwdCVcdu(12), fwdTcSpacePkt(13), // -- conventional telecommand
        fwdTcVca(14), // -- conventional telecommand
        fwdTcFrame(15), fwdCltu(16);
        private final int id;

        private ApplicationIdentifier(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public ApplicationIdentifier byId(int id) {
            for (ApplicationIdentifier v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum DeliveryMode implements SleEnum {
        rtnTimelyOnline(0), rtnCompleteOnline(1), rtnOffline(2), fwdOnline(3), fwdOffline(4);

        private final int id;

        private DeliveryMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static public DeliveryMode byId(int id) {
            for (DeliveryMode v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum FrameQuality implements SleEnum {
        good(0), erred(1), undetermined(2);
        private final int id;

        private FrameQuality(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static public FrameQuality byId(int id) {
            for (FrameQuality v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum ProductionStatus {
        running(0), interrupted(1), halted(2);
        private final int id;

        private ProductionStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public ProductionStatus byId(int id) {
            for (ProductionStatus v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum CltuProductionStatus {
        operational(0), configured(1), interrupted(2), halted(3);
        private final int id;

        private CltuProductionStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public CltuProductionStatus byId(int id) {
            for (CltuProductionStatus v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum LockStatus {
        inLock(0), outOfLock(1), notInUse(2), unknown(3);
        private final int id;

        private LockStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public LockStatus byId(int id) {
            for (LockStatus v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum ForwardDuStatus {
        radiated(0), expired(1), interrupted(2), acknowledged(3), productionStarted(4), productionNotStarted(
                5), unsupportedTransmissionMode(6);
        private final int id;

        private ForwardDuStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public ForwardDuStatus byId(int id) {
            for (ForwardDuStatus v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum UplinkStatus {
        uplinkStatusNotAvailable(0), noRfAvailable(1), noBitLock(2), nominal(3);
        private final int id;

        private UplinkStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public UplinkStatus byId(int id) {
            for (UplinkStatus v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum RequestedFrameQuality {
        goodFramesOnly(0), erredFramesOnly(1), allFrames(2);
        private final int id;

        private RequestedFrameQuality(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static public RequestedFrameQuality byId(int id) {
            for (RequestedFrameQuality v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum UnbindReason implements SleEnum {
        END(0), SUSPEND(1), VERSION_NOT_SUPPORTED(2), OTHER(127);

        private final int id;

        private UnbindReason(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static public UnbindReason byId(int id) {
            for (UnbindReason v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum NotificationMode implements SleEnum {
        DEFERRED(0), IMMEDIATE(1);

        private final int id;

        private NotificationMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static public NotificationMode byId(int id) {
            for (NotificationMode v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static enum CltuThrowEventDiagnostics implements SleEnum {
        operationNotSupported(0), eventInvocIdOutOfSequence(1), noSuchEvent(2);

        private final int id;

        private CltuThrowEventDiagnostics(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static public CltuThrowEventDiagnostics byId(int id) {
            for (CltuThrowEventDiagnostics v : values()) {
                if (v.id == id) {
                    return v;
                }
            }
            throw new IllegalArgumentException("invalid id " + id);
        }
    }

    public static String getDiagnostic(int idx, String[] v) {
        if (idx >= v.length) {
            return "unknown(" + idx + ")";
        } else {
            return v[idx];
        }
    }

    public static String getCommonDiagnostic(int id) {
        return DIAGNOSTIC.getOrDefault(id, "unknown (" + id + ")");
    }

    public static String getEnumName(int id, SleEnum[] v) {
        for (SleEnum se : v) {
            if (se.id() == id) {
                return se.name();
            }
        }
        return "unknown (" + id + ")";
    }
}
