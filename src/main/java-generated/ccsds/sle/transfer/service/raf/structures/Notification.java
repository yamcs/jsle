/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.raf.structures;

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
import ccsds.sle.transfer.service.common.types.IntPosLong;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.ParameterName;
import ccsds.sle.transfer.service.common.types.Time;

public class Notification implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public byte[] code = null;
	private LockStatusReport lossFrameSync = null;
	private RafProductionStatus productionStatusChange = null;
	private BerNull excessiveDataBacklog = null;
	private BerNull endOfData = null;
	
	public Notification() {
	}

	public Notification(byte[] code) {
		this.code = code;
	}

	public void setLossFrameSync(LockStatusReport lossFrameSync) {
		this.lossFrameSync = lossFrameSync;
	}

	public LockStatusReport getLossFrameSync() {
		return lossFrameSync;
	}

	public void setProductionStatusChange(RafProductionStatus productionStatusChange) {
		this.productionStatusChange = productionStatusChange;
	}

	public RafProductionStatus getProductionStatusChange() {
		return productionStatusChange;
	}

	public void setExcessiveDataBacklog(BerNull excessiveDataBacklog) {
		this.excessiveDataBacklog = excessiveDataBacklog;
	}

	public BerNull getExcessiveDataBacklog() {
		return excessiveDataBacklog;
	}

	public void setEndOfData(BerNull endOfData) {
		this.endOfData = endOfData;
	}

	public BerNull getEndOfData() {
		return endOfData;
	}

	public int encode(OutputStream reverseOS) throws IOException {

		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				reverseOS.write(code[i]);
			}
			return code.length;
		}

		int codeLength = 0;
		if (endOfData != null) {
			codeLength += endOfData.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 3
			reverseOS.write(0x83);
			codeLength += 1;
			return codeLength;
		}
		
		if (excessiveDataBacklog != null) {
			codeLength += excessiveDataBacklog.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 2
			reverseOS.write(0x82);
			codeLength += 1;
			return codeLength;
		}
		
		if (productionStatusChange != null) {
			codeLength += productionStatusChange.encode(reverseOS, false);
			// write tag: CONTEXT_CLASS, PRIMITIVE, 1
			reverseOS.write(0x81);
			codeLength += 1;
			return codeLength;
		}
		
		if (lossFrameSync != null) {
			codeLength += lossFrameSync.encode(reverseOS, false);
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
			lossFrameSync = new LockStatusReport();
			codeLength += lossFrameSync.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 1)) {
			productionStatusChange = new RafProductionStatus();
			codeLength += productionStatusChange.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 2)) {
			excessiveDataBacklog = new BerNull();
			codeLength += excessiveDataBacklog.decode(is, false);
			return codeLength;
		}

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 3)) {
			endOfData = new BerNull();
			codeLength += endOfData.decode(is, false);
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

		if (lossFrameSync != null) {
			sb.append("lossFrameSync: ");
			lossFrameSync.appendAsString(sb, indentLevel + 1);
			return;
		}

		if (productionStatusChange != null) {
			sb.append("productionStatusChange: ").append(productionStatusChange);
			return;
		}

		if (excessiveDataBacklog != null) {
			sb.append("excessiveDataBacklog: ").append(excessiveDataBacklog);
			return;
		}

		if (endOfData != null) {
			sb.append("endOfData: ").append(endOfData);
			return;
		}

		sb.append("<none>");
	}

}

