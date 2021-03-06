/**
 * This class file was automatically generated by jASN1 v1.11.2 (http://www.beanit.com)
 */

package ccsds.sle.transfer.service.isp1.credentials;

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


public class HashInput implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 16);

	public byte[] code = null;
	private BerOctetString time = null;
	private BerInteger randomNumber = null;
	private BerVisibleString userName = null;
	private BerOctetString passWord = null;
	
	public HashInput() {
	}

	public HashInput(byte[] code) {
		this.code = code;
	}

	public void setTime(BerOctetString time) {
		this.time = time;
	}

	public BerOctetString getTime() {
		return time;
	}

	public void setRandomNumber(BerInteger randomNumber) {
		this.randomNumber = randomNumber;
	}

	public BerInteger getRandomNumber() {
		return randomNumber;
	}

	public void setUserName(BerVisibleString userName) {
		this.userName = userName;
	}

	public BerVisibleString getUserName() {
		return userName;
	}

	public void setPassWord(BerOctetString passWord) {
		this.passWord = passWord;
	}

	public BerOctetString getPassWord() {
		return passWord;
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
		codeLength += passWord.encode(reverseOS, true);
		
		codeLength += userName.encode(reverseOS, true);
		
		codeLength += randomNumber.encode(reverseOS, true);
		
		codeLength += time.encode(reverseOS, true);
		
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
		if (berTag.equals(BerOctetString.tag)) {
			time = new BerOctetString();
			subCodeLength += time.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(BerInteger.tag)) {
			randomNumber = new BerInteger();
			subCodeLength += randomNumber.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(BerVisibleString.tag)) {
			userName = new BerVisibleString();
			subCodeLength += userName.decode(is, false);
			subCodeLength += berTag.decode(is);
		}
		else {
			throw new IOException("Tag does not match the mandatory sequence element tag.");
		}
		
		if (berTag.equals(BerOctetString.tag)) {
			passWord = new BerOctetString();
			subCodeLength += passWord.decode(is, false);
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
		if (time != null) {
			sb.append("time: ").append(time);
		}
		else {
			sb.append("time: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (randomNumber != null) {
			sb.append("randomNumber: ").append(randomNumber);
		}
		else {
			sb.append("randomNumber: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (userName != null) {
			sb.append("userName: ").append(userName);
		}
		else {
			sb.append("userName: <empty-required-field>");
		}
		
		sb.append(",\n");
		for (int i = 0; i < indentLevel + 1; i++) {
			sb.append("\t");
		}
		if (passWord != null) {
			sb.append("passWord: ").append(passWord);
		}
		else {
			sb.append("passWord: <empty-required-field>");
		}
		
		sb.append("\n");
		for (int i = 0; i < indentLevel; i++) {
			sb.append("\t");
		}
		sb.append("}");
	}

}

