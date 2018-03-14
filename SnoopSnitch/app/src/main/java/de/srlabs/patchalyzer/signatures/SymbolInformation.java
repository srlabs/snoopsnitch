package de.srlabs.patchalyzer.signatures;

/** This object will contain all the information about symbols
 * Created by jonas on 14.12.17.
 */

public class SymbolInformation {

    private String symbolName;
    private int position;
    private int addr;
    private int length;

    public SymbolInformation(String symbolName, int addr, int length){
        this.symbolName = symbolName;
        this.addr = addr;
        this.length = length;
    }

    public void setPosition(int position){
        this.position = position;
    }

    public int getPosition(){
        return position;
    }

    public int getAddr(){
        return addr;
    }

    public int getLength(){
        return length;
    }

    public String getSymbolName(){
        return symbolName;
    }

}
