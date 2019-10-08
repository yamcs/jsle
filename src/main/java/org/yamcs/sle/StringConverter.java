package org.yamcs.sle;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.beanit.jasn1.ber.types.BerObjectIdentifier;

import ccsds.sle.transfer.service.service.instance.id.OidValues;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceAttribute;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;

public class StringConverter {
    public static String toString(ServiceInstanceIdentifier sii) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ServiceInstanceAttribute sia : sii.getServiceInstanceAttribute()) {
            if(first) {
                first= false;
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
            } catch (Exception e) {
                continue;
            }
        }
        return "unknown" + Arrays.toString(boi.value);
    }

}
