package org.yamcs.sle;

/**
 * RAF and RCF antennaId 
 * 
 * @author nm
 *
 */
public class AntennaId {
    
    private byte[] local;
    private int[] global;

    public AntennaId(ccsds.sle.transfer.service.raf.structures.AntennaId antennaId) {
        if(antennaId.getLocalForm()!=null) {
            this.local = antennaId.getLocalForm().value;
        }
        if(antennaId.getGlobalForm()!=null) {
            this.global = antennaId.getGlobalForm().value;
        }
    }

    public AntennaId(ccsds.sle.transfer.service.rcf.structures.AntennaId antennaId) {
        if(antennaId.getLocalForm()!=null) {
            this.local = antennaId.getLocalForm().value;
        }
        if(antennaId.getGlobalForm()!=null) {
            this.global = antennaId.getGlobalForm().value;
        }
    }
    
    
    public byte[] getLocal() {
        return local;
    }

    public void setLocal(byte[] local) {
        this.local = local;
    }

    public int[] getGlobal() {
        return global;
    }

    public void setGlobal(int[] global) {
        this.global = global;
    }
}
