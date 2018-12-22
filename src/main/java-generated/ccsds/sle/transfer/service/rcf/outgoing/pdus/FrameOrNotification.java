/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.rcf.outgoing.pdus;

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

import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.SleBindReturn;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindInvocation;
import ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.common.types.SpaceLinkDataUnit;
import ccsds.sle.transfer.service.common.types.Time;
import ccsds.sle.transfer.service.rcf.structures.AntennaId;
import ccsds.sle.transfer.service.rcf.structures.CarrierLockStatus;
import ccsds.sle.transfer.service.rcf.structures.DiagnosticRcfGet;
import ccsds.sle.transfer.service.rcf.structures.DiagnosticRcfStart;
import ccsds.sle.transfer.service.rcf.structures.FrameSyncLockStatus;
import ccsds.sle.transfer.service.rcf.structures.LockStatus;
import ccsds.sle.transfer.service.rcf.structures.Notification;
import ccsds.sle.transfer.service.rcf.structures.RcfGetParameter;
import ccsds.sle.transfer.service.rcf.structures.RcfProductionStatus;
import ccsds.sle.transfer.service.rcf.structures.SymbolLockStatus;

public class FrameOrNotification implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public byte[] code = null;
	private RcfTransferDataInvocation annotatedFrame = null;
	private RcfSyncNotifyInvocation syncNotification = null;
	
	public FrameOrNotification() {
	}

	public FrameOrNotification(byte[] code) {
		this.code = code;
	}

	public void setAnnotatedFrame(RcfTransferDataInvocation annotatedFrame) {
		this.annotatedFrame = annotatedFrame;
	}

	public RcfTransferDataInvocation getAnnotatedFrame() {
		return annotatedFrame;
	}

	public void setSyncNotification(RcfSyncNotifyInvocation syncNotification) {
		this.syncNotification = syncNotification;
	}

	public RcfSyncNotifyInvocation getSyncNotification() {
		return syncNotification;
	}

	public int encode(OutputStream reverseOS) throws IOException {

		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				reverseOS.write(code[i]);
			}
			return code.length;
		}

		int codeLength = 0;
		if (syncNotification != null) {
			codeLength += syncNotification.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 1
			reverseOS.write(0xA1);
			codeLength += 1;
			return codeLength;
		}
		
		if (annotatedFrame != null) {
			codeLength += annotatedFrame.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 0
			reverseOS.write(0xA0);
			codeLength += 1;
			return codeLength;
		}
		
		throw new IOException("Error encoding CHOICE: No element of CHOICE was selected.");
	}

	public int decode(InputStream is) throws IOException {
		return decode(is, null);
	}

	public int decode(InputStream is, BerTag berTag) throws IOException {

		int codeLength = 0;
		BerTag passedTag = berTag;

		if (berTag == null) {
			berTag = new BerTag();
			codeLength += berTag.decode(is);
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0)) {
			annotatedFrame = new RcfTransferDataInvocation();
			codeLength += annotatedFrame.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
			syncNotification = new RcfSyncNotifyInvocation();
			codeLength += syncNotification.decode(is, false);
			return codeLength;
		}

		if (passedTag != null) {
			return 0;
		}

		throw new IOException("Error decoding CHOICE: Tag " + berTag + " matched to no item.");
	}

	public void encodeAndSave(int encodingSizeGuess) throws IOException {
		ReverseByteArrayOutputStream reverseOS = new ReverseByteArrayOutputStream(encodingSizeGuess);
		encode(reverseOS);
		code = reverseOS.getArray();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendAsString(sb, 0);
		return sb.toString();
	}

	public void appendAsString(StringBuilder sb, int indentLevel) {

		if (annotatedFrame != null) {
			sb.append("annotatedFrame: ");
			annotatedFrame.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (syncNotification != null) {
			sb.append("syncNotification: ");
			syncNotification.appendAsString(sb, indentLevel + 1);
			return;
		}

		sb.append("<none>");
	}

}

