package de.srlabs.patchalyzer;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import de.srlabs.patchalyzer.Constants.ActivityState;
import de.srlabs.patchalyzer.analysis.PatchalyzerService;
import de.srlabs.patchalyzer.analysis.TestUtils;
import de.srlabs.patchalyzer.helpers.NotificationHelper;
import de.srlabs.patchalyzer.helpers.SharedPrefsHelper;
import de.srlabs.patchalyzer.views.PatchalyzerSumResultChart;
import de.srlabs.patchalyzer.views.PatchlevelDateOverviewChart;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;


public class PatchalyzerMainActivity extends FragmentActivity {
    private Handler handler;
    private Button startTestButton;
    private TextView errorTextView, percentageText;
    private WebView legendView;
    private ScrollView webViewContent, metaInfoTextScrollView;
    private ProgressBar progressBar;
    private LinearLayout progressBox;
    private PatchalyzerSumResultChart resultChart;

    private ITestExecutorServiceInterface mITestExecutorService;

    private boolean isServiceBound=false;
    private boolean noInternetDialogShowing = false;
    private String currentPatchlevelDate; // Only valid in ActivityState.VULNERABILITY_LIST
    private boolean restoreStatePending = false;
    private String noCVETestsForApiLevelMessage = null;
    private static final int SDCARD_PERMISSION_RCODE = 1;
    private TestCallbacks callbacks = new TestCallbacks();
    private boolean isActivityActive = false;

    private ActivityState lastActiveState = null;
    private ActivityState nonPersistentState = ActivityState.PATCHLEVEL_DATES;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.callbacks = new TestCallbacks();
        Log.d(Constants.LOG_TAG, "onCreate() called");
        handler = new Handler(Looper.getMainLooper());
        setContentView(R.layout.activity_patchalyzer);
        startTestButton = (Button) findViewById(R.id.btnDoIt);
        webViewContent = (ScrollView) findViewById(R.id.scrollViewTable);
        errorTextView = (TextView) findViewById(R.id.errorText);
        percentageText = (TextView) findViewById(R.id.textPercentage);
        legendView = (WebView) findViewById(R.id.legend);
        metaInfoTextScrollView = (ScrollView) findViewById(R.id.scrollViewText);
        //metaInfoTextScrollView.setBackgroundColor(Color.TRANSPARENT);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        resultChart = (PatchalyzerSumResultChart) findViewById(R.id.sumResultChart);
        progressBox = (LinearLayout) findViewById(R.id.progress_box);
        errorTextView.setText("");
        percentageText.setText("");
        ActionBar actionBar = getActionBar();


        String title = this.getResources().getString(R.string.patchalyzer_label_long);
        if(!Constants.IS_TEST_MODE) {
            actionBar.setSubtitle("\nApp ID: "+ TestUtils.getAppId(this));
        }else{
            title += "- TESTMODE";
        }
        actionBar.setTitle(title);

        // see onOptionsItemSelected
        actionBar.setDisplayHomeAsUpEnabled(true);

        displayCutline(null);

        // This is not persisted right now
        if (savedInstanceState != null) {
            currentPatchlevelDate = savedInstanceState.getString("currentPatchlevelDate");
        }

        if(TestUtils.isTooOldAndroidAPIVersion()){
            startTestButton.setEnabled(false);
            progressBox.setVisibility(View.GONE);
            showMetaInformation(this.getResources().getString(R.string.patchalyzer_too_old_android_api_level),null);
        }
        else {
            initDatabase();
        }
    }

    private void showErrorMessageInMetaInformation(String errorMessage) {
        String html = "<p style=\"font-weight:bold;\">" + getResources().getString(R.string.patchalyzer_sticky_error_message_start)
                + "</p>";
        showMetaInformation(html,errorMessage);
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mITestExecutorService = ITestExecutorServiceInterface.Stub.asInterface(service);
            try{
                mITestExecutorService.updateCallback(callbacks);
            } catch (RemoteException e) {
                Log.e(Constants.LOG_TAG, "RemoteException in onServiceConnected():", e);
            }
            Log.d(Constants.LOG_TAG,"Service connected!");
            isServiceBound = true;
            restoreState();
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(Constants.LOG_TAG, "Service has unexpectedly disconnected");
            isServiceBound = false;
            mITestExecutorService = null;
        }
    };

    private String getWebViewFontStyle() {
        return "<head><style type=\"text/css\">body { " +
                    "    font-family: sans-serif-condensed;\n" +
                    "    font-size: 11sp;\n" +
                    "    font-color: #58585b;\n" +
                    "    text-align: justify;\n" +
                    "}</style></head>";
    }

    class TestCallbacks extends ITestExecutorCallbacks.Stub{
        @Override
        public void showErrorMessage(final String text) throws RemoteException {
            handler.post(new Runnable(){
                @Override
                public void run() {
                    if (isActivityActive) {
                        restoreState();
                        progressBox.setVisibility(View.INVISIBLE);
                        if(text.equals(PatchalyzerService.NO_INTERNET_CONNECTION_ERROR)){
                            showNoInternetConnectionDialog();
                        } else {
                            errorTextView.setText(text);
                        }
                    }
                }
            });
        }


        @Override
        public void updateProgress(final double progressPercent) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchalyzerMainActivity received updateProgress(" + progressPercent + ")");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isActivityActive) {
                        progressBar.setMax(1000);
                        progressBar.setProgress((int) (progressPercent * 1000.0));
                        String percentageString = ""+progressPercent*100.0;
                        if(percentageString.length() > 4){
                            percentageString = percentageString.substring(0, 4);
                            if (percentageString.endsWith(".")) {
                                percentageString = percentageString.substring(0, percentageString.length() - 1);
                            }
                        }
                        percentageText.setText(percentageString+"%");
                    }
                }
            });
        }
        @Override
        public void reloadViewState() throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchalyzerMainActivity received reloadViewState()");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isActivityActive) {
                        restoreState();
                    }
                }
            });
        }
        @Override
        public void finished(final String analysisResultString, final boolean isBuildCertified) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchalyzerMainActivity received finished()");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    JSONObject resultJSON = null;
                    try {
                        resultJSON = new JSONObject(analysisResultString);
                    } catch (JSONException e) {
                        Log.d(Constants.LOG_TAG,"Could not parse JSON from SharedPrefs. Returning null");
                    }
                    resultChart.setAnalysisRunning(false);
                    PatchalyzerSumResultChart.setResultToDrawFromOnNextUpdate(resultJSON);
                    SharedPrefsHelper.saveAnalysisResultNonPersistent(resultJSON, isBuildCertified);
                    if (isActivityActive) {
                        restoreState();
                    }
                }
            });
        }
        @Override
        public void handleFatalError(final String stickyErrorMessage) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchalyzerMainActivity received handleFatalError()");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    startTestButton.setEnabled(false);
                    resultChart.setAnalysisRunning(false);
                    SharedPrefsHelper.saveStickyErrorMessage(stickyErrorMessage, PatchalyzerMainActivity.this);
                    NotificationHelper.showAnalysisFailedNotification(PatchalyzerMainActivity.this);
                    if (isActivityActive) {
                        restoreState();
                    }
                }
            });
        }
        @Override
        public void showNoCVETestsForApiLevel(String message) throws RemoteException{
            Log.i(Constants.LOG_TAG,"PatchalyzerMainActivity received showNoCVETestsForApiLevel()");
            noCVETestsForApiLevelMessage = message;
        }
    }
    public void showMetaInformation(String status, String explain){
        metaInfoTextScrollView.removeAllViews();
        WebView wv = new WebView(PatchalyzerMainActivity.this);
        String html = "<html>"+getWebViewFontStyle()+"<body>\n";
        if(status != null)
            html += "\t" + status;
        if(explain != null)
            html += "<br/>" + explain;
        html += "</body></html>\n";
        Log.i(Constants.LOG_TAG,"Meta information text:\n"+html);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.loadData(html, "text/html; charset=utf-8","utf-8");
        metaInfoTextScrollView.addView(wv);
    }
    public void displayCutline(HashMap<String,PatchalyzerSumResultChart.ResultPart> results){
        String html = "<html>" + getWebViewFontStyle() + "<body>\n" +
                "<table style=\"border:0px collapse;\">";
        if(results == null) {
            html +=
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_PATCHED) + "\">" + this.getResources().getString(R.string.patchalyzer_patched) +
                            "</span></td><td></td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_MISSING) + "\">" + this.getResources().getString(R.string.patchalyzer_patch_missing) +
                            "</span></td><td></td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTCLAIMED) + "\">" + this.getResources().getString(R.string.patchalyzer_after_claimed_patchlevel) +
                            "</span></td><td></td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_INCONCLUSIVE) + "\">" + this.getResources().getString(R.string.patchalyzer_inconclusive) +
                            "</span></td><td></td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTAFFECTED) + "\">" + this.getResources().getString(R.string.patchalyzer_not_affected) +
                            "</span></td><td></td></tr>";
        }else if(results.size() == 5 && results.containsKey("patched") && results.containsKey("missing") && results.containsKey("notClaimed") &&
                results.containsKey("inconclusive") && results.containsKey("notAffected")){
            //display number of results for each category
            html +=
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_PATCHED) + "\">" + this.getResources().getString(R.string.patchalyzer_patched) +
                    "</span></td><td style=\"text-align:right;\">"+results.get("patched").getCount()+"</td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_MISSING) + "\">" + this.getResources().getString(R.string.patchalyzer_patch_missing) +
                    "</span></td><td style=\"text-align:right;\">"+results.get("missing").getCount()+"</td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTCLAIMED) + "\">" + this.getResources().getString(R.string.patchalyzer_after_claimed_patchlevel) +
                    "</span></td><td style=\"text-align:right;\">"+results.get("notClaimed").getCount()+"</td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_INCONCLUSIVE) + "\">" + this.getResources().getString(R.string.patchalyzer_inconclusive) +
                    "</span></td><td style=\"text-align:right;\">"+results.get("inconclusive").getCount()+"</td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTAFFECTED) + "\">" + this.getResources().getString(R.string.patchalyzer_not_affected) +
                    "</span></td><td style=\"text-align:right;\">"+results.get("notAffected").getCount()+"</td></tr>";
        }
        else{
            Log.e(Constants.LOG_TAG,"displayCutline: Result information missing!");
        }

        html += "</table>\n" +
                "</body></html>";
        legendView.setBackgroundColor(Color.TRANSPARENT);
        legendView.loadUrl("about:blank");
        legendView.loadDataWithBaseURL(Constants.WEBVIEW_URL_LOADDATA, html,"text/html; charset=utf-8","utf-8", null);
        //legendView.reload();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(upIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initDatabase(){
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(Constants.LOG_TAG,"Creating SQLite database...");
                    MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(PatchalyzerMainActivity.this));
                    MsdDatabaseManager msdDatabaseManager = MsdDatabaseManager.getInstance();
                    SQLiteDatabase db = msdDatabaseManager.openDatabase();
                    //testing DB init
                    Cursor cursor = db.rawQuery("SELECT * FROM basictests", null);
                    Log.d(Constants.LOG_TAG,"Got "+cursor.getCount()+" test entries!");
                    cursor.close();
                    msdDatabaseManager.closeDatabase();

                }catch(SQLException e){
                    // Testing if the DB creation worked successfully failed
                    Log.e(Constants.LOG_TAG,"DB creation failed, maybe App assets are corrupted: "+ e.getMessage());
                }
            }
        };
        t.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(!TestUtils.isTooOldAndroidAPIVersion()) {
            Intent intent = new Intent(PatchalyzerMainActivity.this, PatchalyzerService.class);
            intent.setAction(ITestExecutorServiceInterface.class.getName());
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        isActivityActive = true;
        restoreState();
    }



    private void startServiceIfNotRunning(){
        try {
            if (mITestExecutorService == null || !mITestExecutorService.isAnalysisRunning()) {
                Intent intent = new Intent(PatchalyzerMainActivity.this, PatchalyzerService.class);
                intent.setAction(ITestExecutorServiceInterface.class.getName());
                startService(intent);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }
        } catch (RemoteException e) {
            Log.d(Constants.LOG_TAG,"RemoteException in startServiceIfNotRunning" , e);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(isServiceBound)
            unbindService(mConnection);
        isActivityActive = false;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        SharedPrefsHelper.clearSavedStickyErrorMessage(PatchalyzerMainActivity.this);
    }


    private void restoreState(){
        NotificationHelper.cancelNonStickyNotifications(this);
        ActivityState tempNonPersistentState = nonPersistentState;
        try {
            if (mITestExecutorService != null && mITestExecutorService.isAnalysisRunning()) {
                // Analysis is running, show progress bar
                setButtonCancelAnalysis();
                progressBox.setVisibility(View.VISIBLE);
                resultChart.setVisibility(View.GONE);
                resultChart.setAnalysisRunning(true);
                webViewContent.setVisibility(View.INVISIBLE);
                displayCutline(null);
                showMetaInformation(getResources().getString(R.string.patchalyzer_sum_result_chart_analysis_in_progress), getResources().getString(R.string.patchalyzer_meta_info_analysis_in_progress));
            } else {
                // Analysis is not running
                resultChart.setAnalysisRunning(false);
                setButtonStartAnalysis();
                progressBox.setVisibility(View.GONE);
                if (SharedPrefsHelper.getAnalysisResult(this) == null) {
                    // No analysis result available
                    resultChart.setVisibility(View.GONE);
                    webViewContent.setVisibility(View.INVISIBLE);
                    String stickyErrorMessage = SharedPrefsHelper.getStickyErrorMessage(this);
                    displayCutline(null);
                    if (stickyErrorMessage != null) {
                        // Last analysis failed recently
                        PatchalyzerMainActivity.this.showErrorMessageInMetaInformation(stickyErrorMessage);
                    } else {
                        // No analysis executed yet, show no error message
                        showMetaInformation(this.getResources().getString(R.string.patchalyzer_claimed_patchlevel_date)+": "
                                + TestUtils.getPatchlevelDate(),this.getResources().getString(R.string.patchalyzer_no_test_result)+"!");
                    }

                } else {
                    // Previous analysis result available, show results table
                    resultChart.setVisibility(View.VISIBLE);
                    webViewContent.setVisibility(View.VISIBLE);
                    showPatchlevelDateNoTable();//should also update the cutline info
                }
            }
        } catch (RemoteException e) {
            Log.d(Constants.LOG_TAG,"RemoteException in restoreState" , e);
        }
        if(tempNonPersistentState == ActivityState.VULNERABILITY_LIST) {
            // show vulnerability details for a specific category
            showDetailsNoTable(currentPatchlevelDate);
        }
    }

    private void setButtonStartAnalysis() {
        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTestButton.setEnabled(false);
                startTest();
            }
        });
        startTestButton.setText(getResources().getString(R.string.patchalyzer_button_start_analysis));
        startTestButton.setEnabled(true);
    }

    private void setButtonCancelAnalysis() {
        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerCancelAnalysis();
            }
        });
        startTestButton.setText(getResources().getString(R.string.patchalyzer_button_cancel_analysis));
        startTestButton.setEnabled(true);
    }

    private void triggerCancelAnalysis() {
        startTestButton.setEnabled(false);
        resultChart.setAnalysisRunning(false);
        ITestExecutorServiceInterface temp = PatchalyzerMainActivity.this.mITestExecutorService;
        try {
            if (temp != null && temp.isAnalysisRunning()) {
                temp.requestCancelAnalysis();
            }
        } catch (RemoteException e) {
            Log.e(Constants.LOG_TAG, "RemoteException in triggerCancelAnalysis:", e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("currentPatchlevelDate", currentPatchlevelDate);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void startTest(){
        if(!TestUtils.isTooOldAndroidAPIVersion()) {
            SharedPrefsHelper.clearSavedAnalysisResult(this);
            SharedPrefsHelper.clearSavedStickyErrorMessage(this);
            resultChart.resetCounts();
            resultChart.invalidate();

            if (TestUtils.isConnectedToInternet(this)) {
                noCVETestsForApiLevelMessage = null;
                clearTable();
                if (Constants.IS_TEST_MODE && !requestSdcardPermission()) {
                    return;
                }
                startServiceIfNotRunning();
                resultChart.setAnalysisRunning(true);
                // restoreState should be called via callback
            } else {
                //no internet connection
                Log.w(Constants.LOG_TAG, "Not testing, because of missing internet connection.");
                showNoInternetConnectionDialog();
                restoreState();
            }
        }
    }

    private boolean requestSdcardPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                SDCARD_PERMISSION_RCODE);
        return false;
    }
    private void clearTable(){
        webViewContent.removeAllViews();
    }



    private void showPatchlevelDateNoTable(){
        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable()");
        webViewContent.removeAllViews();
        resultChart.resetCounts();
        //Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable(): w=" + webViewContent.getWidth() + "  h=" + webViewContent.getHeight() + "  innerW=" + webViewContent.getChildAt(0).getWidth() + "  innerH=" + webViewContent.getChildAt(0).getHeight());
        try{
            JSONObject testResults = SharedPrefsHelper.getAnalysisResult(this);
            String metaInfo = this.getResources().getString(R.string.patchalyzer_claimed_patchlevel_date)+": <b>" + refPatchlevelDate +"</b>";
            if(SharedPrefsHelper.getAnalysisResult(this) == null) {
                showMetaInformation(metaInfo,null);
                displayCutline(null);
                return;
            }
            if (SharedPrefsHelper.isBuildFromLastAnalysisCertified(this)) {
                metaInfo += " " + this.getResources().getString(R.string.patchalyzer_certified_build);
            }
            showMetaInformation(metaInfo,null);
            Vector<String> categories = new Vector<String>();
            Iterator<String> categoryIterator = testResults.keys();
            while (categoryIterator.hasNext())
                categories.add(categoryIterator.next());
            Collections.sort(categories);
            LinearLayout rows = new LinearLayout(this);
            rows.setOrientation(LinearLayout.VERTICAL);
            rows.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));


            for (final String category : categories) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                Button button = (Button) getLayoutInflater().inflate(R.layout.custom_button, null);
                String truncatedCategory = category;
                if (category.startsWith("201")) {
                    truncatedCategory = category.substring(0, 7);
                }
                else if(category.equals("other")){
                    truncatedCategory = "General";
                }
                button.setText(truncatedCategory);
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDetailsNoTable(category);
                    }
                });

                row.addView(button);
                JSONArray vulnerabilitiesForCategory = testResults.getJSONArray(category);

                Vector<Integer> statusColors = new Vector<Integer>();
                int numPatched = 0, numMissing = 0, numInconclusive = 0, numNotAffected = 0, numNotClaimed = 0;

                for (int i = 0; i < vulnerabilitiesForCategory.length(); i++) {
                    JSONObject vulnerability = vulnerabilitiesForCategory.getJSONObject(i);

                    int color = getVulnerabilityIndicatorColor(vulnerability, category);
                    statusColors.add(color);
                    switch(color){
                        case Constants.COLOR_PATCHED:
                            numPatched++;
                            break;
                        case Constants.COLOR_INCONCLUSIVE:
                            numInconclusive++;
                            break;
                        case Constants.COLOR_MISSING:
                            numMissing++;
                            break;
                        case Constants.COLOR_NOTAFFECTED:
                            numNotAffected++;
                            break;
                        case Constants.COLOR_NOTCLAIMED:
                            numNotClaimed++;
                            break;
                    }
                }

                //set result chart
                resultChart.increasePatched(numPatched);
                resultChart.increaseInconclusive(numInconclusive);
                resultChart.increaseMissing(numMissing);
                resultChart.increaseNotAffected(numNotAffected);
                resultChart.increaseNotClaimed(numNotClaimed);

                int[] tmp = new int[statusColors.size()];
                for (int i = 0; i < statusColors.size(); i++) {
                    tmp[i] = statusColors.get(i);
                }
                PatchlevelDateOverviewChart chart = new PatchlevelDateOverviewChart(this, tmp);
                row.addView(chart);
                rows.addView(row);
            }

            webViewContent.addView(rows);
            nonPersistentState = ActivityState.PATCHLEVEL_DATES;

            if(noCVETestsForApiLevelMessage != null){
                showNoCVETestsForApiLevelDialog(noCVETestsForApiLevelMessage);
            }

            resultChart.invalidate();
            resultChart.setVisibility(View.VISIBLE);
            progressBox.setVisibility(View.GONE);

            //update counts in cutline
            displayCutline(resultChart.getParts());

        } catch(Exception e){
            Log.e(Constants.LOG_TAG, "showPatchlevelDateTable Exception", e);
        }
    }
    private void showDetailsNoTable(String category){

        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        int numAffectedVulnerabilities = 0;
        try{
            JSONObject testResults = SharedPrefsHelper.getAnalysisResult(this);
            if(testResults == null){
                showMetaInformation(this.getResources().getString(R.string.patchalyzer_claimed_patchlevel_date)+": " + refPatchlevelDate,this.getResources().getString(R.string.patchalyzer_no_test_result)+"!");
                return;
            }
            JSONArray vulnerabilitiesForPatchlevelDate = testResults.getJSONArray(category);

            WebView wv = new WebView(PatchalyzerMainActivity.this);
            StringBuilder html = new StringBuilder();
            html.append("<html>"+getWebViewFontStyle()+"<body><table style=\"border-collapse:collapse;\">\n");

            for(int i=0;i<vulnerabilitiesForPatchlevelDate.length();i++) {
                JSONObject vulnerability = vulnerabilitiesForPatchlevelDate.getJSONObject(i);
                String identifier = vulnerability.getString("identifier");
                String identifierColor = toColorString(getVulnerabilityIndicatorColor(vulnerability, category));
                String description = vulnerability.getString("title");
                html.append("<tr>");

                if(category.equals("other")){
                    html.append("<td>");
                    html.append("<p style=\"background-color:"+identifierColor+";white-space: nowrap; margin:5px 0; padding: 5px;\">");
                }
                else{
                    html.append("<td style=\"border-bottom: 1px solid #ddd;\">");
                    html.append("<p style=\"background-color:"+identifierColor+";white-space: nowrap; margin-right:10px; padding: 5px;\">");
                }

                html.append(identifier);
                html.append("</p></td>");
                html.append("<td style=\"border-bottom: 1px solid #ddd;padding:5px 0;\">");
                html.append("<p>");
                html.append(description);
                html.append("</p></td>");
                html.append("</tr>");
                numAffectedVulnerabilities++;
            }
            html.append("</table></body></html>");
            wv.setBackgroundColor(Color.TRANSPARENT);
            wv.loadData(html.toString(), "text/html; charset=utf-8","utf-8");
            webViewContent.removeAllViews();
            webViewContent.addView(wv);

            showCategoryMetaInfo(category,numAffectedVulnerabilities);

            nonPersistentState = ActivityState.VULNERABILITY_LIST;
            currentPatchlevelDate = category;
        } catch(Exception e){
            Log.e(Constants.LOG_TAG, "showDetailsNoTable Exception", e);
        }
    }
    private void showCategoryMetaInfo(String category, int numCVEs) {
        StringBuilder infoText = new StringBuilder();
        if(!category.equals("other")) {
            infoText.append("<span style=\"font-weight:bold;\">" + category);
            infoText.append("</span><span>: " + numCVEs + " CVEs total</span>");
        }else{
            infoText.append("<span style=\"font-weight:bold;\">"+this.getResources().getString(R.string.patchalyzer_general_tests));
            infoText.append("</span><span>: " + numCVEs + " tests total</span>");
        }
        showMetaInformation(infoText.toString(),null);
    }

    /**
     * Assign color for test result for certain vulnerability
     * @param vulnerability JSON representation of vulnerability test result
     * @param refPatchlevelDate reference patch level date
     * @return Color representation in int
     */
    public static int getVulnerabilityIndicatorColor(JSONObject vulnerability, String refPatchlevelDate) {
        try {
            if (vulnerability.isNull("fixed") || vulnerability.isNull("vulnerable") || vulnerability.isNull("notAffected")) {
                return Constants.COLOR_INCONCLUSIVE;
            } else if(!vulnerability.isNull("notAffected") && vulnerability.getBoolean("notAffected")){
                return Constants.COLOR_NOTAFFECTED;
            } else if (vulnerability.getBoolean("fixed") && !vulnerability.getBoolean("vulnerable")) {
                return Constants.COLOR_PATCHED;
            } else if (!vulnerability.getBoolean("fixed") && vulnerability.getBoolean("vulnerable")) {
                if (TestUtils.isValidDateFormat(refPatchlevelDate) && !TestUtils.isPatchDateClaimed(refPatchlevelDate))
                    return Constants.COLOR_NOTCLAIMED;
                return Constants.COLOR_MISSING;
            }
        }catch(JSONException e){
            Log.e(Constants.LOG_TAG,"Problem assigning color for tests",e);
        }
        // default color
        return Constants.COLOR_INCONCLUSIVE;
    }
    private String toColorString(int color){
        String hexColorString = Integer.toHexString(color).toUpperCase();
        if(hexColorString.length() == 8){
            hexColorString = hexColorString.substring(2,8);
        }
        return "#"+hexColorString;
    }
    @Override
    public void onBackPressed(){
        if(nonPersistentState == ActivityState.VULNERABILITY_LIST)
            showPatchlevelDateNoTable();
        else
            super.onBackPressed();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        restoreState();
    }
    public void showNoInternetConnectionDialog(){
        if(isActivityActive && !noInternetDialogShowing) {
            Log.d(Constants.LOG_TAG,"Showing internet connection issues dialog");
            showMetaInformation("",null);

            AlertDialog.Builder builder = new AlertDialog.Builder(PatchalyzerMainActivity.this);

            builder.setTitle(this.getResources().getString(R.string.patchalyzer_dialog_no_internet_connection_title));
            builder.setMessage(this.getResources().getString(R.string.patchalyzer_dialog_no_internet_connection_text));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    noInternetDialogShowing = false;
                }
            });
            builder.setOnCancelListener(null);
            builder.setCancelable(false);

            Dialog noInternetConnectionDialog = builder.create();
            noInternetConnectionDialog.show();
            noInternetDialogShowing = true;
        }
    }
    private void showNoCVETestsForApiLevelDialog(String message){
        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        StringBuilder information = new StringBuilder();
        information.append(this.getResources().getString(R.string.patchalyzer_claimed_patchlevel_date)+": <b>" + refPatchlevelDate +"</b></br>");
        information.append("<b><u>"+this.getResources().getString(R.string.patchalyzer_dialog_note_title)+"</u></b></br>");
        information.append(message+"</br>");
        information.append("Android OS version: "+ Build.VERSION.RELEASE);
        showMetaInformation(information.toString(),null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(Constants.LOG_TAG, "Received permission request result; code: " + requestCode);
        if (requestCode == SDCARD_PERMISSION_RCODE) {
            Log.d(Constants.LOG_TAG,"Received request permission results for external storage...");
            if (grantResults.length > 0) {
                //find all neccessary permissions not granted
                List<String> notGrantedPermissions = new LinkedList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        notGrantedPermissions.add(permissions[i]);
                    }
                }

                if (notGrantedPermissions.isEmpty()) {
                    //Success: All neccessary permissions granted
                    // start test in TEST MODE!
                    startServiceIfNotRunning();
                } else {
                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }
                    if(showDialog){ //ask for permission in a loop, cause otherwise test mode will not work
                        requestSdcardPermission();
                    }
                }

            }
        }
    }
}
