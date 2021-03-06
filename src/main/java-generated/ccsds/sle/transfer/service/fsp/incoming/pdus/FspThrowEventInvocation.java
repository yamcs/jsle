/**
 * This class file was automatically generated by jASN1 v1.11.2 (http://www.beanit.com)
 */

package ccsds.sle.transfer.service.fsp.incoming.pdus;

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

import ccsds.sle.transfer.service.bind.types.SleBindInvocation;
import ccsds.sle.transfer.service.bind.types.SlePeerAbort;
import ccsds.sle.transfer.service.bind.types.SleUnbindInvocation;
import ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportInvocation;
import ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.Duration;
import ccsds.sle.transfer.service.common.types.IntPosLong;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.InvokeId;
import ccsds.sle.transfer.service.common.types.SlduStatusNotification;
import ccsds.sle.transfer.service.fsp.structures.BlockingUsage;
import ccsds.sle.transfer.service.fsp.structures.FspData;
import ccsds.sle.transfer.service.fsp.structures.FspParameterName;
import ccsds.sle.transfer.service.fsp.structures.Map;
import ccsds.sle.transfer.service.fsp.structures.MapMuxControl;
import ccsds.sle.transfer.service.fsp.structures.PacketIdentification;
import ccsds.sle.transfer.service.fsp.structures.ProductionTime;
import ccsds.sle.transfer.service.fsp.structures.TransmissionMode;

public class FspThrowEventInvocation implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);

	public byte[] code = null;
	private Credentials invokerCredentials = null;
	private InvokeId invokeId = null;
	private IntPosShort eventIdentifier = null;
	private BerOctetString eventQualifier = null;
	private IntUnsignedLong eventInvocationIdentification = null;
	
	public FspThrowEventInvocation() {
	}

	public FspThrowEventInvocation(byte[] code) {
		this.code = code;
	}

	public void setInvokerCredentials(Credentials invokerCredentials) {
		this.invokerCredentials = invokerCredentials;
	}

	public Credentials getInvokerCredentials() {
		return invokerCredentials;
	}

	public void setInvokeId(InvokeId invokeId) {
		this.invokeId = invokeId;
	}

	public InvokeId getInvokeId() {
		return invokeId;
	}

	public void setEventIdentifier(IntPosShort eventIdentifier) {
		this.eventIdentifier = eventIdentifier;
	}

	public IntPosShort getEventIdentifier() {
		return eventIdentifier;
	}

	public void setEventQualifier(BerOctetString eventQualifier) {
		this.eventQualifier = eventQualifier;
	}

	public BerOctetString getEventQualifier() {
		return eventQualifier;
	}

	public void setEventInvocationIdentification(IntUnsignedLong eventInvocationIdentification) {
		this.eventInvocationIdentification = eventInvocationIdentification;
	}

	public IntUnsignedLong getEventInvocationIdentification() {
		return eventInvocationIdentification;
	}

	public int encode(OutputStream reverseOS) throws IOException {
		return encode(reverseOS, true);
	}

	public int encode(OutputStream reverseOS, boolean withTag) throws IOException {

		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				reverseOS.write(code[i]);
			}
			if (withTag) {
				return tag.encode(reverseOS) + code.length;
			}
			return code.length;
		}

		int codeLength = 0;
		codeLength += eventInvocationIdentification.encode(reverseOS, true);
		
		codeLength += eventQualifier.encode(reverseOS, true);
		
		codeLength += eventIdentifier.encode(reverseOS, true);
		
		codeLength += invokeId.encode(reverseOS, true);
		
		codeLength += invokerCredentials.encode(reverseOS);
		
		codeLength += BerLength.encodeLength(reverseOS, codeLength);

		if (withTag) {
			codeLength += tag.encode(reverseOS);
		}

		return codeLength;

	}

	public int decode(InputStream is) throws IOException {
		return decode(is, true);
	}

	public int decode(InputStream is, boolean withTag) throws IOException {
		int codeLength = 0;
		int subCodeLength = 0;
		BerTag berTag = new BerTag();

		if (withTag) {
			codeLength += tag.decodeAndCheck(is);
		}

		BerLength length = new BerLength();
		codeLength += length.decode(is);

		int totalLength = length.val;
		codeLength += totalLength;

		subCodeLength += berTag.decode(is);
		invokerCredentials = new Credentials();
		subCodeLength += invokerCredentials.decode(is, berTag);
		subCodeLength += berTag.decode(is);
		
		if (berTag.equals(InvokeId.tag)) {
			invokeId = new InvokeId();
			subCodeLength += invokeId.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(IntPosShort.tag)) {
			eventIdentifier = new IntPosShort();
			subCodeLength += eventIdentifier.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(BerOctetString.tag)) {
			eventQualifier = new BerOctetString();
			subCodeLength += eventQualifier.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(IntUnsignedLong.tag)) {
			eventInvocationIdentification = new IntUnsignedLong();
			subCodeLength += eventInvocationIdentification.decode(is, false);
			if (subCodeLength == totalLength) {
				return codeLength;
			}
		}
		throw new IOException("Unexpected end of sequence, length tag: " + totalLength + ", actual sequence length: " + subCodeLength);

		
	}

	public void encodeAndSave(int encodingSizeGuess) throws IOException {
		ReverseByteArrayOutputStream reverseOS = new ReverseByteArrayOutputStream(encodingSizeGuess);
		encode(reverseOS, false);
		code = reverseOS.getArray();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendAsString(sb, 0);
		return sb.toString();
	}

	public void appendAsString(StringBuilder sb, int indentLevel) {

		sb.append("{");
		sb.append("\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (invokerCredentials != null) {
			sb.append("invokerCredentials: ");
			invokerCredentials.appendAsString(sb, indentLevel + 1);
		}
		else {
			sb.append("invokerCredentials: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (invokeId != null) {
			sb.append("invokeId: ").append(invokeId);
		}
		else {
			sb.append("invokeId: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (eventIdentifier != null) {
			sb.append("eventIdentifier: ").append(eventIdentifier);
		}
		else {
			sb.append("eventIdentifier: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (eventQualifier != null) {
			sb.append("eventQualifier: ").append(eventQualifier);
		}
		else {
			sb.append("eventQualifier: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (eventInvocationIdentification != null) {
			sb.append("eventInvocationIdentification: ").append(eventInvocationIdentification);
		}
		else {
			sb.append("eventInvocationIdentification: <empty-required-field>");
		}
		
		sb.append("\n");
		for (int i = 0; i < indentLevel; i++) {
			sb.append("\t");
		}
		sb.append("}");
	}

}

