/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.bind.types;

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

import ccsds.sle.transfer.service.common.types.Credentials;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.service.instance.id.ServiceInstanceIdentifier;

public class UnbindReason extends BerInteger {

	private static final long serialVersionUID = 1L;

	public UnbindReason() {
	}

	public UnbindReason(byte[] code) {
		super(code);
	}

	public UnbindReason(BigInteger value) {
		super(value);
	}

	public UnbindReason(long value) {
		super(value);
	}

}