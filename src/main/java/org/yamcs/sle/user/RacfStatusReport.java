package org.yamcs.sle.user;

import org.yamcs.sle.Constants.LockStatus;
import org.yamcs.sle.Constants.ProductionStatus;

import ccsds.sle.transfer.service.raf.outgoing.pdus.RafStatusReportInvocation;
import ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfStatusReportInvocation;

/**
 * Status report for RAF and RCF services.
 * <p>
 * The difference between them is that the deliveredFrameNumber=errorFreeFrameNumber for RCF whereas they may differ
 * fom RAF.
 * 
 * @author nm
 *
 */
public class RacfStatusReport {
    private int errorFreeFrameNumber;
    private int deliveredFrameNumber;
    private LockStatus frameSyncLockStatus;
    private LockStatus symbolSyncLockStatus;
    private LockStatus subcarrierLockStatus;
    private LockStatus carrierLockStatus;
    private ProductionStatus productionStatus;

    public RacfStatusReport(RafStatusReportInvocation rssi) {
        this.deliveredFrameNumber = rssi.getDeliveredFrameNumber().intValue();
        this.errorFreeFrameNumber = rssi.getErrorFreeFrameNumber().intValue();
        this.frameSyncLockStatus = LockStatus.byId(rssi.getFrameSyncLockStatus().intValue());
        this.symbolSyncLockStatus = LockStatus.byId(rssi.getSymbolSyncLockStatus().intValue());
        this.subcarrierLockStatus = LockStatus.byId(rssi.getSubcarrierLockStatus().intValue());
        this.carrierLockStatus = LockStatus.byId(rssi.getCarrierLockStatus().intValue());
        this.productionStatus = ProductionStatus.byId(rssi.getProductionStatus().intValue());
    }
    
    public RacfStatusReport(RcfStatusReportInvocation rssi) {
        this.deliveredFrameNumber = rssi.getDeliveredFrameNumber().intValue();
        this.errorFreeFrameNumber = deliveredFrameNumber;
        this.frameSyncLockStatus = LockStatus.byId(rssi.getFrameSyncLockStatus().intValue());
        this.symbolSyncLockStatus = LockStatus.byId(rssi.getSymbolSyncLockStatus().intValue());
        this.subcarrierLockStatus = LockStatus.byId(rssi.getSubcarrierLockStatus().intValue());
        this.carrierLockStatus = LockStatus.byId(rssi.getCarrierLockStatus().intValue());
        this.productionStatus = ProductionStatus.byId(rssi.getProductionStatus().intValue());
    }
    public LockStatus getFrameSyncLockStatus() {
        return frameSyncLockStatus;
    }

    public void setFrameSyncLockStatus(LockStatus frameSyncLockStatus) {
        this.frameSyncLockStatus = frameSyncLockStatus;
    }

    public LockStatus getSymbolSyncLockStatus() {
        return symbolSyncLockStatus;
    }

    public void setSymbolSyncLockStatus(LockStatus symbolSyncLockStatus) {
        this.symbolSyncLockStatus = symbolSyncLockStatus;
    }

    public LockStatus getSubcarrierLockStatus() {
        return subcarrierLockStatus;
    }

    public void setSubcarrierLockStatus(LockStatus subcarrierLockStatus) {
        this.subcarrierLockStatus = subcarrierLockStatus;
    }

    public LockStatus getCarrierLockStatus() {
        return carrierLockStatus;
    }

    public void setCarrierLockStatus(LockStatus carrierLockStatus) {
        this.carrierLockStatus = carrierLockStatus;
    }

    public ProductionStatus getProductionStatus() {
        return productionStatus;
    }

    public void setProductionStatus(ProductionStatus productionStatus) {
        this.productionStatus = productionStatus;
    }

    public int getErrorFreeFrameNumber() {
        return errorFreeFrameNumber;
    }

    public void setErrorFreeFrameNumber(int errorFreeFrameNumber) {
        this.errorFreeFrameNumber = errorFreeFrameNumber;
    }

    public int getDeliveredFrameNumber() {
        return deliveredFrameNumber;
    }

    public void setDeliveredFrameNumber(int deliveredFrameNumber) {
        this.deliveredFrameNumber = deliveredFrameNumber;
    }

}
