/**
 * This class file was automatically generated by jASN1 v1.11.2 (http://www.beanit.com)
 */

package ccsds.sle.transfer.service.cltu.outgoing.pdus;

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
import ccsds.sle.transfer.service.cltu.structures.BufferSize;
import ccsds.sle.transfer.service.cltu.structures.CltuGetParameter;
import ccsds.sle.transfer.service.cltu.structures.CltuIdentification;
import ccsds.sle.transfer.service.cltu.structures.CltuLastOk;
import ccsds.sle.transfer.service.cltu.structures.CltuLastProcessed;
import ccsds.sle.transfer.service.cltu.structures.CltuNotification;
import ccsds.sle.transfer.service.cltu.structures.CltuStatus;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuGetParameter;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuStart;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuThrowEvent;
import ccsds.sle.transfer.service.cltu.structures.DiagnosticCltuTransferData;
import ccsds.sle.transfer.service.cltu.structures.EventInvocationId;
import ccsds.sle.transfer.service.cltu.structures.NumberOfCltusProcessed;
import ccsds.sle.transfer.service.cltu.structures.NumberOfCltusRadiated;
import ccsds.sle.transfer.service.cltu.structures.NumberOfCltusReceived;
import ccsds.sle.transfer.service.cltu.structures.ProductionStatus;
import ccsds.sle.transfer.service.cltu.structures.UplinkStatus;
import ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.common.types.Time;

public class CltuProviderToUserPdu implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public byte[] code = null;
	private SleBindReturn cltuBindReturn = null;
	private SleUnbindReturn cltuUnbindReturn = null;
	private CltuStartReturn cltuStartReturn = null;
	private SleAcknowledgement cltuStopReturn = null;
	private SleScheduleStatusReportReturn cltuScheduleStatusReportReturn = null;
	private CltuGetParameterReturn cltuGetParameterReturn = null;
	private CltuThrowEventReturn cltuThrowEventReturn = null;
	private CltuTransferDataReturn cltuTransferDataReturn = null;
	private CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation = null;
	private CltuStatusReportInvocation cltuStatusReportInvocation = null;
	private SlePeerAbort cltuPeerAbortInvocation = null;
	
	public CltuProviderToUserPdu() {
	}

	public CltuProviderToUserPdu(byte[] code) {
		this.code = code;
	}

	public void setCltuBindReturn(SleBindReturn cltuBindReturn) {
		this.cltuBindReturn = cltuBindReturn;
	}

	public SleBindReturn getCltuBindReturn() {
		return cltuBindReturn;
	}

	public void setCltuUnbindReturn(SleUnbindReturn cltuUnbindReturn) {
		this.cltuUnbindReturn = cltuUnbindReturn;
	}

	public SleUnbindReturn getCltuUnbindReturn() {
		return cltuUnbindReturn;
	}

	public void setCltuStartReturn(CltuStartReturn cltuStartReturn) {
		this.cltuStartReturn = cltuStartReturn;
	}

	public CltuStartReturn getCltuStartReturn() {
		return cltuStartReturn;
	}

	public void setCltuStopReturn(SleAcknowledgement cltuStopReturn) {
		this.cltuStopReturn = cltuStopReturn;
	}

	public SleAcknowledgement getCltuStopReturn() {
		return cltuStopReturn;
	}

	public void setCltuScheduleStatusReportReturn(SleScheduleStatusReportReturn cltuScheduleStatusReportReturn) {
		this.cltuScheduleStatusReportReturn = cltuScheduleStatusReportReturn;
	}

	public SleScheduleStatusReportReturn getCltuScheduleStatusReportReturn() {
		return cltuScheduleStatusReportReturn;
	}

	public void setCltuGetParameterReturn(CltuGetParameterReturn cltuGetParameterReturn) {
		this.cltuGetParameterReturn = cltuGetParameterReturn;
	}

	public CltuGetParameterReturn getCltuGetParameterReturn() {
		return cltuGetParameterReturn;
	}

	public void setCltuThrowEventReturn(CltuThrowEventReturn cltuThrowEventReturn) {
		this.cltuThrowEventReturn = cltuThrowEventReturn;
	}

	public CltuThrowEventReturn getCltuThrowEventReturn() {
		return cltuThrowEventReturn;
	}

	public void setCltuTransferDataReturn(CltuTransferDataReturn cltuTransferDataReturn) {
		this.cltuTransferDataReturn = cltuTransferDataReturn;
	}

	public CltuTransferDataReturn getCltuTransferDataReturn() {
		return cltuTransferDataReturn;
	}

	public void setCltuAsyncNotifyInvocation(CltuAsyncNotifyInvocation cltuAsyncNotifyInvocation) {
		this.cltuAsyncNotifyInvocation = cltuAsyncNotifyInvocation;
	}

	public CltuAsyncNotifyInvocation getCltuAsyncNotifyInvocation() {
		return cltuAsyncNotifyInvocation;
	}

	public void setCltuStatusReportInvocation(CltuStatusReportInvocation cltuStatusReportInvocation) {
		this.cltuStatusReportInvocation = cltuStatusReportInvocation;
	}

	public CltuStatusReportInvocation getCltuStatusReportInvocation() {
		return cltuStatusReportInvocation;
	}

	public void setCltuPeerAbortInvocation(SlePeerAbort cltuPeerAbortInvocation) {
		this.cltuPeerAbortInvocation = cltuPeerAbortInvocation;
	}

	public SlePeerAbort getCltuPeerAbortInvocation() {
		return cltuPeerAbortInvocation;
	}

	public int encode(OutputStream reverseOS) throws IOException {

		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				reverseOS.write(code[i]);
			}
			return code.length;
		}

		int codeLength = 0;
		if (cltuPeerAbortInvocation != null) {
			codeLength += cltuPeerAbortInvocation.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 104
			reverseOS.write(0x68);
			reverseOS.write(0x9F);
			codeLength += 2;
			return codeLength;
		}
		
		if (cltuStatusReportInvocation != null) {
			codeLength += cltuStatusReportInvocation.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 13
			reverseOS.write(0xAD);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuAsyncNotifyInvocation != null) {
			codeLength += cltuAsyncNotifyInvocation.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 12
			reverseOS.write(0xAC);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuTransferDataReturn != null) {
			codeLength += cltuTransferDataReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 11
			reverseOS.write(0xAB);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuThrowEventReturn != null) {
			codeLength += cltuThrowEventReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 9
			reverseOS.write(0xA9);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuGetParameterReturn != null) {
			codeLength += cltuGetParameterReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 7
			reverseOS.write(0xA7);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuScheduleStatusReportReturn != null) {
			codeLength += cltuScheduleStatusReportReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 5
			reverseOS.write(0xA5);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuStopReturn != null) {
			codeLength += cltuStopReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 3
			reverseOS.write(0xA3);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuStartReturn != null) {
			codeLength += cltuStartReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 1
			reverseOS.write(0xA1);
			codeLength += 1;
			return codeLength;
		}
		
		if (cltuUnbindReturn != null) {
			codeLength += cltuUnbindReturn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 103
			reverseOS.write(0x67);
			reverseOS.write(0xBF);
			codeLength += 2;
			return codeLength;
		}
		
		if (cltuBindReturn != null) {
			codeLength += cltuBindReturn.encode(reverseOS, false);
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
			cltuBindReturn = new SleBindReturn();
			codeLength += cltuBindReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 103)) {
			cltuUnbindReturn = new SleUnbindReturn();
			codeLength += cltuUnbindReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 1)) {
			cltuStartReturn = new CltuStartReturn();
			codeLength += cltuStartReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 3)) {
			cltuStopReturn = new SleAcknowledgement();
			codeLength += cltuStopReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 5)) {
			cltuScheduleStatusReportReturn = new SleScheduleStatusReportReturn();
			codeLength += cltuScheduleStatusReportReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 7)) {
			cltuGetParameterReturn = new CltuGetParameterReturn();
			codeLength += cltuGetParameterReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 9)) {
			cltuThrowEventReturn = new CltuThrowEventReturn();
			codeLength += cltuThrowEventReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 11)) {
			cltuTransferDataReturn = new CltuTransferDataReturn();
			codeLength += cltuTransferDataReturn.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 12)) {
			cltuAsyncNotifyInvocation = new CltuAsyncNotifyInvocation();
			codeLength += cltuAsyncNotifyInvocation.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 13)) {
			cltuStatusReportInvocation = new CltuStatusReportInvocation();
			codeLength += cltuStatusReportInvocation.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 104)) {
			cltuPeerAbortInvocation = new SlePeerAbort();
			codeLength += cltuPeerAbortInvocation.decode(is, false);
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

		if (cltuBindReturn != null) {
			sb.append("cltuBindReturn: ");
			cltuBindReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuUnbindReturn != null) {
			sb.append("cltuUnbindReturn: ");
			cltuUnbindReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuStartReturn != null) {
			sb.append("cltuStartReturn: ");
			cltuStartReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuStopReturn != null) {
			sb.append("cltuStopReturn: ");
			cltuStopReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuScheduleStatusReportReturn != null) {
			sb.append("cltuScheduleStatusReportReturn: ");
			cltuScheduleStatusReportReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuGetParameterReturn != null) {
			sb.append("cltuGetParameterReturn: ");
			cltuGetParameterReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuThrowEventReturn != null) {
			sb.append("cltuThrowEventReturn: ");
			cltuThrowEventReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuTransferDataReturn != null) {
			sb.append("cltuTransferDataReturn: ");
			cltuTransferDataReturn.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuAsyncNotifyInvocation != null) {
			sb.append("cltuAsyncNotifyInvocation: ");
			cltuAsyncNotifyInvocation.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuStatusReportInvocation != null) {
			sb.append("cltuStatusReportInvocation: ");
			cltuStatusReportInvocation.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (cltuPeerAbortInvocation != null) {
			sb.append("cltuPeerAbortInvocation: ").append(cltuPeerAbortInvocation);
			return;
		}

		sb.append("<none>");
	}

}

