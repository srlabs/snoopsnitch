package de.srlabs.patchalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/** This class contains all the testsuite meta information
 * Created by jonas on 02.03.18.
 */

public class TestSuite {

    private Context context;

    private int minAppVersion = -1;
    private String version = null;
    private String updradeUrl = null;
    private JSONObject vulnerabilities = null;
    private Set<String> vulnerabilitesChunks = null;
    private String noCVETestsMessage = null;
    private File testSuiteFile = null;
    private boolean emptyTestSuiteJSON = false;


    public TestSuite(Context context, File testsuiteJSONFile){
        this.context = context;
        this.testSuiteFile = testsuiteJSONFile;
    }

    public void addBasicTestsToDB() throws IllegalStateException{ //FIXME this is the old version, not using chunks
        addBasicTestsToDB(testSuiteFile);
    }

    private void addBasicTestsToDB(File file) throws IllegalStateException{ //FIXME this is the old version, not using chunks
        if(file == null || !file.exists())
            throw new IllegalStateException("JSON file does not exist!");
        BasicTestParser parser = new BasicTestParser(context,file);
        parser.initReadingBasicTests();
        JSONObject basicTest = null;

        // write all basic test to DB
        while ((basicTest = parser.getNextBasicTest()) != null) {
            try {
                //Log.d(Constants.LOG_TAG,"Trying to insert basic test (uuid:"+basicTest.getString("uuid")+"to DB!");
                parser.insertBasicTestToDB(basicTest);
            }catch(SQLiteConstraintException e){
                //ignore, cause this prevents redundant entries in the DB
                //Log.d(Constants.LOG_TAG,"-> basicTest already in DB!");
            }
        }
        parser.finishReading();
    }

    public void parseVulnerabilites(){ //FIXME this is the old version, not using chunks
        if(testSuiteFile == null || !testSuiteFile.exists())
            throw new IllegalStateException("testSuite JSON file does not exist!");
        BasicTestParser parser = new BasicTestParser(context, testSuiteFile);
        parser.initReadingVulnerabilities();
        vulnerabilities = parser.parseVulnerabilities();
        Log.d(Constants.LOG_TAG,"vulnerabilities:"+vulnerabilities.toString());
        parser.finishReading();

    }
    public void parseInfoFromJSON() throws IOException{ //TODO differentiate between IOException when parsing JSON or when downloading chunks
        if(testSuiteFile == null || !testSuiteFile.exists())
            throw new IllegalStateException("testSuiteJSON file does not exist!");
            InputStream inputStream = new FileInputStream(testSuiteFile);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));

            //read outer JSON object
            jsonReader.beginObject();
            if(jsonReader.peek().equals(JsonToken.END_OBJECT)){
                Log.e(Constants.LOG_TAG,"Got empty testsuite JSON from server!");
                emptyTestSuiteJSON = true;
                jsonReader.endObject();
                jsonReader.close();
                return;
            }

            while(jsonReader.hasNext()){
                    JsonToken jsonToken = jsonReader.peek();
                    //Log.d(Constants.LOG_TAG,"parsing testsuite JSON -> token:"+jsonToken);
                    switch (jsonToken) {
                        case NAME:
                                //skip internal objects ('basicTests' and 'vulnerabilities')
                                String name = jsonReader.nextName();
                                switch(name){
                                    case "noCVETestsMessage":
                                        setNoCVETestsMessage(jsonReader.nextString());
                                        break;
                                    case "version":
                                        setVersion(jsonReader.nextString());
                                        break;
                                    case "minAppVersion":
                                        setMinAppVersion(jsonReader.nextInt());
                                        break;
                                    case "upgradeUrl":
                                        setUpradeUrl(jsonReader.nextString());
                                        break;
                                    case "vulnerabilitiesUrls":
                                        parseVulnerabilitiesChunks(jsonReader);
                                        break;
                                    case "basicTestUrls":
                                        parseBasicTestChunks(jsonReader);
                                        break;
                                    case "basicTests":
                                    case "vulnerabilities":
                                        jsonReader.skipValue();
                                        break;
                                }
                                break;
                    }
            }
            jsonReader.endObject();
            jsonReader.close();
            writeInfoToSharedPrefs();
    }

    private void parseVulnerabilitiesChunks(JsonReader jsonReader) throws IOException{
        if(jsonReader != null){
            vulnerabilitesChunks = new HashSet<>();
            ServerApi api = new ServerApi();
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                vulnerabilitesChunks.add(jsonReader.nextString());
            }
            jsonReader.endArray();
            for(String vulnerabilityChunk : vulnerabilitesChunks){
                if(api.getVulnerabilityChunkCacheFile(context, vulnerabilityChunk) == null) {
                    //if not download file with .tmp suffix -> try to parse it to JSONObject -> without problems then rename file to erase .tmp suffix and replace with .json
                    if (api.downloadVulnerabilityChunk(context, vulnerabilityChunk) == null) {
                        throw new IOException("Downloading and parsing vulnerability chunk " + vulnerabilityChunk + " failed");
                    }
                }
            }

        }
    }

    private void parseBasicTestChunks(JsonReader jsonReader) {
        if(jsonReader != null){
            try {
                Set<String> basicTestChunkURLs = new HashSet<String>();
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    basicTestChunkURLs.add(jsonReader.nextString());
                }
                jsonReader.endArray();
                //for all chunks
                ServerApi api = new ServerApi();
                BasicTestParser database = new BasicTestParser(context);

                for(String basicTestChunkURL : basicTestChunkURLs){
                    Log.d(Constants.LOG_TAG,"Checking basic test chunk: "+basicTestChunkURL);
                    //already in DB?
                    if(!database.wasBasicTestChunkSuccessful(basicTestChunkURL)) {
                        Log.d(Constants.LOG_TAG,"Fetching basic test chunk :"+basicTestChunkURL);
                        try {
                            File chunkFile = api.downloadBasicTestChunk(context, basicTestChunkURL);
                            Log.d(Constants.LOG_TAG,"Adding all basic tests from chunk: "+chunkFile.getAbsolutePath()+" to DB...");
                            addBasicTestsToDB(chunkFile);
                            database.markBasicTestChunkSuccessful(basicTestChunkURL);
                        } catch (IOException | IllegalStateException e) {
                            Log.e(Constants.LOG_TAG, "Exception while downloading and parsing basic test chunk: " + basicTestChunkURL,e);
                        }
                    }
                }
            }catch(IOException e){
                Log.e(Constants.LOG_TAG,"Exception while parsing basicTestChunks: "+e.getMessage());
            }
        }
    }

    public JSONObject getVulnerabilities() throws IOException{
        if(vulnerabilities == null) {
            ServerApi api = new ServerApi();
            JSONObject allVulnerabilities = new JSONObject();
            for (String vulnerabilityChunk : vulnerabilitesChunks) {
                File chunkFile = api.getVulnerabilityChunkCacheFile(context, vulnerabilityChunk);
                if (chunkFile == null) {
                    Log.d(Constants.LOG_TAG, "Vulnerability chunk file missing: " + vulnerabilityChunk);
                    chunkFile = api.downloadVulnerabilityChunk(context, vulnerabilityChunk);
                    if (chunkFile == null) {
                        Log.e(Constants.LOG_TAG, "Downloading and parsing vulnerability chunk " + vulnerabilityChunk + " failed");
                        throw new IOException("Failed downloading and parsing vulnerability chunk: "+vulnerabilityChunk);
                        //FIXME repeat download process?!
                    }
                }
                BasicTestParser parser = new BasicTestParser(context, chunkFile);
                parser.initReadingVulnerabilities();
                parser.parseAndAddVulnerabilities(allVulnerabilities);
            }
            vulnerabilities = allVulnerabilities;
        }
        return vulnerabilities;
    }

    private void setNoCVETestsMessage(String message){
        this.noCVETestsMessage = message;
    }

    public String getVersion(){
        return version;
    }

    public int getMinAppVersion(){
        return minAppVersion;
    }

    public String getUpdradeUrl(){ return updradeUrl; }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setUpradeUrl(String upgradeUrl){
        this.updradeUrl = upgradeUrl;
    }

    private void setMinAppVersion(int minAppVersion) {
        this.minAppVersion = minAppVersion;
    }

    public String getNoCVETestMessage() {
        return noCVETestsMessage;
    }

    private void writeInfoToSharedPrefs(){
        if(!emptyTestSuiteJSON) {
            SharedPreferences.Editor editor = context.getSharedPreferences("TestSuite", Context.MODE_PRIVATE).edit();
            editor.putInt("minAppVersion", getMinAppVersion());
            editor.putString("version", getVersion());
            editor.commit();
        }
    }
}
