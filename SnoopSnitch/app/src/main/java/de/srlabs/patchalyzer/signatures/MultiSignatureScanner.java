package de.srlabs.patchalyzer.signatures;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.ProcessHelper;

/**
 * Scanner for searching for a set of multiple rolling signatures in a file
 * @author jonas
 *
 */
public class MultiSignatureScanner {

	private HashSet<RollingSignature> signatureChecker;

	public MultiSignatureScanner() {
		signatureChecker = new HashSet<RollingSignature>();
	}

	/**
	 * Add rolling signature check (string)
	 * @param signatureString
	 */
	public void addSignatureChecker(String signatureString) {
		Signature checker = Signature.getInstance(signatureString);
		addSignatureChecker(checker);
	}
	/**
	 * Add rolling signature check objects
	 * @param checker
	 */
	public void addSignatureChecker(Signature checker) {
		if (checker == null || !(checker instanceof RollingSignature))
			throw new IllegalStateException("Error: signature checker must be of type 'RollingSignature'");
		signatureChecker.add((RollingSignature) checker);
	}

	/**
	 * Scans a file for a number of signatures. Returns a set of SymbolInformation objects (containing signature names and their position in the file)
	 * @param filePath
	 * @return set of all found symbols and their position wrapped in SymbolInformation objects
	 */
	public Set<SymbolInformation> scanFile(String filePath) throws IOException, IllegalStateException {

		File targetFile = new File(filePath);
		if (targetFile == null || !targetFile.exists()) {
			throw new IllegalStateException("Scanning file not possible, cause the file does not exist!");
		}
		long fileSize = targetFile.length();
		Log.d(Constants.LOG_TAG, "DEBUG: fileSize:" + fileSize);
		// Map (summarize) all checksumLengths (from all rolling signature checks) to a set of the checksum1 and checksum2 byte arrays
		TreeMap<Integer, Set<byte[]>> checksumLengths = new TreeMap<Integer, Set<byte[]>>();
		for (RollingSignature checker : signatureChecker) {
			int checksumLength = checker.getCheckSumLen();
			if (!checksumLengths.containsKey(checksumLength)) {
				checksumLengths.put(checksumLength, new HashSet<byte[]>());
			}
			Set<byte[]> checksums = checksumLengths.get(checksumLength);
			checksums.add(checker.getChecksum1());
			checksums.add(checker.getChecksum2());
			Log.d(Constants.LOG_TAG, "DEBUG: checksumLength: " + checksumLength + " -> checksum1:" + Signature.bytesToHex(checker.getChecksum1()) + " checksum2:" + Signature.bytesToHex(checker.getChecksum2()));
		}
		//build byte buffer to send to sigtool binary for searching
		// structure: [number of total signature checks]+
		//						for each individual checksum length, append: [number of checksums with length X][X==checksum length][checksum 0 with length X][...][checksum n with length X]
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		buf.write(Signature.pack(checksumLengths.keySet().size()));
		NavigableSet<Integer> checksumLenghtsKeys = checksumLengths.navigableKeySet(); //TODO test! -> this should be sorted in ascending order
		for (Integer checksumLen : checksumLenghtsKeys) {
			Log.d(Constants.LOG_TAG, "DEBUG: checksumLen: " + checksumLen);
			buf.write(Signature.pack(checksumLengths.get(checksumLen).size()));
			buf.write(Signature.pack(checksumLen));
			for (byte[] checksum : checksumLengths.get(checksumLen)) {
				buf.write(checksum);
			}
		}
		//let sigtool binary do the searching, and parse the stdout here
		Log.d(Constants.LOG_TAG, "DEBUG: byte buffer size:" + buf.size());
		byte[] result = ProcessHelper.sendByteBufferToSigToolSearch("sigtool", buf.toByteArray(), "--aarch64v1", targetFile.getAbsolutePath());
		if(isPermissionDeniedError(result)){
			throw new IOException("Error when scanning file: "+filePath+" - Permission denied.");
		}
		if ((result.length % 16) != 0) {
			throw new IllegalStateException("Output length not a multiple of 16 bytes: " + result.length + " " + Signature.bytesToHex(result));
		}
		Log.d(Constants.LOG_TAG, "DEBUG: Finished searching in file with sigtool!");

		Log.d(Constants.LOG_TAG, "DEBUG: Creating checksumsFound datastructure and filling it...");
		HashMap<Integer, HashMap<String, Set<Long>>> checksumsFound = null; //use hex strings of checksums, cause byte[] are not suitable for hashing/comparing here
		try {
			checksumsFound = new HashMap<Integer, HashMap<String, Set<Long>>>();
			for (int i = 0; i < result.length; i += 16) {
				long position = Signature.unpack(Arrays.copyOfRange(result, i, i + 4));
				int checksumLen = (int) Signature.unpack(Arrays.copyOfRange(result, i + 4, i + 8));
				if (position > fileSize) {
					throw new IllegalStateException("Parsed symbol position exceeds file size.");
				}
				if (checksumLen >= 1000000) {
					throw new IllegalStateException("Length of checksum is too big (>= 1e6)");
				}

				byte[] checksum = Arrays.copyOfRange(result, i + 8, i + 16);
				if (!checksumsFound.containsKey(checksumLen)) {
					Log.d(Constants.LOG_TAG, "DEBUG: adding checksumLen:" + checksumLen + " -> new Hashmap");
					checksumsFound.put(checksumLen, new HashMap<String, Set<Long>>());
				}
				if (!checksumsFound.get(checksumLen).containsKey(checksum)) {
					HashSet<Long> hashset = new HashSet<Long>();
					checksumsFound.get(checksumLen).put(Signature.bytesToHex(checksum), hashset);
				}
				//checksumsFound[checksumLen][checksum].add(pos)
				checksumsFound.get(checksumLen).get(Signature.bytesToHex(checksum)).add(position);
				Log.d(Constants.LOG_TAG, "DEBUG: FILL: checksumLen:" + checksumLen + " checksum:" + Signature.bytesToHex(checksum) + " position:" + position + " entries:" + checksumsFound.get(checksumLen).size());
			}
		} catch (Exception e) {
			Log.e(Constants.LOG_TAG, "Error: when building checksumsFound datastructure!:" + e.getMessage());
		}
		Log.d(Constants.LOG_TAG, "DEBUG: Created checksumsFound datastructure!");

		Log.d(Constants.LOG_TAG, "DEBUG: Gathering results information...");
		Set<SymbolInformation> foundItems = new HashSet<SymbolInformation>();
		for (RollingSignature checker : signatureChecker) {
			Log.d(Constants.LOG_TAG, "DEBUG: checksumLen=" + checker.getCheckSumLen() + " sigOffset=" + checker.getChecksumOffset() + " sigStr=" + checker.toString());
			int checksumLen = checker.getCheckSumLen();
			Log.d(Constants.LOG_TAG, "DEBUG: checksum1: " + Signature.bytesToHex(checker.getChecksum1()) + " checksum2:" + Signature.bytesToHex(checker.getChecksum2()));

			if (checksumsFound.get(checksumLen).get(Signature.bytesToHex(checker.getChecksum2())) == null) {
				Log.d(Constants.LOG_TAG, "DEBUG: checksum2 not found!");
				continue;
			}
			if (checksumsFound.get(checksumLen).get(Signature.bytesToHex(checker.getChecksum1())) == null ) {
				Log.d(Constants.LOG_TAG, "DEBUG: checksum1 not found!");
				continue;
			}

			for (Long found1pos : checksumsFound.get(checksumLen).get(Signature.bytesToHex(checker.getChecksum1()))) {
				Log.d(Constants.LOG_TAG, "DEBUG: FOUND1: " + checker.toString() + " at " + found1pos);
				long wantedFound2pos = found1pos + checker.getChecksumOffset();
				if (checksumsFound.get(checksumLen).get(Signature.bytesToHex(checker.getChecksum2())).contains(wantedFound2pos)) {
					foundItems.add(new SymbolInformation(checker.toString(), found1pos)); //TODO solve this!
				}
			}
		}
		Log.d(Constants.LOG_TAG, "DEBUG: Gathered results information!");
		return foundItems;
	}

	private boolean isPermissionDeniedError(byte[] result) {
		if(result != null){
			String resultString = Signature.bytesToHex(result);
			String permissionDeniedHexMessage = "4661696c656420746f206f70656e2066696c650a3a205065726d697373696f6e2064656e6965640a";
			Log.d(Constants.LOG_TAG,"resultString: "+resultString+" permissionDeniedHex: "+permissionDeniedHexMessage);
			return resultString.equals(permissionDeniedHexMessage);
		}
		return false;
	}
}
