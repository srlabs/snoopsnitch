package de.srlabs.patchalyzer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
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
    private SQLiteDatabase db;
    private Context context;
    private HashMap<String,String[]> TEST_TYPE_FIELDS;


    public BasicTestParser(Context context, File testsuiteFile){
        this.context = context;
        this.testSuiteJSON = testsuiteFile;
        MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
        fillNecessaryTestTypeFieldTester();
    }

    public BasicTestParser(Context context){
        this.context = context;
        MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
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
                Log.d(Constants.LOG_TAG, "Parsed basic test: " + test.toString());
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
    //----------------------------------------------------------------------------------------------
    /**
     * Write a JSONObject representation of a basic test to the SQLite DB
     * @param basicTest
     * @return
     */
    public void insertBasicTestToDB(final JSONObject basicTest) throws IllegalStateException, SQLiteConstraintException{
        if(basicTest == null ){
            throw new IllegalStateException(mTAG+"Null objects can not be added to DB, sneaky...");
        }
        if(!basicTest.has("uuid") || !basicTest.has("testType")){
            throw new IllegalStateException("JSONObject does not contain necessary info (uuid + testType): "+basicTest.toString());
        }

        try {
            if(db == null || !db.isOpen())
                db = MsdDatabaseManager.getInstance().openDatabase();
            //get all keys from basic test
            Iterator<String> keyIterator = basicTest.keys();
            Set<String> keys = new HashSet<String>();
            while (keyIterator.hasNext()) {
                keys.add(keyIterator.next());
            }

            checkTestTypeSufficientInfo(basicTest);

            ContentValues values = new ContentValues();
            //write to basictests table
            for (String key : keys) {
                if (key.equals("substring")) {
                    byte[] data = basicTest.getString("substring").getBytes("UTF-8");
                    String base64String = Base64.encodeToString(data, Base64.DEFAULT);
                    values.put("substringB64", base64String);
                } else {
                    values.put(key, basicTest.getString(key));
                }
            }
            db.insertOrThrow("basictests", null, values);

        }catch(JSONException | UnsupportedEncodingException e){
            Log.e(Constants.LOG_TAG,mTAG+"Error while parsing JSON info and adding basic test to DB:"+e);
        }
    }

    private void checkTestTypeSufficientInfo(JSONObject basicTest) throws JSONException, IllegalStateException{
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

    public Vector<JSONObject> getNotPerformedTests(int limit){
        Log.d(Constants.LOG_TAG,"getNotPerformedTests called with limit: "+limit);
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }

        Vector<JSONObject> results = new Vector<>();

        //basic test table info
        Cursor cursor = db.query(
                "basictests",   // The table to query
                null,             // The array of columns to return (pass null to get all)
                "result = ? and exception IS NULL",              // The columns for the WHERE clause
                new String[]{"-1"},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null,               // The sort order
                ""+limit
        );

        Log.d(Constants.LOG_TAG,"Got batch of tests with size: "+cursor.getCount()+" from DB!");

        while(cursor.moveToNext()){
            try {
                JSONObject basicTest = null;
                basicTest = new JSONObject();
                String[] columns = cursor.getColumnNames();
                String testType = null;
                for (String column : columns) {
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                results.add(basicTest);
            }catch(JSONException e){
                Log.d(Constants.LOG_TAG,"JSONException while parsing basic test:"+e.getMessage());
            }
        }
        cursor.close();
        return results;
    }

    public Vector<JSONObject> getNotPerformedTestsSortedByFilenameAndTestType( int limit){
        Log.d(Constants.LOG_TAG,"getNotPerformedTests called with limit: "+limit);
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }

        Vector<JSONObject> results = new Vector<>();

        //basic test table info
        Cursor cursor = db.query(
                "basictests",   // The table to query
                null,             // The array of columns to return (pass null to get all)
                "result = ? and exception IS NULL",              // The columns for the WHERE clause
                new String[]{"-1"},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                "filename, testType DESC",               // The sort order
                ""+limit
        );

        if(cursor == null || cursor.getCount() == 0)
            return null;

        Log.d(Constants.LOG_TAG,"Got batch of tests with size: "+cursor.getCount()+" from DB!");

        while(cursor.moveToNext()){
            try {
                JSONObject basicTest = null;
                basicTest = new JSONObject();
                String[] columns = cursor.getColumnNames();
                for (String column : columns) {
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                results.add(basicTest);
            }catch(JSONException e){
                Log.d(Constants.LOG_TAG,"JSONException while parsing basic test:"+e.getMessage());
            }
        }
        cursor.close();
        return results;
    }

    public void addTestResultToDB(String uuid, Boolean result){
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }
        ContentValues values = new ContentValues();
        //write to basictests table
        if(result == null)
            values.put("result",2);
        else if(result)
            values.put("result",1);
        else
            values.put("result",0);
        db.update("basictests",values,"uuid = ?",new String[]{uuid});
    }

    public void addTestExceptionToDB(String uuid, String exception){
        if(exception == null)
            return;
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }
        ContentValues values = new ContentValues();
        //write to basictests table
        values.put("exception",exception);
        db.update("basictests",values,"uuid = ?",new String[]{uuid});
    }

    public Boolean getTestResult(String uuid){
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabaseReadOnly();
        }
        //basic test table info
        Cursor cursor = db.query(
                "basictests",   // The table to query
                new String[]{"result"},             // The array of columns to return (pass null to get all)
                "uuid = ?",              // The columns for the WHERE clause
                new String[]{uuid},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndex("result"));
        cursor.close();

        if(result == 2 || result == -1) //inconclusive or not performed yet
            return null;
        return (result == 1);
    }

    private String getTableNameForTestType(String testType) throws IllegalStateException{
        if(testType == null || testType.equals("")){
            throw new IllegalStateException("Invalid testType of basicTest!");
        }
        return "test_"+testType.toLowerCase();
    }

    public Vector<String> getAllBasicTestsUUIDs() throws JSONException {
        //make sure DB access is ready (read only)
        if (db == null || !db.isOpen()) {
            db = MsdDatabaseManager.getInstance().openDatabaseReadOnly();
        }

        //basic test table info
        Cursor cursor = db.query(
                "basictests",   // The table to query
                null,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        Log.d(Constants.LOG_TAG, mTAG + "" + cursor.getCount() + " basic tests in DB");
        if(cursor.getCount() > 0) {
            Vector<String> uuids = new Vector<String>();
            int uuidIndex = cursor.getColumnIndex("uuid");
            while (cursor.moveToNext()) {
                String uuid = cursor.getString(uuidIndex);
                uuids.add(uuid);
            }
            cursor.close();
            return uuids;
        }
        cursor.close();
        return null;
    }

    public Vector<JSONObject> getAllBasicTests() throws JSONException, UnsupportedEncodingException {
        //make sure DB access is ready (read only)
        if (db == null || !db.isOpen()) {
            db = MsdDatabaseManager.getInstance().openDatabaseReadOnly();
        }

        //basic test table info
        Cursor cursor = db.query(
                "basictests",   // The table to query
                null,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        Log.d(Constants.LOG_TAG, mTAG + "" + cursor.getCount() + " basic tests in DB");
        if(cursor.getCount() > 0) {
            Vector<JSONObject> basicTests = new Vector<JSONObject>();
            while (cursor.moveToNext()) {
                JSONObject basicTest = null;
                String[] columns = cursor.getColumnNames();
                String testType = null;
                for (String column : columns) {
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                basicTests.add(basicTest);
            }
            cursor.close();
            return basicTests;
        }
        cursor.close();
        return null;
    }

    public JSONObject getBasicTestByUUID(String uuid){
        if(uuid == null || uuid.equals("")){ //TODO improve sanity check here
            Log.e(Constants.LOG_TAG,mTAG+"Malformated UUID.");
        }
        //make sure DB access is ready (read only)
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabaseReadOnly();
        }
        Cursor cursor = null;
        try {
            //basic test table info
             cursor = db.query(
                    "basictests",   // The table to query
                    null,             // The array of columns to return (pass null to get all)
                    "uuid = ?",              // The columns for the WHERE clause
                    new String[]{uuid},          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    null               // The sort order
            );

            //Log.d(Constants.LOG_TAG,"DB Query: cursor: "+cursor.getCount()+" columns:"+ Arrays.toString(cursor.getColumnNames()));
            JSONObject basicTest = null;
            if(cursor.moveToFirst()) {
                basicTest = new JSONObject();
                basicTest.put("uuid", uuid);
                String[] columns = cursor.getColumnNames();
                String testType = null;
                for (String column : columns) {
                    if (column.equals("uuid"))
                        continue;
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
            }
            cursor.close();
            return basicTest;

        }catch(Exception e){
            Log.e(Constants.LOG_TAG,"Exception when retrieving basic test from DB",e);
            if(cursor != null)
                cursor.close();
        }
        return null;
    }

    public void resetAllBasicTests() {
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }
        ContentValues values = new ContentValues();
        //write to basictests table
        values.putNull("exception");
        values.put("result",-1);
        db.update("basictests",values,null,null);
    }

    public int getNumberOfTotalNotPerformedTests() { //basic test table info
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }
        Cursor cursor = db.query(
                "basictests",   // The table to query
                new String[]{"uuid"},             // The array of columns to return (pass null to get all)
                "result = ? and exception IS NULL",              // The columns for the WHERE clause
                new String[]{"-1"},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null
        );

        if(cursor == null)
            return -1;
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public void markBasicTestChunkSuccessful(String basicTestChunkURL) {
        if(basicTestChunkURL == null)
            return;
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }
        Log.d(Constants.LOG_TAG,"Marking basicTestChunkURL: "+basicTestChunkURL+" as downloaded and parsed successfully in DB");
        ContentValues values = new ContentValues();
        //write to basictests table
        values.put("url",basicTestChunkURL);
        values.put("successful",1);
        db.insertOrThrow("basictest_chunks", null, values);
    }

    public boolean wasBasicTestChunkSuccessful(String basicTestChunkURL) {
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = MsdDatabaseManager.getInstance().openDatabase();
        }
        Cursor cursor = db.query(
                "basictest_chunks",   // The table to query
                new String[]{"successful"},             // The array of columns to return (pass null to get all)
                "url = ? and successful = ?",              // The columns for the WHERE clause
                new String[]{basicTestChunkURL,""+1},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null
        );

        if(cursor == null)
            return false;
        if(cursor.getCount() == 1) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }
}