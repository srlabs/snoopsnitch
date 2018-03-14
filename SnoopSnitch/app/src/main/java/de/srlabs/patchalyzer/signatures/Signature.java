package de.srlabs.patchalyzer.signatures;

import android.content.Context;
import android.util.Log;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.ProcessHelper;


/** Generic superclass for all signature types
 * Created by jonas on 14.12.17.
 */

public abstract class Signature {
    public static final String MASK_SIGNATURE_TYPE="MASK";
    private String filePath;
    private String signatureString;
    private String symbol;
    private boolean doStrip;
    private Context context;
    private HashMap<String,SymbolInformation> symTable;

    public Signature(){}

    public Signature(Context context, String signatureString, String symbol, String filePath, boolean doStrip){
        this.signatureString = signatureString;
        this.symbol = symbol;
        this.filePath = filePath;
        this.doStrip = doStrip;
        this.context = context;
    }

    public boolean check() throws Exception{
        Signature signature = parse(signatureString);

        this.symTable = readSymbolTable(filePath);

        int symbolPos = symTable.get(this.symbol).getPosition();
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
    public Signature parse(String signatureString){
        if(signatureString != null) {
            String signatureType = signatureString.split(":")[0];
            if(signatureType.equals(MASK_SIGNATURE_TYPE)){
                return new MaskSignature().parse(signatureString);
            }
            else{
                throw new IllegalStateException("Invalid signature type: "+signatureType);
            }
        }
        return null;
    }

    public abstract int getCodeLength();

    public abstract boolean checkCodeBuf(byte[] code);

    //FIXME: redundant code -> check function in TestUtils (using HashMap<String,JSONObject>)
    public HashMap<String,SymbolInformation> readSymbolTable(String filePath) throws Exception{
     Log.i(Constants.LOG_TAG,"Creating symbol table for file: "+filePath);
     String objFile = filePath;
     HashMap<String,SymbolInformation> symtable = new HashMap<>();

     if(filePath == null){
         throw new IllegalStateException("filePath argument == null!");
     }

     Pattern patternWhitespaces = Pattern.compile("\\s+");

     List<String> lines = ProcessHelper.getObjDumptTOutput(objFile);
     for (String line : lines){
         line = line.trim(); //.decode()
         if(!line.contains(".text"))
             continue;
         if(line.contains(".text.unlikely"))
             continue;
         if(line.contains(".text."))
             continue;


         String[] components = patternWhitespaces.split(line);
         if(components.length < 4)
             continue;

         String symbolName = components[components.length-1];
         String addrHex = components[0];
         String lenHex = null;
         for(int i = 0; i < components.length-1; i++){
            if(components[i].equals(".text")){
                lenHex = components[i+1];
            }
         }
         if(lenHex == null){
             throw new IllegalStateException("Invalid line: "+line);
         }
         int addr = Integer.parseInt(addrHex,16);
         int length = Integer.parseInt(lenHex,16);

         symtable.put(symbolName,new SymbolInformation(symbolName,addr,length));
     }
     ArrayList<Section> sections = new ArrayList<>();
     for (String line : ProcessHelper.getObjDumpHW(objFile)){
         line = line.trim(); //.decode()
         if(line.contains("CODE")){
             // Idx Name Size VMA LMA File off
             String[] items = patternWhitespaces.split(line);
             int size = Integer.parseInt(items[2],16);
             int vma = Integer.parseInt(items[3],16);
             int fileOffset = Integer.parseInt(items[5],16);
             sections.add(new Section(size,vma,fileOffset));
         }
     }

     // add pos
     for(SymbolInformation symbolInformation : symtable.values()){
         int addr = symbolInformation.getAddr();
         int pos = addr;
         for(Section section : sections){
             if(addr >= section.getVma() && addr < section.getVma()+section.getSize()){
                 pos = section.getFileOffset() + (addr - section.getVma());
                 symbolInformation.setPosition(pos);
             }
         }
     }

     if(filePath.endsWith(".o")){
        for(String line : ProcessHelper.getObjDumpHWwithCheck(objFile)){
            line = line.trim(); //.decode()
            if(!line.contains(".text.")){
                continue;
            }
            String[] components = patternWhitespaces.split(line);
            for(int i=0; i < components.length-1;i++){
                if(components[i].startsWith(".text.")){
                    String symbolName = components[i].substring((".text.").length());
                    int codeLen = Integer.parseInt(components[i+1],16);
                    int pos = Integer.parseInt(components[i+4],16);
                    SymbolInformation symbolInformation = new SymbolInformation(symbolName,pos,codeLen);
                    symbolInformation.setPosition(pos);
                    symtable.put(symbolName,symbolInformation);
                }
            }
        }
     }

     this.symTable = symtable;
     return symtable;
    }

    public HashMap<String,SymbolInformation> getSymTable(){
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
}
