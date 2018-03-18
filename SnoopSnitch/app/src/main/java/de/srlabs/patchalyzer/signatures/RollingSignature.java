package de.srlabs.patchalyzer.signatures;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import android.content.Context;
import android.util.Log;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.ProcessHelper;

/**
 * This class represents a rolling signature. Rolling signatures are based on the rsync rolling checksum algorithm:
	https://rsync.samba.org/tech_report/node3.html
	This algorithm allows quick scanning of a binary file.
	For the signature the input code is pre-filtered to zero out certain bits depending on the
	instruction opcode (which may be modified by the linker due to reloc entries). Unfortunately
	this only works for instruction sets with a fixed instruction length, e.g. ARM or AARCH64 but
	not with instruction sets with a variable instruction length (e.g. X86/AMD64 or Thumb).
	Currently only AARCH64 is implemented.

	When scanning for a single signature it would be possible to make a rolling checksum with exactly the
	right size for this function. However, in a typical situation many different signatures with different
	code lengths will exist for a given function (depending on patch status and compiler version/options).
	This would require many checksum sizes and slow down the search process.

	In order to mitigate this problem, the rolling signatures will only use fixed checksum sizes of  size 4, 8,
	16, 32, 64, 128, 256, 512, 1024, 2048, 4096, ... bytes. In order to fully cover the function code, one
	rolling signature consists of two (partly overlapping) checksums. For example, if the function size is
	72 bytes, the first checksum will cover bytes 0-64 and the second checksum bytes 8-72. When both checksums
	are found in the target binary with an offset of 8 bytes between the checksums, this results in a match
	of the complete rolling signature.

	For performance reasons the actual signature computation is offloaded to a C binary.

	Signature text representation: See the toString() method
 * @author jonas
 *
 */
public class RollingSignature extends Signature {

	public static final String[] SIGNATURE_TYPES = {"R_AARCH64_V1"};
	private String signatureType;
	private int checksumLen;
	private int checksumOffset;
	private byte[] checksum1;
	private byte[] checksum2;
	private int codeLen;
	private Context context;

	public int getCheckSumLen() {
		return this.checksumLen;
	}

	public int getCodeLength() {
		return this.codeLen;
	}

	public int getChecksumOffset() {
		return this.checksumOffset;
	}

	public byte[] getChecksum1() {
		return this.checksum1;
	}

	public byte[] getChecksum2() {
		return this.checksum2;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	private String getArchArg() {
		return getArchArgFromSigType(this.signatureType);
	}

	/**
	 * Get the arch argument for sigtool
	 * @param signatureType signature type string
	 * @return parameter for sigtool binary
	 */
	public String getArchArgFromSigType(String signatureType) throws IllegalStateException {
		if (signatureType.equals("R_AARCH64_V1"))
			return "--aarch64v1";
		else
			throw new IllegalStateException("Invalid sigType " + signatureType);
	}

	/**
	 * Requirement: Set context first with setContext()
	 * @param code
	 * @return
	 */
	public boolean checkCodeBuf(byte[] code) {
		//FIXME not referenced/needed?!
		if (context == null) {
			throw new IllegalStateException("Error: Set context first, to create temporary file!");
		}
		//create temp file
		File tempFn = null;
		try {
			tempFn = File.createTempFile("patchalyzer", ".tmp", context.getCacheDir());
			FileOutputStream out = new FileOutputStream(tempFn);
			out.write(code);
			out.close();
			Log.d(Constants.LOG_TAG, "DEBUG: Created and wrote to temporary file: " + tempFn.getAbsolutePath());
		} catch (IOException e) {
			throw new IllegalStateException("Error while creating or writing temporary file:", e);
		}
		if (tempFn.length() != code.length) {
			throw new IllegalStateException("Writing to temporary file failed, incorrect file size");
		}
		try {
			byte[] checksum1 = ProcessHelper.getSigToolCalcOutput("sigtool", getArchArg(), tempFn.getAbsolutePath(), "0", "" + this.checksumLen);
			if (!Arrays.equals(checksum1, this.checksum1)) {
				return false;
			}
		} catch (Exception e) {
			Log.d(Constants.LOG_TAG, "Exception when running sigtool: " + e.getMessage());
		}
		try {
			byte[] checksum2 = ProcessHelper.getSigToolCalcOutput("sigtool", getArchArg(), tempFn.getAbsolutePath(), "" + this.checksumOffset, "" + this.checksumLen);
			if (!Arrays.equals(checksum2, this.checksum2)) {
				return false;
			}
		} catch (Exception e) {
			Log.d(Constants.LOG_TAG, "Exception when running sigtool: " + e.getMessage());
		}
		return true;
	}

	@Override
	public RollingSignature parse(String signatureString) {
		if (signatureString == null || signatureString.length() == 0) {
			return null;
		}
		String[] parts = signatureString.split(":");
		if (parts != null && parts.length == 3) {
			this.signatureType = parts[0];
			String signatureData = parts[1] + parts[2];
			this.checksumLen = (int) Math.pow(2, Integer.parseInt(signatureData.substring(0, 2), 16));
			this.checksumOffset = Integer.parseInt(signatureData.substring(2, 8), 16); //Offset between the two (overlapping) checksums
			byte[] sigsBin = new BigInteger(parts[2], 16).toByteArray();
			if (sigsBin.length != 16) {
				throw new IllegalStateException("Malformated signatureString: sigsBin.length != 16");
			}
			this.checksum1 = Arrays.copyOfRange(sigsBin, 0, 8);
			this.checksum2 = Arrays.copyOfRange(sigsBin, 8, sigsBin.length);
			this.codeLen = this.checksumLen + this.checksumOffset;
			if (!toString().equals(signatureString)) { // Consistency check, make sure that parsing worked correctly by re-encoding the signature
				Log.d(Constants.LOG_TAG, signatureString);
				Log.d(Constants.LOG_TAG, toString());
				throw new IllegalStateException("Error while parsing signatureString, reencoding not producing same signatureString");
			}
			return this;
		}
		return null;
	}

	/**
	 * Creates the signature string
	 */
	public String toString() {
		if (this.checksumOffset < 0) {
			throw new IllegalStateException("Error while creating signatureString: checksumOffset < 0!");
		}
		StringBuilder stringRepresentation = new StringBuilder();
		stringRepresentation.append(this.signatureType);
		stringRepresentation.append(":");
		int x = (int)(Math.log(this.checksumLen) / Math.log(2));
		stringRepresentation.append(String.format("%02x%06x:", x, this.checksumOffset));
		stringRepresentation.append(Signature.bytesToHex(this.checksum1));
		stringRepresentation.append(Signature.bytesToHex(this.checksum2));
		return stringRepresentation.toString();
	}



}
