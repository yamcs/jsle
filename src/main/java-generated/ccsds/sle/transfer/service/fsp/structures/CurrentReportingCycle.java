/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.fsp.structures;

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
import ccsds.sle.transfer.service.common.types.ForwardDuStatus;
import ccsds.sle.transfer.service.common.types.IntPosLong;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.ParameterName;
import ccsds.sle.transfer.service.common.types.SpaceLinkDataUnit;
import ccsds.sle.transfer.service.common.types.Time;

public class CurrentReportingCycle implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public byte[] code = null;
	private BerNull cyclicReportOff = null;
	private ReportingCycle cyclicReportOn = null;
	
	public CurrentReportingCycle() {
	}

	public CurrentReportingCycle(byte[] code) {
		this.code = code;
	}

	public void setCyclicReportOff(BerNull cyclicReportOff) {
		this.cyclicReportOff = cyclicReportOff;
	}

	public BerNull getCyclicReportOff() {
		return cyclicReportOff;
	}

	public void setCyclicReportOn(ReportingCycle cyclicReportOn) {
		this.cyclicReportOn = cyclicReportOn;
	}

	public ReportingCycle getCyclicReportOn() {
		return cyclicReportOn;
	}

	public int encode(OutputStream reverseOS) throws IOException {

		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				reverseOS.write(code[i]);
			}
			return code.length;
		}

		int codeLength = 0;
		if (cyclicReportOn != null) {
			codeLength += cyclicReportOn.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 1
			reverseOS.write(0x81);
			codeLength += 1;
			return codeLength;
		}
		
		if (cyclicReportOff != null) {
			codeLength += cyclicReportOff.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 0
			reverseOS.write(0x80);
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

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 0)) {
			cyclicReportOff = new BerNull();
			codeLength += cyclicReportOff.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 1)) {
			cyclicReportOn = new ReportingCycle();
			codeLength += cyclicReportOn.decode(is, false);
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

		if (cyclicReportOff != null) {
			sb.append("cyclicReportOff: ").append(cyclicReportOff);
			return;
		}

		if (cyclicReportOn != null) {
			sb.append("cyclicReportOn: ").append(cyclicReportOn);
			return;
		}

		sb.append("<none>");
	}

}

