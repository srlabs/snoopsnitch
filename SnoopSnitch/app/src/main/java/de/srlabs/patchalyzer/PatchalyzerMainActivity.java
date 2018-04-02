package de.srlabs.patchalyzer;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import de.srlabs.patchalyzer.Constants.ActivityState;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;


public class PatchalyzerMainActivity extends FragmentActivity {
    private Handler handler;
    private Button startTestButton;
    private TextView statusTextView, percentageText;
    private WebView legendView;
    private ScrollView webViewContent, metaInfoText;
    private ProgressBar progressBar;
    private PatchalyzerSumResultChart resultChart;

    // Make this activity into a singleton for easier access from service
    protected static PatchalyzerMainActivity instance;
    public TestCallbacks callbacks;

    private boolean isServiceBound=false;
    private boolean noInternetDialogShowing = false;
    private String currentPatchlevelDate; // Only valid in ActivityState.VULNERABILITY_LIST
    private boolean restoreStatePending = false;
    private String noCVETestsForApiLevelMessage = null;
    private static final int SDCARD_PERMISSION_RCODE = 1;

    private ActivityState lastActiveState = null;
    private ActivityState nonPersistentState = ActivityState.PATCHLEVEL_DATES;


    protected ActivityState getActivityState() {
        SharedPreferences settings = getSharedPreferences("PATCHALYZER", 0);
        return ActivityState.valueOf(settings.getString("state", ActivityState.PATCHLEVEL_DATES.toString()));
    }

    // Saves ActivityState to sharedPrefs and triggers UI reload if PatchalyzerMainActivity.instance exists
    protected static void setActivityState(ContextWrapper context, ActivityState state) {


        Log.d(Constants.LOG_TAG,"Writing " + state.toString() + " state to sharedPrefs");
        SharedPreferences settings = context.getSharedPreferences("PATCHALYZER", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("state", state.toString());
        editor.commit();

        PatchalyzerMainActivity instance = PatchalyzerMainActivity.instance;
        if (instance != null) {
            instance.restoreState();
        }
    }


    class TestCallbacks extends ITestExecutorCallbacks.Stub{
        @Override
        public void showErrorMessage(final String text) throws RemoteException {
            handler.post(new Runnable(){
                @Override
                public void run() {
                    if(text.equals(TestExecutorService.NO_INTERNET_CONNECTION_ERROR)){
                        showNoInternetConnectionDialog();
                    }
                    else {
                        statusTextView.setText(text);
                    }
                    startTestButton.setEnabled(true);
                }
            });
        }

        @Override
        public void showStatusMessage(final String text) throws RemoteException {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(text != null){
                        statusTextView.setText(text);
                    }
                }
            });
        }

        @Override
        public void showOutdatedError(String upgradeUrl) throws RemoteException {
            if(upgradeUrl == null)
                upgradeUrl = Constants.DEFAULT_APK_UPGRADE_URL;
            final String finalUpgradeUrl = upgradeUrl;
            handler.post(new Runnable() {
                public void run() {
                    startTestButton.setEnabled(false);
                    webViewContent.removeAllViews();
                    WebView wv = new WebView(PatchalyzerMainActivity.this);
                    String html = "<html><body><h1>New version available</h1>This app version is out of date. Please download the latest version here: <a href=\"" + finalUpgradeUrl + "\">" + finalUpgradeUrl + "</a></body></html>";
                    wv.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
                    webViewContent.addView(wv);
                }
            });
        }

        @Override
        public void updateProgress(final double progressPercent) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchalyzerMainActivity received updateProgress(" + progressPercent + ")"+ PatchalyzerMainActivity.this + " - "+ TestExecutorService.instance);
            handler.post(new Runnable() {
                @Override
                public void run() {
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
            });
        }
        @Override
        public void finished() throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchalyzerMainActivity received finished()");
            handler.post(new Runnable() {
                @Override
                public void run() {;
                    startTestButton.setEnabled(true);
                    showMetaInformation("Finished");

                    showPatchlevelDateNoTable();
                }
            });
        }
        @Override
        public void showNoCVETestsForApiLevel(String message) throws RemoteException{
            Log.i(Constants.LOG_TAG,"PatchalyzerMainActivity received showNoCVETestsForApiLevel()");
            noCVETestsForApiLevelMessage = message;
        }
    }
    public void showMetaInformation(String status){
        metaInfoText.removeAllViews();
        WebView wv = new WebView(PatchalyzerMainActivity.this);
        String html = "<html><body>\n";
        if(status != null)
            html += "\t" + status + "</body></html>\n";
        Log.i(Constants.LOG_TAG,"Meta information text:\n"+html);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.loadData(html, "text/html",null);
        metaInfoText.addView(wv);
    }
    public void displayCutline(){
        String html =
                    "\t<div style=\"text-align:right\">\n"+
                        "\t<span style=\"color:"+toColorString(Constants.COLOR_MISSING)+"\">Patch missing</span>&nbsp;&nbsp;<br>\n" +
                        "\t<span style=\"color:"+toColorString(Constants.COLOR_PATCHED)+"\">Patched</span>&nbsp;&nbsp;<br>\n" +
                        "\t<span style=\"color:"+toColorString(Constants.COLOR_INCONCLUSIVE)+"\">Test inconclusive</span>&nbsp;&nbsp;<br>\n" +
                        "\t<span style=\"color:"+toColorString(Constants.COLOR_NOTAFFECTED)+"\">Not affected</span>&nbsp;&nbsp;<br>\n" +
                        "\t<span style=\"color:"+toColorString(Constants.COLOR_NOTCLAIMED)+"\">After claimed patch level</span>&nbsp;&nbsp;\n" +
                    "\t</div>\n"+
                "</body></html>";
        legendView.setBackgroundColor(Color.TRANSPARENT);
        legendView.loadData(html,"text/html",null);
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.callbacks = new TestCallbacks();
        Log.d(Constants.LOG_TAG, "onCreate() called");
        handler = new Handler(Looper.getMainLooper());
        setContentView(R.layout.activity_patchalyzer);
        startTestButton = (Button) findViewById(R.id.btnDoIt);
        webViewContent = (ScrollView) findViewById(R.id.scrollViewTable);
        statusTextView = (TextView) findViewById(R.id.textView);
        percentageText = (TextView) findViewById(R.id.textPercentage);
        legendView = (WebView) findViewById(R.id.legend);
        metaInfoText = (ScrollView) findViewById(R.id.scrollViewText);
        //metaInfoText.setBackgroundColor(Color.TRANSPARENT);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        resultChart = (PatchalyzerSumResultChart) findViewById(R.id.sumResultChart);

        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });
        statusTextView.setText("");
        percentageText.setText("");
        ActionBar actionBar = getActionBar();


        if(!Constants.IS_TEST_MODE) {
            actionBar.setTitle("Patchalyzer");
            actionBar.setSubtitle("App ID: "+TestUtils.getAppId(this));
        }else{
            actionBar.setTitle("Patchalyzer - TESTMODE");
        }

        // see onOptionsItemSelected
        actionBar.setDisplayHomeAsUpEnabled(true);

        displayCutline();

        if (savedInstanceState != null) {
            //TODO: Move this to sharedprefs?
            currentPatchlevelDate = savedInstanceState.getString("currentPatchlevelDate");
        }

        initDatabase();


        restoreState();


        //startService();
        PatchalyzerMainActivity.instance = this;
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
        PatchalyzerMainActivity.instance = this;

        cancelAnalysisFinishedNotification();

        TestExecutorService service = TestExecutorService.instance;
        if (service != null) {
            TestExecutorService.TestExecutorServiceHelper helper = service.helper;
            helper.updateCallback(callbacks);
        }

    }

    private void cancelAnalysisFinishedNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(TestExecutorService.FINISHED_NOTIFICATION_ID);
    }

    private ComponentName startServiceIfNotRunning(){
        if (TestExecutorService.instance == null) {
            Intent intent = new Intent(PatchalyzerMainActivity.this, TestExecutorService.class);
            //intent.setAction(ITestExecutorServiceInterface.class.getName());
            startService(intent);
        }

        return null;
    }

    @Override
    protected void onPause(){
        super.onPause();
        PatchalyzerMainActivity.instance = null;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        PatchalyzerMainActivity.instance = null;
        //if(isServiceBound)
          //  unbindService(mConnection);

        /*
        Log.i(Constants.LOG_TAG,"onDestroy() called -> persisting state to sharedPrefs: "+state.toString());
        SharedPreferences settings = getSharedPreferences("PATCHALYZER", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("state", state.toString());
        editor.commit();
        */
    }


    private void restoreState(){
        cancelAnalysisFinishedNotification();
        ActivityState tempNonPersistentState = nonPersistentState;
        showPatchlevelDateNoTable();
        if (TestExecutorService.instance == null) {
            startTestButton.setEnabled(true);
        } else {
            startTestButton.setEnabled(false);
            showMetaInformation("Testing your phone...");
        }
        if(tempNonPersistentState == ActivityState.VULNERABILITY_LIST) {
            // TODO: show specific vulnerability here
            showDetailsNoTable(currentPatchlevelDate);
        }

    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("state", getActivityState());
        savedInstanceState.putString("currentPatchlevelDate", currentPatchlevelDate);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void startTest(){

        progressBar.setVisibility(View.VISIBLE);
        percentageText.setVisibility(View.VISIBLE);
        resultChart.setVisibility(View.INVISIBLE);
        resultChart.resetCounts();

        if(TestUtils.isConnectedToInternet(this)) {
            noCVETestsForApiLevelMessage = null;
            clearTable();
            startTestButton.setEnabled(false);
            if(!requestSdcardPermission()) {
                startTestButton.setEnabled(true);
                return;
            }
            startServiceIfNotRunning();

            //statusTextView.setText("Testing your phone...");
            metaInfoText.removeAllViews();
            metaInfoText.addView(statusTextView);

            recreate();
        }else{
            //no internet connection
            Log.w(Constants.LOG_TAG,"Not testing, because of missing internet connection.");
            showNoInternetConnectionDialog();
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
        showMetaInformation(null);
        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        showMetaInformation("Claimed patch level: <b>" + refPatchlevelDate +"</b>");
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable()");
        //Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable(): w=" + webViewContent.getWidth() + "  h=" + webViewContent.getHeight() + "  innerW=" + webViewContent.getChildAt(0).getWidth() + "  innerH=" + webViewContent.getChildAt(0).getHeight());
        try{
            JSONObject testResults = TestUtils.getAnalysisResult(this);
            if(TestUtils.getAnalysisResult(this) == null) {
                // TODO: This could be used further down to display the analysis execution date.
                showMetaInformation("Claimed patch level: " + refPatchlevelDate+"<br>No test results!");
                return;
            }
            Vector<String> categories = new Vector<String>();
            Iterator<String> categoryIterator = testResults.keys();
            while (categoryIterator.hasNext())
                categories.add(categoryIterator.next());
            Collections.sort(categories);
            LinearLayout rows = new LinearLayout(this);
            rows.setOrientation(LinearLayout.VERTICAL);
            rows.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));

            resultChart.resetCounts();
            for (final String category : categories) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                Button button = (Button) getLayoutInflater().inflate(R.layout.custom_button, null);;
                // TODO: Remove this, category is now already truncated with recent test suite
                String truncatedCategory = category;
                if (category.startsWith("201")) {
                    truncatedCategory = category.substring(0, 7);
                }
                else if(category.equals("other")){
                    truncatedCategory = "General";
                }
                button.setText(truncatedCategory);
                button.getMeasuredWidth();
                button.setWidth(308);
                Log.i(Constants.LOG_TAG, "button.getMinimumWidth()=" + button.getMinimumWidth() + "   Category: " + truncatedCategory);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDetailsNoTable(category);
                    }
                });

                row.addView(button);
                JSONArray vulnerabilitiesForCategory = testResults.getJSONArray(category);
                //row.addView(makePatchlevelDateColumn(vulnerabilitiesForPatchlevelDate));

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
                //chart.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 30));
                row.addView(chart);
                rows.addView(row);
            }
            webViewContent.removeAllViews();
            webViewContent.addView(rows);
            nonPersistentState = ActivityState.PATCHLEVEL_DATES;

            if(noCVETestsForApiLevelMessage != null){
                showNoCVETestsForApiLevelDialog(noCVETestsForApiLevelMessage);
            }

            resultChart.invalidate();
            resultChart.setVisibility(View.VISIBLE);
            percentageText.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);

        } catch(Exception e){
            Log.e(Constants.LOG_TAG, "showPatchlevelDateTable Exception", e);
        }
    }
    private void showDetailsNoTable(String category){

        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        int numAffectedVulnerabilities = 0;
        try{
            JSONObject testResults = TestUtils.getAnalysisResult(this);
            if(testResults == null){
                showMetaInformation("Claimed patch level: " + refPatchlevelDate+"<br>No test results!");
                return;
            }
            JSONArray vulnerabilitiesForPatchlevelDate = testResults.getJSONArray(category);

            WebView wv = new WebView(PatchalyzerMainActivity.this);
            StringBuilder html = new StringBuilder();
            html.append("<html><body><table style=\"border-collapse:collapse;\">\n");

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
            wv.loadData(html.toString(), "text/html",null);
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
            infoText.append("<h4 style=\"margin-bottom:0px\">" + category + "</h4>\n<hr>");
            infoText.append("<p><b>" + numCVEs + "</b> CVEs total</p>");
        }else{
            infoText.append("<h4 style=\"margin-bottom:0px\">General tests</h4>\n<hr>");
            infoText.append("<p><b>"+ numCVEs + "</b> tests total</p>");
        }
        showMetaInformation(infoText.toString());
    }

    /**
     * Assign color for test result for certain vulnerability
     * @param vulnerability JSON representation of vulnerability test result
     * @param refPatchlevelDate reference patch level date
     * @return Color representation in int
     */
    private int getVulnerabilityIndicatorColor(JSONObject vulnerability, String refPatchlevelDate) {
        try {
            if (TestUtils.isValidDateFormat(refPatchlevelDate) && !TestUtils.isPatchDateClaimed(refPatchlevelDate)) {
                return Constants.COLOR_NOTCLAIMED;
            } else if (vulnerability.isNull("fixed") || vulnerability.isNull("vulnerable") || vulnerability.isNull("notAffected")) {
                return Constants.COLOR_INCONCLUSIVE;
            } else if(!vulnerability.isNull("notAffected") && vulnerability.getBoolean("notAffected")){
                return Constants.COLOR_NOTAFFECTED;
            } else if (vulnerability.getBoolean("fixed") && !vulnerability.getBoolean("vulnerable")) {
                return Constants.COLOR_PATCHED;
            } else if (!vulnerability.getBoolean("fixed") && vulnerability.getBoolean("vulnerable")) {
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
        if(nonPersistentState == ActivityState.PATCHLEVEL_DATES)
            showPatchlevelDateNoTable();
        else if(nonPersistentState == ActivityState.VULNERABILITY_LIST){
            showDetailsNoTable(currentPatchlevelDate);
        }
    }
    public void showNoInternetConnectionDialog(){
        if(!noInternetDialogShowing) {
            Log.d(Constants.LOG_TAG,"Showing internet connection issues dialog");
            showMetaInformation("");

            AlertDialog.Builder builder = new AlertDialog.Builder(PatchalyzerMainActivity.this);

            builder.setTitle("No network connection");
            builder.setMessage("Could not establish connection to backend server.\nPlease make sure your " +
                    "device is connected to the internet and try again later.");
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
        information.append("Claimed patch level: <b>" + refPatchlevelDate +"</b></br>");
        information.append("<b><u>NOTE</u></b></br>");
        information.append(message+"</br>");
        information.append("Android OS version: "+ Build.VERSION.RELEASE);
        showMetaInformation(information.toString());
    }

    /**
     * Simple method to dump the size of all components to debug layout problems
     * @param v
     * @param prefix
     */
    private void dumpLayout(View v, String prefix){
        Log.i(Constants.LOG_TAG, "dumpLayout(): " + prefix + ": w=" + v.getWidth() + "  h=" + v.getHeight());
        if(v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View subView = vg.getChildAt(i);
                dumpLayout(subView, prefix + ", " + i);
            }
        }
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
