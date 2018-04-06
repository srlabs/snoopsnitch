package de.srlabs.patchalyzer.util;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.analysis.TestUtils;

/**
 * This class handles all the up- and downloads to/from the backend
 */
public class ServerApi {
    public static final String API_URL="https://snoopsnitch-api.srlabs.de/v1/";
    // For Testing:
    //public static final String API_URL="https://v22018043785564093.megasrv.de/v1/";

    public File downloadTestSuite(String filenamePrefix, Context context, String appid, int apiVersion, String currentVersion, int appVersion) throws JSONException, IOException{
        URL url = new URL(API_URL + "test/suite?appId=" + appid + "&androidApiVersion=" + apiVersion + "&testVersion=" + URLEncoder.encode(currentVersion, "UTF-8") + "&appVersion=" + appVersion + "&64bit="+ TestUtils.is64BitSystem());
        Log.i(Constants.LOG_TAG,"Downloading tests: "+url.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(100000);
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        File outputFile = new File(context.getCacheDir(), filenamePrefix+"_"+apiVersion+".json");
        try {
            // pipe testSuite JSON to cache file
            TestUtils.writeInputstreamToFile(connection.getInputStream(), outputFile);
        }finally{
            connection.disconnect();
        }
        return outputFile;
    }

    public File downloadBasicTestChunk(Context context, String urlString) throws IOException{
        URL url = new URL(urlString);
        Log.i(Constants.LOG_TAG,"Downloading basic test chunk: "+url.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(100000);
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        String[] parts = urlString.split("/");
        String chunkName = parts[parts.length-1];

        File outputFile = new File(context.getCacheDir(), chunkName);
        Log.d(Constants.LOG_TAG,"Saving basic test chunk file to :"+outputFile.getAbsolutePath());


        try {
            // pipe testSuite JSON to cache file
            TestUtils.writeInputstreamToFile(connection.getInputStream(), outputFile);
        }finally{
            connection.disconnect();
        }
        return outputFile;
    }

    public JSONArray getRequests(String appid, int apiVersion , String phoneModel, String romBuildFingerprint, String romDisplayName, long romBuildDate, int appVersion) throws JSONException, IOException {
        Log.i(Constants.LOG_TAG, "getRequests(appid=" + appid + ", phoneModel=" + phoneModel + ", romBuildFingerprint=" + romBuildFingerprint + ", romDisplayName=" + romDisplayName + ", romBuildDate=" + romBuildDate);
        URL url = new URL(API_URL + "get/requests?appId=" + URLEncoder.encode(appid,"UTF-8") + "&androidApiVersion=" + apiVersion +
                "&phoneModel=" + URLEncoder.encode(phoneModel, "UTF-8") + "&romBuildFingerprint=" + URLEncoder.encode(romBuildFingerprint, "UTF-8") +
                "&romDisplayName=" + URLEncoder.encode(romDisplayName, "UTF-8") + "&romBuildDate=" + romBuildDate + "&appVersion=" + appVersion);
        Log.i(Constants.LOG_TAG, "getRequests() URL: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("connection", "close");
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(3600*1000);
        connection.setInstanceFollowRedirects(false);
        connection.connect();
        int code = connection.getResponseCode();
        if(code == 200 || code == 304){
                // Read response
                BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                String line = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseStreamReader.close();
                connection.disconnect();
                Log.i(Constants.LOG_TAG, "getRequests received json: " + stringBuilder.toString());
            return new JSONArray(stringBuilder.toString());
        }
        // Read response
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getErrorStream())));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        connection.disconnect();
        throw new IllegalStateException("getRequests(): The server returned an invalid response code " + code + "  Response contents: " + stringBuilder.toString());
    }
    public void reportFile(String filename, String appid, String phoneModel, String romBuildFingerprint, String romDisplayName, long romBuildDate, int appVersion) throws JSONException, IOException {
        Log.i(Constants.LOG_TAG, "reportFile(appid=" + appid + ", phoneModel=" + phoneModel + ", romBuildFingerprint=" + romBuildFingerprint + ", romDisplayName=" + romDisplayName + ", romBuildDate=" + romBuildDate + ", filename=" + filename);
        URL url = new URL(API_URL + "report/file?appId=" + URLEncoder.encode(appid,"UTF-8") +
                "&phoneModel=" + URLEncoder.encode(phoneModel, "UTF-8") + "&romBuildFingerprint=" + URLEncoder.encode(romBuildFingerprint, "UTF-8") +
                "&romDisplayName=" + URLEncoder.encode(romDisplayName, "UTF-8") + "&romBuildDate=" + romBuildDate + "&appVersion=" + appVersion + "&filename=" + URLEncoder.encode(filename, "UTF-8"));
        Log.i(Constants.LOG_TAG, "reportFile() URL: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("connection", "close");
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(3600*1000);
        connection.setInstanceFollowRedirects(false);
        connection.setDoOutput(true);

        String boundary = generateBoundary();
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        DataOutputStream request = new DataOutputStream(connection.getOutputStream());
        request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"file\"\r\n\r\n");
        //request.writeBytes(sysinfo.toString() + "\r\n");
        FileInputStream fis = new FileInputStream(filename);
        byte[] buf = new byte[32*1024];
        while(true){
            int bytesRead = fis.read(buf);
            if(bytesRead <= 0)
                break;
            request.write(buf, 0, bytesRead);
        }
        request.writeBytes("\r\n");

        request.writeBytes("--" + boundary + "--\r\n");
        request.flush();
        request.close();
        Log.i(Constants.LOG_TAG, "reportFile(): Finished writing request");
        int code = connection.getResponseCode();
        if(code == 200 || code == 304){
            connection.disconnect();
            return;
        }
        // Read response
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getErrorStream())));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        connection.disconnect();
        throw new IllegalStateException("reportFile(): The server returned an invalid response code " + code + "  Response contents: " + stringBuilder.toString());
    }
    public void reportSys(JSONObject sysinfo, String appid, String phoneModel, String romBuildFingerprint, String romDisplayName, long romBuildDate, int appVersion) throws JSONException, IOException {
        Log.i(Constants.LOG_TAG, "reportSys(appid=" + appid + ", phoneModel=" + phoneModel + ", romBuildFingerprint=" + romBuildFingerprint + ", romDisplayName=" + romDisplayName + ", romBuildDate=" + romBuildDate + ", uploadSize=" + sysinfo.toString().length());
        URL url = new URL(API_URL + "report/system?appId=" + URLEncoder.encode(appid,"UTF-8") +
                "&phoneModel=" + URLEncoder.encode(phoneModel, "UTF-8") + "&romBuildFingerprint=" + URLEncoder.encode(romBuildFingerprint, "UTF-8") +
                "&romDisplayName=" + URLEncoder.encode(romDisplayName, "UTF-8") + "&romBuildDate=" + romBuildDate + "&appVersion=" + appVersion);
        Log.i(Constants.LOG_TAG, "reportSys() URL: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("connection", "close");
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(3600*1000);
        connection.setInstanceFollowRedirects(false);
        connection.setDoOutput(true);
        String boundary = generateBoundary();
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        DataOutputStream request = new DataOutputStream(connection.getOutputStream());
        request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"systemData\"; filename=\"systemData\"\r\n\r\n");
        //request.writeBytes(sysinfo.toString() + "\r\n");
        request.write(sysinfo.toString().getBytes("UTF8"));
        request.writeBytes("\r\n");

        request.writeBytes("--" + boundary + "--\r\n");
        request.flush();
        request.close();
        Log.i(Constants.LOG_TAG, "reportSys(): Finished writing request");
        int code = connection.getResponseCode();
        Log.i(Constants.LOG_TAG, "reportSys code=" + code);
        if(code == 200 || code == 304){
            connection.disconnect();
            return;
        }
        // Read response
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getErrorStream())));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        connection.disconnect();
        throw new IllegalStateException("reportSys(): The server returned an invalid response code " + code + "  Response contents: " + stringBuilder.toString());
    }
    public void reportTest(JSONObject testData, String appid, String phoneModel, String romBuildFingerprint, String romDisplayName, long romBuildDate, int appVersion) throws JSONException, IOException {
        Log.i(Constants.LOG_TAG, "reportTest(appid=" + appid + ", phoneModel=" + phoneModel + ", romBuildFingerprint=" + romBuildFingerprint + ", romDisplayName=" + romDisplayName + ", romBuildDate=" + romBuildDate);
        URL url = new URL(API_URL + "report/test?appId=" + URLEncoder.encode(appid,"UTF-8") +
                "&phoneModel=" + URLEncoder.encode(phoneModel, "UTF-8") + "&romBuildFingerprint=" + URLEncoder.encode(romBuildFingerprint, "UTF-8") +
                "&romDisplayName=" + URLEncoder.encode(romDisplayName, "UTF-8") + "&romBuildDate=" + romBuildDate + "&appVersion=" + appVersion);
        Log.i(Constants.LOG_TAG, "reportTest() URL: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(3600*1000);
        connection.setInstanceFollowRedirects(false);
        connection.setDoOutput(true);
        String boundary = generateBoundary();
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        DataOutputStream request = new DataOutputStream(connection.getOutputStream());
        request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"testData\"; filename=\"testData\"\r\n\r\n");
        request.writeBytes(testData.toString() + "\r\n");
        request.writeBytes("--" + boundary + "--\r\n");
        request.flush();
        request.close();
        Log.i(Constants.LOG_TAG, "reportTest(): Finished writing request");
        int code = connection.getResponseCode();
        Log.i(Constants.LOG_TAG, "reportTest: code=" + code);
        if(code == 200 || code == 304){
            connection.disconnect();
            return;
        }
        // Read response
        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getErrorStream())));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        connection.disconnect();
        throw new IllegalStateException("reportTest(): The server returned an invalid response code " + code + "  Response contents: " + stringBuilder.toString());
    }
    private String generateBoundary(){
        SecureRandom sr = new SecureRandom();
        byte[] random = new byte[16];
        sr.nextBytes(random);
        return TestUtils.byteArrayToHex(random);
    }

    public File downloadVulnerabilityChunk(Context context, String urlString) throws IOException{
        URL url = new URL(urlString);
        Log.i(Constants.LOG_TAG,"Downloading vulnerability chunk: "+url.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(100000);
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        String[] parts = urlString.split("/");
        String chunkName = parts[parts.length-1];

        StringBuilder stringBuilder = new StringBuilder();
        String jsonString = null;
        try {
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
            String line = "";
            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();

            //try to parse to JSONObject
            jsonString = stringBuilder.toString();
            new JSONObject(jsonString);

            File outputFile = new File(context.getCacheDir(),chunkName);
            TestUtils.writeStringToFile(jsonString, outputFile);

            connection.disconnect();

            return outputFile;
        }catch(JSONException e){
            Log.e(Constants.LOG_TAG,"Exception while downloading and parsing vulnerability chunk to JSON: "+e.getMessage());
            //writeStringToFile(jsonString, new File(context.getDataDir(), chunkName+".tmp"));
        }
        connection.disconnect();
        return null;
    }

    public File getVulnerabilityChunkCacheFile(Context context, String urlString) throws IOException{
        String[] parts = urlString.split("/");
        String chunkName = parts[parts.length-1];
        File file = new File(context.getCacheDir(),chunkName);
        if(!file.exists()) {
            Log.d(Constants.LOG_TAG,"Cached vulnerability chunk file does not exists here:"+file.getAbsolutePath());
            return null;
        }
        return file;
    }
}

