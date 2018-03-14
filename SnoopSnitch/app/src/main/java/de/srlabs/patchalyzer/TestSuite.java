package de.srlabs.patchalyzer;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.ServiceConfigurationError;
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
    private String noCVETestsMessage = null;
    private File testSuiteFile = null;


    public TestSuite(Context context, File testsuiteJSONFile){
        this.context = context;
        this.testSuiteFile = testsuiteJSONFile;
        parseInfoFromJSON(testsuiteJSONFile);
    }

    public void addBasicTestsToDB() throws IllegalStateException{
        addBasicTestsToDB(testSuiteFile);
    }

    private void addBasicTestsToDB(File file) throws IllegalStateException{
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

    public void parseVulnerabilites(){
        if(testSuiteFile == null || !testSuiteFile.exists())
            throw new IllegalStateException("testSuite JSON file does not exist!");
        BasicTestParser parser = new BasicTestParser(context, testSuiteFile);
        parser.initReadingVulnerabilities();
        vulnerabilities = parser.parseVulnerabilities();
        Log.d(Constants.LOG_TAG,"vulnerabilities:"+vulnerabilities.toString());
        parser.finishReading();

    }
    private void parseInfoFromJSON(File testSuiteJSON){
        if(testSuiteJSON == null || !testSuiteJSON.exists())
            throw new IllegalStateException("testSuiteJSON file does not exist!");
        try{
            InputStream inputStream = new FileInputStream(testSuiteJSON);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));

            //read outer JSON object
            jsonReader.beginObject();

            while(jsonReader.hasNext()){
                    JsonToken jsonToken = jsonReader.peek();
                    Log.d(Constants.LOG_TAG,"parsing testsuite JSON -> token:"+jsonToken);
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
                                    case "vulnerabilitiesChunks":
                                        parseVulnerabilitiesChunks(jsonReader);
                                        break;
                                    case "basicTestsChunks":
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

        }catch(IOException e){
            Log.d(Constants.LOG_TAG,"TestSuite: parseInfoFromJSON error: "+e);
        }
    }

    private void parseVulnerabilitiesChunks(JsonReader jsonReader) {
        if(jsonReader != null){
            try {
                Set<String> vulnerabilitesChunks = new HashSet<>();
                ServerApi api = new ServerApi();
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    vulnerabilitesChunks.add(jsonReader.nextString());
                }
                jsonReader.endArray();
                for(String vulnerabilityChunk : vulnerabilitesChunks){
                    if(!api.isVulnerabilityChunkCached(context, vulnerabilityChunk)) {
                        //if not download file with .tmp suffix -> try to parse it to JSONObject -> without problems then rename file to erase .tmp suffix and replace with .json
                        if (!api.downloadVulnerabilityChunk(context, vulnerabilityChunk)) {
                            Log.e(Constants.LOG_TAG, "Downloading and parsing vulnerability chunk " + vulnerabilityChunk + " failed");
                            //FIXME repeat download process?!
                        }
                    }
                }

            }catch(IOException e){
                Log.e(Constants.LOG_TAG,"Exception while parsing vulnerabilities chunks from test suite: "+e.getMessage());
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
                    //already in DB?
                    if(!database.wasBasicTestChunkSuccessful(basicTestChunkURL)) {
                        try {
                            File chunkFile = api.downloadBasicTestChunk(context, basicTestChunkURL);
                            addBasicTestsToDB(chunkFile);
                            database.markBasicTestChunkSuccessful(basicTestChunkURL);
                        } catch (IOException | IllegalStateException e) {
                            Log.e(Constants.LOG_TAG, "Exception while downloading and parsing basic test chunk: " + basicTestChunkURL + " : " + e.getMessage());
                        }
                    }
                }
            }catch(IOException e){
                Log.e(Constants.LOG_TAG,"Exception while parsing basicTestChunks: "+e.getMessage());
            }
        }
    }

    public JSONObject getVulnerabilities(){
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
}
