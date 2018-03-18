package de.srlabs.patchalyzer;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import junit.framework.TestResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class BasicTestCache {
    private final String testSuiteVersion;
    private int apiLevel;
    private static final int TEST_BATCH_SIZE = 1024;
    LinkedBlockingQueue<JSONObject> testQueue = new LinkedBlockingQueue<JSONObject>();
    LinkedBlockingQueue<BasicTestResult> resultQueue = new LinkedBlockingQueue<BasicTestResult>();
    private SharedPreferences sharedPrefs;
    private BasicTestParser database;
    private Context service;
    int progressTotal = 0;
    int progressDone = 0;
    final Object progressLock = new Object();
    private HashMap<String,String> exceptionsByTestId = new HashMap<String, String>();
    private ProgressItem progressItem;
    private Runnable finishedRunnable = null;
    private boolean stopTesting = false;

    public BasicTestCache(TestExecutorService service, String testSuiteVersion, int apiLevel){
        this.testSuiteVersion = testSuiteVersion;
        this.apiLevel = apiLevel;
        this.service = service;
        this.sharedPrefs = service.getSharedPreferences("BasicTestCache", Context.MODE_PRIVATE);
        this.database = new BasicTestParser(service);
        long currentBuildDate = TestUtils.getBuildDateUtc();

        // Invalidate cached results if the build fingerprint or the build timestamp (seconds since 1970) changes => Make sure that teste are repeated after a firmware upgrade
        // Also invalidate cache and rerun test if this app is updated.
        if(currentBuildDate != sharedPrefs.getLong("buildDateUtc", -1) ||
                !TestUtils.getBuildFingerprint().equals(sharedPrefs.getString("buildFingerprint", "INVALID")) ||
                !TestUtils.getBuildDisplayName().equals(sharedPrefs.getString("buildDisplayName", "INVALID")) ||
                ! ( Constants.APP_VERSION == sharedPrefs.getInt("appVersion", -1) )
                ) {
            clearCache();
        }
    }
    public void clearCache(){
        Log.i(Constants.LOG_TAG, "BasicTestCache.clearCache() called");
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.putLong("buildDateUtc", TestUtils.getBuildDateUtc());
        editor.putString("buildFingerprint", TestUtils.getBuildFingerprint());
        editor.putString("buildDisplayName", TestUtils.getBuildDisplayName());
        editor.putInt("appVersion", Constants.APP_VERSION);
        editor.commit();
        this.sharedPrefs = service.getSharedPreferences("BasicTestCache", Context.MODE_PRIVATE);

        resetDBInformation();
    }

    private void resetDBInformation() {
        // set all basic test exceptions back and reset all the result int values to -1
        database.resetAllBasicTests();
    }

    public Boolean getOrExecute(String uuid) throws JSONException, IOException {
        if(uuid.startsWith("!")){
            Boolean subtestResult = getOrExecute(uuid.substring(1));
            if(subtestResult == null)
                return null;
            return !subtestResult;
        }
        JSONObject basicTest = database.getBasicTestByUUID(uuid);
        if(basicTest.has("exception") && !basicTest.getString("exception").equals("")) {
            exceptionsByTestId.put(uuid, basicTest.getString("exception"));
            return null;
        }
        return basicTest.isNull("result") ? null : basicTest.getBoolean("result");
    }

    public void startTesting(ProgressItem progressItem, Runnable finishedRunnable){
        this.progressItem = progressItem;
        this.finishedRunnable = finishedRunnable;
        progressTotal = database.getNumberOfTotalNotPerformedTests();
        Log.d(Constants.LOG_TAG,""+progressTotal+" basicTests to be performed!");
        progressDone = 0;

        Log.i(Constants.LOG_TAG, "Setting progressTotal to " + progressTotal);
        startWorking();

    }

    public void startWorking() {
        stopTesting = false;
        new MasterWorkingThread(4).start();
    }

    public int getQueueSize(){
        return testQueue.size();
    }

    public JSONObject toJson() throws IOException, JSONException {
        JSONObject result = new JSONObject();
        Vector<String> uuids = database.getAllBasicTestsUUIDs();
        for(String uuid : uuids){
            Boolean subtestResult = getOrExecute(uuid);
            if(subtestResult == null) {
                result.put(uuid, JSONObject.NULL);
            } else {
                result.put(uuid, subtestResult);
            }
        }
        JSONObject exceptionsJson = new JSONObject(exceptionsByTestId);
        result.put("exceptions", exceptionsJson);
        result.put("testSuiteVersion", testSuiteVersion);
        result.put("apiLevel", apiLevel);
        return result;
    }

    public void stopTesting(){
        stopTesting = true;
    }

    /**
     * This thread is going to handle the DB connection and fill the working
     */
    class MasterWorkingThread extends Thread{

        private int nThreads = 0;
        Vector<RegularWorkingThread> workingThreads = null;

        public MasterWorkingThread(int nThreads){
            this.nThreads = nThreads;
        }

        @Override
        public void run(){

            addNewThreads();
            int testBatchSize = addNextMissingTestBatch();

            while(true){
                if(stopTesting){
                    break;
                }
                //check if results queue contains all results
                for(int i=0; i < testBatchSize; i++) {
                    try {
                        BasicTestResult testResult = resultQueue.take();
                        if (testResult.getException() != null) {
                            Log.d(Constants.LOG_TAG,"TestResult: "+testResult.getBasicTestUUID()+" exception:"+testResult.getException());
                            database.addTestExceptionToDB(testResult.getBasicTestUUID(), testResult.getException());
                        } else {
                            Log.d(Constants.LOG_TAG,"TestResult: "+testResult.getBasicTestUUID()+" result:"+testResult.getResult());
                            database.addTestResultToDB(testResult.getBasicTestUUID(), testResult.getResult());
                        }
                        updateTotalProgress();
                    }catch(InterruptedException e){
                        Log.e(Constants.LOG_TAG,"InterruptedException in MasterWorkingThread.run():"+e.getMessage());
                    }
                }
                testBatchSize = addNextMissingTestBatch();
                Log.d(Constants.LOG_TAG,"testBatchSize:"+testBatchSize);

                if(testBatchSize == 0){
                    break;
                }
            }
            terminateThreads();
            progressItem.update(1.0);
            if(!stopTesting) {
                finishedRunnable.run();
            }
        }

        private void terminateThreads(){
          Log.d(Constants.LOG_TAG,"terminating threads...");
          for(int i =0; i < nThreads; i++){
              try {
                  testQueue.add(new JSONObject("{\"STOPMARKER\":true}"));
              }catch(JSONException e){
                  Log.d(Constants.LOG_TAG,"JSONException while adding stopmarker to queue..."+e.getMessage());
              }
          }
          for(RegularWorkingThread thread : workingThreads){
              while(true) {
                  try {
                      thread.join();
                      Log.d(Constants.LOG_TAG, "Joined worker thread");
                      break;
                  } catch (InterruptedException e) {
                      Log.e(Constants.LOG_TAG, "InterruptedException in terminateThreads:"+e.getMessage());
                  }
              }
          }
          Log.d(Constants.LOG_TAG, "Joined all worker threads");
        }

        private void addNewThreads() {
            workingThreads = new Vector<>();
            resultQueue = new LinkedBlockingQueue<BasicTestResult>();
            Log.d(Constants.LOG_TAG,"Starting test threads:"+nThreads);
            for (int i = 0; i < nThreads; i++) {
                RegularWorkingThread thread = new RegularWorkingThread(service,testQueue,resultQueue);
                thread.start();
                workingThreads.add(thread);
            }
        }

        public int addNextMissingTestBatch() {
            int result = 0;
            Vector<JSONObject> basicTestBatch = database.getNotPerformedTests(TEST_BATCH_SIZE);
            for(JSONObject basicTest : basicTestBatch){
                Log.d(Constants.LOG_TAG,"Addind basic test to queue:"+basicTest.toString());
                testQueue.add(basicTest);
                result++;
            }
            return result;
        }

        private void updateTotalProgress() {
            synchronized (progressLock){
                progressDone += 1;
            }
            //Log.i(Constants.LOG_TAG, "BasicTestCache Thread evaluated " + uuid + " to " + result + "  progressDone=" + progressDone + "  progressTotal=" + progressTotal);
            if(progressTotal == 0){
                progressItem.update(1.0);
            } else {
                progressItem.update(((double) progressDone) / ((double) progressTotal));
            }
        }

    }

    /**
     * These threads will work through the test queue, pick a basic test execute it and report back the result to the masterworking thread
     */
    class RegularWorkingThread extends Thread{

        private Context context = null;
        private LinkedBlockingQueue<JSONObject> tasks = null;
        private Queue<BasicTestResult> results = null;

        public RegularWorkingThread(Context context, LinkedBlockingQueue<JSONObject> tasks, Queue<BasicTestResult> results){
            this.context = context;
            this.tasks = tasks;
            this.results = results;
        }

        @Override
        public void run(){
            while(true){
                try {
                    JSONObject basicTest = tasks.take();
                    if(basicTest.has("STOPMARKER")) {
                        Log.d(Constants.LOG_TAG," shutting down worker thread..");
                        return;
                    }
                    BasicTestResult result = performTest(basicTest);
                    synchronized (results) {
                        results.add(result);
                    }

                }catch(InterruptedException e){
                    Log.d(Constants.LOG_TAG,"InterruptedException while dequeuing from tasks: "+e.getMessage());
                }
            }
        }



        private BasicTestResult performTest(JSONObject basicTest){
            try {
                // run basic test and add result to result queue
                Log.d(Constants.LOG_TAG," working on task: "+basicTest.toString());
                Boolean result = null;
                String exception = null;
                try {
                    result = TestEngine.executeBasicTest(context, basicTest);
                } catch (Exception e) {
                    exception = e.getMessage();
                }
                if (exception == null) {
                    Boolean val = null;
                    if (result != null && result) {
                        val = true;
                    } else if (result != null && !result) {
                        val = false;
                    }
                    return new BasicTestResult(basicTest.getString("uuid"), val, null);

                } else {
                    return new BasicTestResult(basicTest.getString("uuid"), null, exception);
                }
            }catch(JSONException e){
                Log.e(Constants.LOG_TAG, "working thread:"+e.getMessage());
                return null;
            }
        }
    }
}
