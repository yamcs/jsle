/**
 * This class file was automatically generated by jASN1 v1.11.2 (http://www.beanit.com)
 */

package ccsds.sle.transfer.service.fsp.outgoing.pdus;

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
import com.beanit.jasn1.ber.*;
import com.beanit.jasn1.ber.types.*;
import com.beanit.jasn1.ber.types.string.*;

import ccsds.sle.transfer.service.bind.types.SleBindReturn;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.common.types.Time;
import ccsds.sle.transfer.service.fsp.structures.BufferSize;
import ccsds.sle.transfer.service.fsp.structures.DiagnosticFspGet;
import ccsds.sle.transfer.service.fsp.structures.DiagnosticFspInvokeDirective;
import ccsds.sle.transfer.service.fsp.structures.DiagnosticFspStart;
import ccsds.sle.transfer.service.fsp.structures.DiagnosticFspThrowEvent;
import ccsds.sle.transfer.service.fsp.structures.DiagnosticFspTransferData;
import ccsds.sle.transfer.service.fsp.structures.FspGetParameter;
import ccsds.sle.transfer.service.fsp.structures.FspNotification;
import ccsds.sle.transfer.service.fsp.structures.FspPacketCount;
import ccsds.sle.transfer.service.fsp.structures.FspPacketLastOk;
import ccsds.sle.transfer.service.fsp.structures.FspPacketLastProcessed;
import ccsds.sle.transfer.service.fsp.structures.FspProductionStatus;
import ccsds.sle.transfer.service.fsp.structures.PacketIdentification;

public class FspProviderToUserPdu implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public byte[] code = null;
	private SleBindReturn fspBindReturn = null;
	private SleUnbindReturn fspUnbindReturn = null;
	private FspStartReturn fspStartReturn = null;
	private SleAcknowledgement fspStopReturn = null;
	private FspTransferDataReturn fspTransferDataReturn = null;
	private FspAsyncNotifyInvocation fspAsyncNotifyInvocation = null;
	private SleScheduleStatusReportReturn fspScheduleStatusReportReturn = null;
	private FspStatusReportInvocation fspStatusReportInvocation = null;
	private FspGetParameterReturn fspGetParameterReturn = null;
	private FspThrowEventReturn fspThrowEventReturn = null;
	private FspInvokeDirectiveReturn fspInvokeDirectiveReturn = null;
	private SlePeerAbort fspPeerAbortInvocation = null;
	
	public FspProviderToUserPdu() {
	}

	public FspProviderToUserPdu(byte[] code) {
		this.code = code;
	}

	public void setFspBindReturn(SleBindReturn fspBindReturn) {
		this.fspBindReturn = fspBindReturn;
	}

	public SleBindReturn getFspBindReturn() {
		return fspBindReturn;
	}

	public void setFspUnbindReturn(SleUnbindReturn fspUnbindReturn) {
		this.fspUnbindReturn = fspUnbindReturn;
	}

	public SleUnbindReturn getFspUnbindReturn() {
		return fspUnbindReturn;
	}

	public void setFspStartReturn(FspStartReturn fspStartReturn) {
		this.fspStartReturn = fspStartReturn;
	}

	public FspStartReturn getFspStartReturn() {
		return fspStartReturn;
	}

	public void setFspStopReturn(SleAcknowledgement fspStopReturn) {
		this.fspStopReturn = fspStopReturn;
	}

	public SleAcknowledgement getFspStopReturn() {
		return fspStopReturn;
	}

	public void setFspTransferDataReturn(FspTransferDataReturn fspTransferDataReturn) {
		this.fspTransferDataReturn = fspTransferDataReturn;
	}

	public FspTransferDataReturn getFspTransferDataReturn() {
		return fspTransferDataReturn;
	}

	public void setFspAsyncNotifyInvocation(FspAsyncNotifyInvocation fspAsyncNotifyInvocation) {
		this.fspAsyncNotifyInvocation = fspAsyncNotifyInvocation;
	}

	public FspAsyncNotifyInvocation getFspAsyncNotifyInvocation() {
		return fspAsyncNotifyInvocation;
	}

	public void setFspScheduleStatusReportReturn(SleScheduleStatusReportReturn fspScheduleStatusReportReturn) {
		this.fspScheduleStatusReportReturn = fspScheduleStatusReportReturn;
	}

	public SleScheduleStatusReportReturn getFspScheduleStatusReportReturn() {
		return fspScheduleStatusReportReturn;
	}

	public void setFspStatusReportInvocation(FspStatusReportInvocation fspStatusReportInvocation) {
		this.fspStatusReportInvocation = fspStatusReportInvocation;
	}

	public FspStatusReportInvocation getFspStatusReportInvocation() {
		return fspStatusReportInvocation;
	}

	public void setFspGetParameterReturn(FspGetParameterReturn fspGetParameterReturn) {
		this.fspGetParameterReturn = fspGetParameterReturn;
	}

	public FspGetParameterReturn getFspGetParameterReturn() {
		return fspGetParameterReturn;
	}

	public void setFspThrowEventReturn(FspThrowEventReturn fspThrowEventReturn) {
		this.fspThrowEventReturn = fspThrowEventReturn;
	}

	public FspThrowEventReturn getFspThrowEventReturn() {
		return fspThrowEventReturn;
	}

	public void setFspInvokeDirectiveReturn(FspInvokeDirectiveReturn fspInvokeDirectiveReturn) {
		this.fspInvokeDirectiveReturn = fspInvokeDirectiveReturn;
	}

	public FspInvokeDirectiveReturn getFspInvokeDirectiveReturn() {
		return fspInvokeDirectiveReturn;
	}

	public void setFspPeerAbortInvocation(SlePeerAbort fspPeerAbortInvocation) {
		this.fspPeerAbortInvocation = fspPeerAbortInvocation;
	}

	public SlePeerAbort getFspPeerAbortInvocation() {
		return fspPeerAbortInvocation;
	}

	public int encode(OutputStream reverseOS) throws IOException {

		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				reverseOS.write(code[i]);
			}
			return code.length;
		}

		int codeLength = 0;
		if (fspPeerAbortInvocation != null) {
			codeLength += fspPeerAbortInvocation.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 104
			reverseOS.write(0x68);
			reverseOS.write(0x9F);
			codeLength += 2;
			return codeLength;
		}
		
		if (fspInvokeDirectiveReturn != null) {
			codeLength += fspInvokeDirectiveReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 15
			reverseOS.write(0xAF);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspThrowEventReturn != null) {
			codeLength += fspThrowEventReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 9
			reverseOS.write(0xA9);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspGetParameterReturn != null) {
			codeLength += fspGetParameterReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 7
			reverseOS.write(0xA7);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspStatusReportInvocation != null) {
			codeLength += fspStatusReportInvocation.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 13
			reverseOS.write(0xAD);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspScheduleStatusReportReturn != null) {
			codeLength += fspScheduleStatusReportReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 5
			reverseOS.write(0xA5);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspAsyncNotifyInvocation != null) {
			codeLength += fspAsyncNotifyInvocation.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 12
			reverseOS.write(0xAC);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspTransferDataReturn != null) {
			codeLength += fspTransferDataReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 11
			reverseOS.write(0xAB);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspStopReturn != null) {
			codeLength += fspStopReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 3
			reverseOS.write(0xA3);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspStartReturn != null) {
			codeLength += fspStartReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 1
			reverseOS.write(0xA1);
			codeLength += 1;
			return codeLength;
		}
		
		if (fspUnbindReturn != null) {
			codeLength += fspUnbindReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 103
			reverseOS.write(0x67);
			reverseOS.write(0xBF);
			codeLength += 2;
			return codeLength;
		}
		
		if (fspBindReturn != null) {
			codeLength += fspBindReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 101
			reverseOS.write(0x65);
			reverseOS.write(0xBF);
			codeLength += 2;
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

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 101)) {
			fspBindReturn = new SleBindReturn();
			codeLength += fspBindReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 103)) {
			fspUnbindReturn = new SleUnbindReturn();
			codeLength += fspUnbindReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
			fspStartReturn = new FspStartReturn();
			codeLength += fspStartReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 3)) {
			fspStopReturn = new SleAcknowledgement();
			codeLength += fspStopReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 11)) {
			fspTransferDataReturn = new FspTransferDataReturn();
			codeLength += fspTransferDataReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 12)) {
			fspAsyncNotifyInvocation = new FspAsyncNotifyInvocation();
			codeLength += fspAsyncNotifyInvocation.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 5)) {
			fspScheduleStatusReportReturn = new SleScheduleStatusReportReturn();
			codeLength += fspScheduleStatusReportReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 13)) {
			fspStatusReportInvocation = new FspStatusReportInvocation();
			codeLength += fspStatusReportInvocation.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
			fspGetParameterReturn = new FspGetParameterReturn();
			codeLength += fspGetParameterReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 9)) {
			fspThrowEventReturn = new FspThrowEventReturn();
			codeLength += fspThrowEventReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 15)) {
			fspInvokeDirectiveReturn = new FspInvokeDirectiveReturn();
			codeLength += fspInvokeDirectiveReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 104)) {
			fspPeerAbortInvocation = new SlePeerAbort();
			codeLength += fspPeerAbortInvocation.decode(is, false);
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

		if (fspBindReturn != null) {
			sb.append("fspBindReturn: ");
			fspBindReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspUnbindReturn != null) {
			sb.append("fspUnbindReturn: ");
			fspUnbindReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspStartReturn != null) {
			sb.append("fspStartReturn: ");
			fspStartReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspStopReturn != null) {
			sb.append("fspStopReturn: ");
			fspStopReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspTransferDataReturn != null) {
			sb.append("fspTransferDataReturn: ");
			fspTransferDataReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspAsyncNotifyInvocation != null) {
			sb.append("fspAsyncNotifyInvocation: ");
			fspAsyncNotifyInvocation.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspScheduleStatusReportReturn != null) {
			sb.append("fspScheduleStatusReportReturn: ");
			fspScheduleStatusReportReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspStatusReportInvocation != null) {
			sb.append("fspStatusReportInvocation: ");
			fspStatusReportInvocation.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspGetParameterReturn != null) {
			sb.append("fspGetParameterReturn: ");
			fspGetParameterReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspThrowEventReturn != null) {
			sb.append("fspThrowEventReturn: ");
			fspThrowEventReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspInvokeDirectiveReturn != null) {
			sb.append("fspInvokeDirectiveReturn: ");
			fspInvokeDirectiveReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (fspPeerAbortInvocation != null) {
			sb.append("fspPeerAbortInvocation: ").append(fspPeerAbortInvocation);
			return;
		}

		sb.append("<none>");
	}

}

