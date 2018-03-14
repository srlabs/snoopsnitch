package de.srlabs.patchalyzer.signatures;

/**
 * Created by jonas on 14.12.17.
 */

public class Section {
    private int size;
    private int vma;
    private int fileOffset;

    public Section(int size, int vma, int offset){
        this.size = size;
        this.vma = vma;
        this.fileOffset = offset;
    }

    public int getSize(){
        return size;
    }

    public int getVma(){
        return vma;
    }

    public int getFileOffset(){
        return fileOffset;
    }


}
