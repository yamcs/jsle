package org.yamcs.sle;

import static org.yamcs.sle.Constants.BER_NULL;

import com.beanit.jasn1.ber.types.BerInteger;

/**
 * Global Virtual Channel Identifier
 * <p>
 * 
 * Used by the RCF service to specify which channel to subscribe to. 
 * @author nm
 *
 */
public class GVCID {

    final int transferFrameVersionNumber;
    final int spacecraftId;
    final int vcId;
    
    public GVCID(int transferFrameVersionNumber, int spacecraftId, int vcId) {
        super();
        this.transferFrameVersionNumber = transferFrameVersionNumber;
        this.spacecraftId = spacecraftId;
        this.vcId = vcId;
    }
    public int getTransferFrameVersionNumber() {
        return transferFrameVersionNumber;
    }

    public int getSpacecraftId() {
        return spacecraftId;
    }

    public int getVcId() {
        return vcId;
    }

    public ccsds.sle.transfer.service.rcf.structures.GvcId toRcf() {
        ccsds.sle.transfer.service.rcf.structures.GvcId rcfgvid = new ccsds.sle.transfer.service.rcf.structures.GvcId();
        rcfgvid.setSpacecraftId(new BerInteger(spacecraftId));
        rcfgvid.setVersionNumber(new BerInteger(transferFrameVersionNumber));
        ccsds.sle.transfer.service.rcf.structures.GvcId.VcId v = new ccsds.sle.transfer.service.rcf.structures.GvcId.VcId();
        if(vcId==-1) {
            v.setMasterChannel(BER_NULL);
        } else {
            v.setVirtualChannel(new ccsds.sle.transfer.service.rcf.structures.VcId(vcId));
        }
        rcfgvid.setVcId(v);
        return rcfgvid;
    }
}
