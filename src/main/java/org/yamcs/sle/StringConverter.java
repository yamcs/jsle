package org.yamcs.sle;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.beanit.jasn1.ber.types.BerObjectIdentifier;
import com.beanit.jasn1.ber.types.string.BerVisibleString;

import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuStart;
import ccsds.sle.transfer.service.common.types.Diagnostics;
import ccsds.sle.transfer.service.raf.structures.DiagnosticRafStart;
import ccsds.sle.transfer.service.rcf.structures.DiagnosticRcfStart;
import ccsds.sle.transfer.service.service.instance.id.OidValues;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceAttribute;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;

public class StringConverter {
    public static String toString(ServiceInstanceIdentifier sii) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ServiceInstanceAttribute sia : sii.getServiceInstanceAttribute()) {
            if (first) {
                first = false;
            } else {
                sb.append(".");
            }
            ServiceInstanceAttribute.SEQUENCE sias = sia.getSEQUENCE().get(0);
            sb.append(toString(sias.getIdentifier())).append("=").append(sias.getSiAttributeValue().toString());
        }
        return sb.toString();
    }

    public static String toString(BerObjectIdentifier boi) {
        if (Arrays.equals(boi.value, OidValues.fslFg.value)) {
            return "fsl-fg";
        }
        if (Arrays.equals(boi.value, OidValues.rslFg.value)) {
            return "rsl-fg";
        }
        for (Field f : OidValues.class.getFields()) {
            try {
                Object o = f.get(OidValues.class);
                if (o instanceof BerObjectIdentifier) {
                    BerObjectIdentifier boi1 = (BerObjectIdentifier) o;
                    if (Arrays.equals(boi.value, boi1.value)) {
                        return f.getName();
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return "unknown" + Arrays.toString(boi.value);
    }
    
    static public ServiceInstanceIdentifier parseServiceInstanceIdentifier(String sii) {
        ServiceInstanceIdentifier servInstId = new ServiceInstanceIdentifier();
        List<ServiceInstanceAttribute> l = servInstId.getServiceInstanceAttribute();
        
        for(String a: sii.split("\\.")) {
            String[] sia = a.split("=");
            if(sia.length!=2) {
                throw new IllegalArgumentException("Cannot parse '"+a+"' part of the Service Instance Identifier");
            }
            l.add(getSia(getSiaId(sia[0]), sia[1]));
        }
        return servInstId;
    }
    
   static public BerObjectIdentifier getSiaId(String sia) {
        if ("fsl-fg".equalsIgnoreCase(sia)) {
            return OidValues.fslFg;
        }

        if ("rsl-fg".equalsIgnoreCase(sia)) {
            return OidValues.rslFg;
        }

        for (Field f : OidValues.class.getFields()) {
            try {
                if (f.getName().equals(sia)) {
                    Object o = f.get(OidValues.class);
                    if (o instanceof BerObjectIdentifier) {
                        return (BerObjectIdentifier) o;
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Unknown Service Instance Attribute '"+sia+"'");
    }
    
    private static ServiceInstanceAttribute getSia(BerObjectIdentifier oid, String value) {
        ServiceInstanceAttribute sia = new ServiceInstanceAttribute();
        ServiceInstanceAttribute.SEQUENCE sias = new ServiceInstanceAttribute.SEQUENCE();
        sias.setIdentifier(oid);
        sias.setSiAttributeValue(new BerVisibleString(value));
        sia.getSEQUENCE().add(sias);
        return sia;
    }
    
    
    public static String toString(DiagnosticCltuStart dcs) {
        if (dcs.getCommon() != null) {
            return toString(dcs.getCommon());
        } else  if (dcs.getSpecific() != null) {
            return Constants.getDiagnostic(dcs.getSpecific().intValue(), Constants.CLTU_START_DIAGNOSTICS_SPECIFIC);
        } else {
            return "unknown";
        }
    }
    
    public static String toString(DiagnosticRafStart drs) {
        if (drs.getCommon() != null) {
            return toString(drs.getCommon());
        } else  if (drs.getSpecific() != null) {
            return Constants.getDiagnostic(drs.getSpecific().intValue(), Constants.RAF_START_DIAGNOSTICS_SPECIFIC);
        } else {
            return "unknown";
        }
    }
    
    public static String toString(DiagnosticRcfStart drs) {
        if (drs.getCommon() != null) {
            return toString(drs.getCommon());
        } else  if (drs.getSpecific() != null) {
            return Constants.getDiagnostic(drs.getSpecific().intValue(), Constants.RAF_START_DIAGNOSTICS_SPECIFIC);
        } else {
            return "unknown";
        }
    }
    private static String toString(Diagnostics common) {
        int x = common.intValue();
        if(x == 100) {
            return "duplicateInvokeId";
        } else if (x == 127) {
            return "otherReason";
        } else {
            return "unknown("+x+")";
        }
    }

   
}
