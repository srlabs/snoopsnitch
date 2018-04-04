package de.srlabs.patchalyzer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;


/** This class is parsing the testsuite JSON file step by step to avoid having the full testsuite in memory at once
 * Created by jonas on 06.03.18.
 */

public class BasicTestParser {
    private static final String mTAG = "BasicTestParser: ";
    private File testSuiteJSON = null;
    private JsonReader jsonReader;
    private static HashMap<String,String[]> TEST_TYPE_FIELDS;


    public BasicTestParser(File testsuiteFile){
        this.testSuiteJSON = testsuiteFile;
        if(TEST_TYPE_FIELDS == null)
            fillNecessaryTestTypeFieldTester();
    }

    public BasicTestParser(){
        if(TEST_TYPE_FIELDS == null)
            fillNecessaryTestTypeFieldTester();
    }

    private void fillNecessaryTestTypeFieldTester(){
        TEST_TYPE_FIELDS = new HashMap<String, String[]>();

        TEST_TYPE_FIELDS.put("FILE_EXISTS", new String[]{"filename"});
        TEST_TYPE_FIELDS.put("FILE_CONTAINS_SUBSTRING", new String[]{"filename", "substringB64;substring"});
        TEST_TYPE_FIELDS.put("XZ_CONTAINS_SUBSTRING",new String[]{"filename", "substringB64;substring"});
        TEST_TYPE_FIELDS.put("ZIP_CONTAINS_SUBSTRING",new String[]{"zipFile","zipItem","substringB64;substring"});
        TEST_TYPE_FIELDS.put("ZIP_ENTRY_EXISTS",new String[]{"zipFile","zipItem"});
        TEST_TYPE_FIELDS.put("BINARY_CONTAINS_SYMBOL",new String[]{"filename","symbol"});
        TEST_TYPE_FIELDS.put("DISAS_FUNCTION_CONTAINS_STRING", new String[]{"filename","symbol","substringB64;substring"});
        TEST_TYPE_FIELDS.put("DISAS_FUNCTION_MATCHES_REGEX",new String[]{"filename","symbol","regex"});
        TEST_TYPE_FIELDS.put("MASK_SIGNATURE_SYMBOL",new String[]{"signature", "symbol","filename"});
        TEST_TYPE_FIELDS.put("JAVA_TEST",new String[]{"testClassName"});
        TEST_TYPE_FIELDS.put("CHIPSET_VENDOR",new String[]{"vendor;VENDOR"});
        TEST_TYPE_FIELDS.put("CHIPSET_VENDOR_OR_UNKNOWN",new String[]{"vendor;VENDOR"});
        TEST_TYPE_FIELDS.put("COMBINED_SIGNATURE",new String[]{"filename","rollingSignature","maskSignature"});
        TEST_TYPE_FIELDS.put("ROLLING_SIGNATURE",new String[]{"filename","rollingSignature"});
    }

    /**
     * Read until we reach the basicTest object
     */
    public void initReadingBasicTests() throws IllegalStateException{
        if(testSuiteJSON == null)
            throw new IllegalStateException("No testsuite JSON file specified for parsing!");
        seekTillBeginOfJSONObject("basicTests");
    }
    public void initReadingVulnerabilities() throws IllegalStateException{
        if(testSuiteJSON == null)
            throw new IllegalStateException("No testsuite JSON file specified for parsing!");
        seekTillBeginOfJSONObject("vulnerabilities");
    }
    private void seekTillBeginOfJSONObject(String objectName){
        try {
            InputStream inputStream = new FileInputStream(testSuiteJSON);
            jsonReader = new JsonReader(new InputStreamReader(inputStream));

            //read outer JSON object
            jsonReader.beginObject();

            boolean stop = false;
            while (jsonReader.hasNext() && !stop) {
                JsonToken jsonToken = jsonReader.peek();
                switch (jsonToken) {
                    case NAME:
                        String name = jsonReader.nextName();
                        if(name.equals(objectName)) {
                            jsonReader.beginObject();
                            stop = true;
                        }
                        else{
                            jsonReader.skipValue();
                        }
                        break;
                }
            }
        }catch(IOException e){
            Log.e(Constants.LOG_TAG,"Error while trying to read testsuite JSON file "+testSuiteJSON.getAbsolutePath()+":",e);
        }
    }

    public void finishReading(){
        try {
            jsonReader.endObject();
            jsonReader.close();
        }catch(IOException e){
            Log.d(Constants.LOG_TAG,"IOException when closing jsonReader: "+e.getMessage());
        }
    }

    /**
     * This method will iterate over all basic tests in the testsuite JSON file.
     * Prerequirement is to call: initReading() first to seek to the correct position in the file inputstream
     * @return basictests represented as JSONObject instances, or null if there are no further basictests or something went wrong while parsing
     */
    public JSONObject getNextBasicTest() {
        try {
            boolean finished = false;
            boolean isUUID = true;
            JSONObject test = null;
            while (jsonReader.hasNext() && !finished) {
                JsonToken jsonToken = jsonReader.peek();
                switch(jsonToken){
                    case NAME:
                        if(isUUID) {
                            String name = jsonReader.nextName();
                            if(!(Character.isUpperCase(name.charAt(0)) || Character.isDigit(name.charAt(0))))
                                return null;
                            test = new JSONObject();
                            test.put("uuid", name); //FIXME: will escape normal slashes in filepaths/filenames!
                            jsonReader.beginObject();
                            isUUID = false;
                        }
                        else{
                            String name = jsonReader.nextName();
                            String value = jsonReader.nextString();
                            test.put(name,value);
                        }
                        break;
                    case END_OBJECT:
                        finished = true;
                        break;
                }
            }
            if(test != null) {
                Log.d(Constants.LOG_TAG, "Parsed basic test: " + test.getString("uuid"));
                jsonReader.endObject();
            }
            return test;
        } catch (Exception e) {
            Log.d(Constants.LOG_TAG, "BasicTestParser: getNextBasicTest error: " + e);
        }
        return null;
    }

    //----------------------------------------------------------------------------------------------
    /**
     * This will parse all vulnerabilites related info from the JSON to a JSON object
     * Precondition: initReadingVulnerabilities
     */
    public JSONObject parseVulnerabilities(){
        return parseAndAddVulnerabilities(null);
    }

    public JSONObject parseAndAddVulnerabilities(JSONObject vulnerabilities){
        if(vulnerabilities == null)
            vulnerabilities = new JSONObject();
        try {
            boolean finished = false;

            while (jsonReader.hasNext() && !finished) {
                JsonToken jsonToken = jsonReader.peek();
                switch(jsonToken){
                    case NAME:
                        String name = jsonReader.nextName();
                        //Log.d(Constants.LOG_TAG,"1st layer) Found NAME:"+name);
                        vulnerabilities.put(name, parseVulnerability(jsonReader));
                        break;
                    case END_OBJECT:
                        finished = true;
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "BasicTestParser: parseVulnerabilities error: " + e);
        }
        return vulnerabilities;
    }

    private JSONObject parseVulnerability(JsonReader jsonReader) throws IOException,JSONException{
        jsonReader.beginObject();
        JSONObject vuln = new JSONObject();
        while (jsonReader.hasNext()) {
            JsonToken jsonToken = jsonReader.peek();
            switch (jsonToken) {
                case NAME:
                    String name = jsonReader.nextName();
                    //Log.d(Constants.LOG_TAG,"2nd layer) Found NAME:"+name);
                    switch (name) {
                        case "category":
                        case "cve":
                        case "severity":
                        case "title":
                        case "identifier":
                        case "patchlevelDate":
                            String info = jsonReader.nextString();
                            //Log.d(Constants.LOG_TAG,"2nd layer) Found string: "+name+" -> "+info);
                            vuln.put(name, info);
                            break;
                        case "maxApiLevel":
                        case "minApiLevel":
                        case "testVersion":
                            Integer infoInt = jsonReader.nextInt();
                            //Log.d(Constants.LOG_TAG,"2nd layer) Found int: "+name+" -> "+infoInt);
                            vuln.put(name, infoInt);
                            break;
                        case "testRequires64bit":
                            Boolean bool = jsonReader.nextBoolean();
                            //Log.d(Constants.LOG_TAG,"2nd layer) Found boolean: "+name+" -> "+bool);
                            vuln.put(name,bool);
                            break;
                        case "testFixed":
                        case "testNotAffected":
                        case "testVulnerable":
                            vuln.put(name,parseSubTestInfo(jsonReader));
                            break;

                    }
                    break;
            }
        }
        jsonReader.endObject();
        return vuln;
    }

    private JSONObject parseSubTestInfo(JsonReader jsonReader) throws IOException, JSONException{
        jsonReader.beginObject();
        JSONObject subtests = new JSONObject();
        while (jsonReader.hasNext()) {
            JsonToken jsonToken = jsonReader.peek();
            switch (jsonToken) {
                case NAME:
                    String name = jsonReader.nextName();
                    switch(name){
                        case "testType":
                            String testType = jsonReader.nextString();
                            subtests.put(name,testType);
                            break;
                        case "subtests":
                            subtests.put(name, parseSubTests(jsonReader));
                    }
            }
        }
        jsonReader.endObject();
        return subtests;
    }

    private JSONArray parseSubTests(JsonReader jsonReader) throws IOException, JSONException{
        jsonReader.beginArray();
        JSONArray subtests = new JSONArray();
        while(jsonReader.hasNext()){
            JsonToken jsonToken = jsonReader.peek();
            switch (jsonToken) {
                case STRING:
                    subtests.put(jsonReader.nextString());
                    break;
                case BEGIN_OBJECT:
                    subtests.put(parseSubTestInfo(jsonReader));
            }
        }
        jsonReader.endArray();
        return subtests;
    }

    public static void checkTestTypeSufficientInfo(JSONObject basicTest) throws JSONException, IllegalStateException{
        if (basicTest == null || !basicTest.has("testType"))
            throw new IllegalStateException("basic test not valid!");

        String testType = basicTest.getString("testType");

        if (!TEST_TYPE_FIELDS.containsKey(testType))
            throw new IllegalStateException("Unknown testType of basicTest");

        for(String fieldname : TEST_TYPE_FIELDS.get(testType)){
            if(fieldname.contains(";")){
                String[] parts = fieldname.split(";");
                if(!basicTest.has(parts[0]) && !basicTest.has(parts[1])){
                    throw new IllegalStateException("(parts:"+parts[0]+" - "+parts[1]+" -Missing necessary field for a test: "+testType+" :"+fieldname);
                }
            }
            else if(!basicTest.has(fieldname))
                throw new IllegalStateException("Missing necessary field for a test: "+testType+" :"+fieldname);
        }

    }

}