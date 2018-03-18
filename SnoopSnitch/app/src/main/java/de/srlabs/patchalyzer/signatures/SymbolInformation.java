package de.srlabs.patchalyzer.signatures;

/** This object contains all the information about symbols
 * Created by jonas on 14.12.17.
 */

public class SymbolInformation {

	private String symbolName;
	private long position;
	private int addr;
	private int length;

	public SymbolInformation(String symbolName, long pos) {
		this.symbolName = symbolName;
		this.position = pos;
	}

	public SymbolInformation(String symbolName, int addr, int length) {
		this.symbolName = symbolName;
		this.addr = addr;
		this.length = length;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	public long getPosition() {
		return position;
	}

	public int getAddr() {
		return addr;
	}

	public int getLength() {
		return length;
	}

	public String getSymbolName() {
		return symbolName;
	}

	@Override
	public int hashCode() {
		return symbolName.hashCode() + ((int)position) + addr + length;
	}

}
