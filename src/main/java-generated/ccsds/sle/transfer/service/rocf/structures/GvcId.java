/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.rocf.structures;

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

public class GvcId implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public static class VcId implements BerType, Serializable {

		private static final long serialVersionUID = 1L;

		public byte[] code = null;
		private BerNull masterChannel = null;
		private ccsds.sle.transfer.service.rocf.structures.VcId virtualChannel = null;
		
		public VcId() {
		}

		public VcId(byte[] code) {
			this.code = code;
		}

		public void setMasterChannel(BerNull masterChannel) {
			this.masterChannel = masterChannel;
		}

		public BerNull getMasterChannel() {
			return masterChannel;
		}

		public void setVirtualChannel(ccsds.sle.transfer.service.rocf.structures.VcId virtualChannel) {
			this.virtualChannel = virtualChannel;
		}

		public ccsds.sle.transfer.service.rocf.structures.VcId getVirtualChannel() {
			return virtualChannel;
		}

		public int encode(OutputStream reverseOS) throws IOException {

			if (code != null) {
				for (int i = code.length - 1; i >= 0; i--) {
					reverseOS.write(code[i]);
				}
				return code.length;
			}

			int codeLength = 0;
			if (virtualChannel != null) {
				codeLength += virtualChannel.encode(reverseOS, false);
				// write tag: CONTEXT_CLASS, PRIMITIVE, 1
				reverseOS.write(0x81);
				codeLength += 1;
				return codeLength;
			}
			
			if (masterChannel != null) {
				codeLength += masterChannel.encode(reverseOS, false);
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
				masterChannel = new BerNull();
				codeLength += masterChannel.decode(is, false);
				return codeLength;
			}

			if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 1)) {
				virtualChannel = new ccsds.sle.transfer.service.rocf.structures.VcId();
				codeLength += virtualChannel.decode(is, false);
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

			if (masterChannel != null) {
				sb.append("masterChannel: ").append(masterChannel);
				return;
			}

			if (virtualChannel != null) {
				sb.append("virtualChannel: ").append(virtualChannel);
				return;
			}

			sb.append("<none>");
		}

	}

	public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);

	public byte[] code = null;
	private BerInteger spacecraftId = null;
	private BerInteger versionNumber = null;
	private VcId vcId = null;
	
	public GvcId() {
	}

	public GvcId(byte[] code) {
		this.code = code;
	}

	public void setSpacecraftId(BerInteger spacecraftId) {
		this.spacecraftId = spacecraftId;
	}

	public BerInteger getSpacecraftId() {
		return spacecraftId;
	}

	public void setVersionNumber(BerInteger versionNumber) {
		this.versionNumber = versionNumber;
	}

	public BerInteger getVersionNumber() {
		return versionNumber;
	}

	public void setVcId(VcId vcId) {
		this.vcId = vcId;
	}

	public VcId getVcId() {
		return vcId;
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
		codeLength += vcId.encode(reverseOS);
		
		codeLength += versionNumber.encode(reverseOS, true);
		
		codeLength += spacecraftId.encode(reverseOS, true);
		
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
		if (berTag.equals(BerInteger.tag)) {
			spacecraftId = new BerInteger();
			subCodeLength += spacecraftId.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(BerInteger.tag)) {
			versionNumber = new BerInteger();
			subCodeLength += versionNumber.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		vcId = new VcId();
		subCodeLength += vcId.decode(is, berTag);
		if (subCodeLength == totalLength) {
			return codeLength;
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
		if (spacecraftId != null) {
			sb.append("spacecraftId: ").append(spacecraftId);
		}
		else {
			sb.append("spacecraftId: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (versionNumber != null) {
			sb.append("versionNumber: ").append(versionNumber);
		}
		else {
			sb.append("versionNumber: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (vcId != null) {
			sb.append("vcId: ");
			vcId.appendAsString(sb, indentLevel + 1);
		}
		else {
			sb.append("vcId: <empty-required-field>");
		}
		
		sb.append("\n");
		for (int i = 0; i < indentLevel; i++) {
			sb.append("\t");
		}
		sb.append("}");
	}

}

