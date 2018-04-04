package de.srlabs.patchalyzer;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Vector;

import de.srlabs.patchalyzer.signatures.SymbolInformation;

/**This class bundles similiar tests (== same file and probably same test type) together to cache certain information.
 * This way we can reduce the amount of redundant work like e.g. the same objdump calls.
 * Created by jonas on 15.03.18.
 */

public class TestBundle {

    private Vector<JSONObject> basicTests = null;
    private Vector<String> objdumpLines = null;
    private HashMap<String, SymbolInformation> symbolTable = null;
    private String filename = null;
    private boolean isStopMarker = false;

    private TestBundle(){
        this.basicTests = new Vector<JSONObject>();
    }

    public TestBundle(String filename){
        this.filename = filename;
        this.basicTests = new Vector<JSONObject>();
    }

    public static TestBundle getStopMarker(){
        TestBundle stopMarker = new TestBundle();
        stopMarker.setStopMarker();
        return stopMarker;
    }

    public void add(JSONObject basicTest){
        basicTests.add(basicTest);
    }

    public void setSymbolTable(HashMap<String, SymbolInformation> symbolTable){
        this.symbolTable = symbolTable;
    }

    public void setStopMarker() {
        this.isStopMarker = true;
    }

    public boolean isStopMarker() {
        return isStopMarker;
    }

    public Vector<JSONObject> getBasicTests() {
        return basicTests;
    }

    public String getFilename(){
        return filename;
    }

    public HashMap<String,SymbolInformation> getSymbolTable() {
        return symbolTable;
    }

    public int getTestCount(){
        return basicTests.size();
    }

    public Vector<String> getObjdumpLines() {
        return objdumpLines;
    }

    public void setObjdumpLines(Vector<String> objdumpLines) {
        this.objdumpLines = objdumpLines;
    }
}
