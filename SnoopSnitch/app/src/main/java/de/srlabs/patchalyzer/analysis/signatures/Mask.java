package de.srlabs.patchalyzer.analysis.signatures;

/**
 * Created by jonas on 15.12.17.
 */

public class Mask {

    private int position;
    private long mask;

    public Mask(int position, long mask){
        this.mask = mask;
        this.position = position;
    }
    public int getPosition(){
        return position;
    }
    public long getMask(){
        return mask;
    }

}
