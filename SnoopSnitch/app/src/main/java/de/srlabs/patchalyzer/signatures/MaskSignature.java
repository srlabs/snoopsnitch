package de.srlabs.patchalyzer.signatures;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.srlabs.patchalyzer.ProcessHelper;


/** Contains code length, checksum (SHA256) and list of masks
 *  Idea: Use mask to zero out all bits which can be change due to relocation entries in .o file
 * Created by jonas on 14.12.17.
 */
public class MaskSignature extends Signature {

	public static final String SIGNATURE_TYPE = "MASK";
	private List<Mask> maskList = new ArrayList<>();
	private String signatureType;
	private String architecture;
	private int codeLen;
	private byte[] originalCode;
	private String checksumSha256;


	public MaskSignature() {
		maskList = new ArrayList<>();
	}

	/**
	 * Reads a signature based on sigStr
	 * @param signatureString
	 */
	@Override
	public MaskSignature parse(String signatureString) {
		String[] parts  = signatureString.split(":");
		if (parts.length == 4) {
			this.signatureType = parts[0];
			this.codeLen = Integer.parseInt(parts[1], 16);
			this.checksumSha256 = parts[2];
			//Log.i(Constants.LOG_TAG,"sha256 checksum:"+this.checksumSha256);
			String maskString = parts[3];
			String[] maskStrList = maskString.split("_");
			int pos = 0;
			for (String maskStr : maskStrList) {
				int offset = Integer.parseInt(maskStr.substring(0, 4), 16);
				String maskCode = maskStr.substring(4);
				long mask = 0;
				switch (maskCode) {
				case "A":
					mask = 0x9f00001fL;
					break;
				case "B":
					mask = 0xffc003ffL;
					break;
				case "C":
					mask = 0xfc000000L;
					break;
				default:
					if (maskCode.length() == 8) {
						mask = Long.parseLong(maskCode, 16);
					} else {
						throw new IllegalStateException("Mask code not neccessary length!");
					}
				}
				pos += offset;
				this.maskList.add(new Mask(pos, mask));
			}
			return this;
		}
		//Log.e(Constants.LOG_TAG,"Error while parsing signature string, wrong format!");
		return null;
	}

	@Override
	public int getCodeLength() {
		return this.codeLen;
	}

	public String getSignatureType() {
		return this.signatureType;
	}


	/**
	 * Checks if the signature matches the given code
	 * @return
	 */
	@Override
	public boolean checkCodeBuf(byte[] code) {

		ByteArrayOutputStream maskedCode = new ByteArrayOutputStream();
		int maskPos = 0;
		for (int i = 0; i < code.length; i += 4) {
			byte[] instBytes = Arrays.copyOfRange(code, i, i + 4);
			if (maskPos < this.maskList.size() && this.maskList.get(maskPos).getPosition() == i) {
				long inst = Signature.unpack(instBytes);
				inst = (inst & this.maskList.get(maskPos).getMask());
				instBytes = Signature.pack(inst);
				maskPos += 1;
			}
			try {
				maskedCode.write(instBytes);
			} catch (IOException e) {
				//Log.e(Constants.LOG_TAG,"Error when appending bytes: "+e);
			}
		}

		String calculatedHash = ProcessHelper.sha256(maskedCode.toByteArray());
		//Log.d(Constants.LOG_TAG,"this.hash = "+this.checksumSha256+" calculated:"+calculatedHash);
		if (this.checksumSha256.equals(calculatedHash)) {
			return true;
		}
		return false;
	}

}
