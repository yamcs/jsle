/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.rcf.structures;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.io.Serializable;
import org.openmuc.jasn1.ber.*;
import org.openmuc.jasn1.ber.types.*;
import org.openmuc.jasn1.ber.types.string.*;

import ccsds.sle.transfer.service.common.pdus.ReportingCycle;
import ccsds.sle.transfer.service.common.types.DeliveryMode;
import ccsds.sle.transfer.service.common.types.Diagnostics;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.ParameterName;
import ccsds.sle.transfer.service.common.types.Time;

public class CarrierLockStatus extends LockStatus {

	private static final long serialVersionUID = 1L;

	public CarrierLockStatus() {
	}

	public CarrierLockStatus(byte[] code) {
		super(code);
	}

	public CarrierLockStatus(BigInteger value) {
		super(value);
	}

	public CarrierLockStatus(long value) {
		super(value);
	}

}
