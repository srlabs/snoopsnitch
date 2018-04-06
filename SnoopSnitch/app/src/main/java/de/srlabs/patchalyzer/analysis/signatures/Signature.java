package de.srlabs.patchalyzer.analysis.signatures;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;

import de.srlabs.patchalyzer.analysis.TestUtils;


/** Generic superclass for all signature types
 * Created by jonas on 14.12.17.
 */

public abstract class Signature {
	private String filePath;
	private String signatureString;
	private String symbol;
	private boolean doStrip;
	private HashMap<String, SymbolInformation> symTable;

	public Signature() {}

	public Signature(String signatureString, String symbol, String filePath, boolean doStrip) {
		this.signatureString = signatureString;
		this.symbol = symbol;
		this.filePath = filePath;
		this.doStrip = doStrip;
	}

	public boolean check() throws Exception {
		Signature signature = parse(signatureString);

		this.symTable = readSymbolTable(filePath);

		long symbolPos = symTable.get(this.symbol).getPosition();
		int symbolLength = symTable.get(this.symbol).getLength();

		//read file content region to byte array
		byte[] codeBuf = new byte[symbolLength];
		RandomAccessFile file = new RandomAccessFile(filePath, "r");
		file.seek(symbolPos);
		file.read(codeBuf);
		file.close();

		return signature.checkCodeBuf(codeBuf);
	}

	/**
	 * Reads a signature from it's string reprentation, automatically choosing the right subclass
	 * @param signatureString
	 */
	public static Signature getInstance(String signatureString) throws IOException{
		if (signatureString != null) {
			String signatureType = signatureString.split(":")[0];
			if (signatureType.equals(MaskSignature.SIGNATURE_TYPE)) {
				return new MaskSignature().parse(signatureString);
			} else if (Arrays.asList(RollingSignature.SIGNATURE_TYPES).contains(signatureType)) {
				return new RollingSignature().parse(signatureString);
			} else {
				throw new IllegalStateException("Invalid signature type: " + signatureType);
			}
		}
		return null;
	}

	public abstract Signature parse(String signatureString) throws IOException;

	public abstract int getCodeLength();

	public abstract boolean checkCodeBuf(byte[] code);

	//FIXME: redundant code -> check function in TestUtils (using HashMap<String,JSONObject>)
	public HashMap<String, SymbolInformation> readSymbolTable(String filePath) throws Exception {
		HashMap<String, SymbolInformation> symtable = TestUtils.readSymbolTable(filePath);
		this.symTable = symtable;
		return symtable;
	}



	public HashMap<String, SymbolInformation> getSymTable() {
		return symTable;
	}

	/**
	 * unpack byte array to unsigned 32bit integer and wrap as long
	 */
	public static long unpack(byte[] bytes) {
		long value = bytes[0] & 0xFFL;
		value |= (bytes[1] << 8) & 0xFFFFL;
		value |= (bytes[2] << 16) & 0xFFFFFFL;
		value |= (bytes[3] << 24) & 0xFFFFFFFFL;
		return value;
	}

	/**
	 * pack unsigned 32bit integer (wrapped in long) to byte array
	 * @param i
	 * @return
	 */
	public static byte[] pack(long i) {
		byte[] result = new byte[4];
		result[3] = (byte) ((i >> 24) & 0xff);
		result[2] = (byte) ((i >> 16) & 0xff);
		result[1] = (byte) ((i >> 8) & 0xff);
		result[0] = (byte) ((i >> 0) & 0xff);
		return result;
	}

	/**
	 * Convert byte array to hex string
	 * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java/9855338#9855338
	 *  -> supposed to be faster than String.format()
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789abcdef".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
