/**
 * This class file was automatically generated by jASN1 v1.10.1-SNAPSHOT (http://www.openmuc.org)
 */

package ccsds.sle.transfer.service.cltu.structures;

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
import ccsds.sle.transfer.service.common.types.ConditionalTime;
import ccsds.sle.transfer.service.common.types.DeliveryMode;
import ccsds.sle.transfer.service.common.types.Diagnostics;
import ccsds.sle.transfer.service.common.types.Duration;
import ccsds.sle.transfer.service.common.types.ForwardDuStatus;
import ccsds.sle.transfer.service.common.types.IntPosLong;
import ccsds.sle.transfer.service.common.types.IntPosShort;
import ccsds.sle.transfer.service.common.types.IntUnsignedLong;
import ccsds.sle.transfer.service.common.types.IntUnsignedShort;
import ccsds.sle.transfer.service.common.types.ParameterName;
import ccsds.sle.transfer.service.common.types.SpaceLinkDataUnit;
import ccsds.sle.transfer.service.common.types.Time;

public class NumberOfCltusRadiated extends IntUnsignedLong {

	private static final long serialVersionUID = 1L;

	public NumberOfCltusRadiated() {
	}

	public NumberOfCltusRadiated(byte[] code) {
		super(code);
	}

	public NumberOfCltusRadiated(BigInteger value) {
		super(value);
	}

	public NumberOfCltusRadiated(long value) {
		super(value);
	}

}
