package de.srlabs.patchalyzer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
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
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import de.srlabs.patchalyzer.Constants.ActivityState;
import de.srlabs.snoopsnitch.BuildConfig;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;


public class MainActivity extends Activity {
    private Handler handler;
    private Button startTestButton;
    private TextView statusTextView;
    private WebView legendView;
    private ScrollView webViewContent, metaInfoText;
    private ProgressBar progressBar;
    private PatchalyzerSumResultChart resultChart;

    private ITestExecutorServiceInterface mITestExecutorService;
    private boolean isServiceBound=false;
    private boolean noInternetDialogShowing = false;
    private JSONObject testResults = null;
    private String currentPatchlevelDate; // Only valid in ActivityState.VULNERABILITY_LIST
    private boolean restoreStatePending = false;
    private boolean showTablePending = false;
    private String noCVETestsForApiLevelMessage = null;
    private static final int SDCARD_PERMISSION_RCODE = 1;

    private ActivityState state = ActivityState.START;

    MsdSQLiteOpenHelper database = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mITestExecutorService = ITestExecutorServiceInterface.Stub.asInterface(service);
            Log.d(Constants.LOG_TAG,"Service connected!");
            isServiceBound = true;
            if(restoreStatePending){
                restoreState();
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(Constants.LOG_TAG, "Service has unexpectedly disconnected");
            isServiceBound = false;
            mITestExecutorService = null;
        }
    };

    private MyCallbacks callbacks = new MyCallbacks();
    class MyCallbacks extends ITestExecutorCallbacks.Stub{
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
                    WebView wv = new WebView(MainActivity.this);
                    String html = "<html><body><h1>New version available</h1>This app version is out of date. Please download the latest version here: <a href=\"" + finalUpgradeUrl + "\">" + finalUpgradeUrl + "</a></body></html>";
                    wv.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
                    webViewContent.addView(wv);
                }
            });
        }

        @Override
        public void updateProgress(final double progressPercent) throws RemoteException {
            Log.i(Constants.LOG_TAG, "MainActivity received updateProgress(" + progressPercent + ")");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setMax(1000);
                    progressBar.setProgress((int) (progressPercent * 1000.0));
                }
            });
        }
        @Override
        public void finished() throws RemoteException {
            Log.i(Constants.LOG_TAG, "MainActivity received finished()");
            handler.post(new Runnable() {
                @Override
                public void run() {;
                    startTestButton.setEnabled(true);
                    showMetaInformation("Finished");
                    if(showTablePending) {
                        showPatchlevelDateNoTable();
                        showTablePending = false;
                    }
                }
            });
        }
        @Override
        public void showNoCVETestsForApiLevel(String message) throws RemoteException{
            Log.i(Constants.LOG_TAG,"MainActivity received showNoCVETestsForApiLevel()");
            noCVETestsForApiLevelMessage = message;
        }
    }
    public void showMetaInformation(String status){
        metaInfoText.removeAllViews();
        WebView wv = new WebView(MainActivity.this);
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
                        "\t<span style=\"color:"+toColorString(Constants.COLOR_NOTAFFECTED)+"\">Not affected</span>&nbsp;&nbsp;\n" +
                    "\t</div>\n"+
                "</body></html>";
        legendView.setBackgroundColor(Color.TRANSPARENT);
        legendView.loadData(html,"text/html",null);
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        setContentView(R.layout.activity_patchalyzer);
        startTestButton = (Button) findViewById(R.id.btnDoIt);
        webViewContent = (ScrollView) findViewById(R.id.scrollViewTable);
        statusTextView = (TextView) findViewById(R.id.textView);
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
        if(!Constants.IS_TEST_MODE) {
            setTitle("Patchalyzer - AppID " + TestUtils.getAppId(this));
        }else{
            setTitle("Patchalyzer - TESTMODE");
        }
        displayCutline();

        if (savedInstanceState != null) {
            state = (ActivityState) savedInstanceState.get("state");
            currentPatchlevelDate = savedInstanceState.getString("currentPatchlevelDate");
            restoreStatePending = true;
        }

        initDatabase();

        // Restore preferences
        SharedPreferences settings = getSharedPreferences("PATCHALYZER", 0);
        state = ActivityState.valueOf(settings.getString("state", ActivityState.START.toString()));
        restoreStatePending = true;

    }

    private void initDatabase(){
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(Constants.LOG_TAG,"Creating SQLite database...");
                    MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(MainActivity.this));
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
        startService();

    }

    private void startService(){
        Intent intent = new Intent(MainActivity.this, TestExecutorService.class);
        intent.setAction(ITestExecutorServiceInterface.class.getName());
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.i("Patchalyzer_Activity","onStop() called -> persisting state to sharedPrefs");

        SharedPreferences settings = getSharedPreferences("PATCHALYZER", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("state", state.toString());
        editor.commit();
     }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(isServiceBound)
            unbindService(mConnection);
    }
    private void restoreState(){
        if(state == ActivityState.PATCHLEVEL_DATES) {
            showPatchlevelDateNoTable();
            startTestButton.setEnabled(true);
        } else if(state == ActivityState.VULNERABILITY_LIST){
            showDetailsNoTable(currentPatchlevelDate);
            startTestButton.setEnabled(true);
        } else{
            startTestButton.setEnabled(true);
        }
        restoreStatePending = false;
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("state", state);
        savedInstanceState.putString("currentPatchlevelDate", currentPatchlevelDate);
        super.onSaveInstanceState(savedInstanceState);
    }
    private void btnClear_clicked(){
        clearTable();
        progressBar.setProgress(0);
        statusTextView.setText("");
        try {
            mITestExecutorService.clearCache();
        } catch(RemoteException e){
            statusTextView.setText(e.toString());
        }
    }
    private void btnDebug_clicked() {
        try {
            String[] cmd = new String[3];
            cmd[0] = "/data/data/de.srlabs.patchalyzer/lib/libbusybox.so";
            cmd[1] = "xzcat";
            cmd[2] = "/data/local/tmp/test.xz";
            Log.i(Constants.LOG_TAG, "XZCAT COMMAND: " + cmd[0] + " " + cmd[1] + " " + cmd[2]);
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            Log.i(Constants.LOG_TAG, "XZCAT LINE: " + line);

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while(true){
                line = errorReader.readLine();
                Log.i(Constants.LOG_TAG,"XZCAT ERROR: " + line);
                if(line == null)
                    break;
            }
        } catch(Exception e){
            Log.e(Constants.LOG_TAG, "XZCAT", e);
        }
    }
    private void dumpBuf(String name, byte[] buf){
        String output = name + ": ";
        for(int i=0;i<buf.length;i++){
            output += String.format("%x, ", buf[i]);
        }
        Log.e(Constants.LOG_TAG, output);
    }
    private void startTest(){
        //startService();


        progressBar.setVisibility(View.VISIBLE);
        resultChart.setVisibility(View.INVISIBLE);

        if(TestUtils.isConnectedToInternet(this)) {
            noCVETestsForApiLevelMessage = null;
            clearTable();
            startTestButton.setEnabled(false);
            try {
                showTablePending = true;
                if(!Constants.IS_TEST_MODE) {
                    mITestExecutorService.startWork(true, true, true, true, true, callbacks);
                }
                else{
                    if(!requestSdcardPermission()) {
                        startTestButton.setEnabled(true);
                        return;
                    }
                    mITestExecutorService.startWork(true, true, true, false, false, callbacks);
                }
                statusTextView.setText("Testing your phone...");
                metaInfoText.removeAllViews();
                metaInfoText.addView(statusTextView);
            } catch (RemoteException e) {
                Log.e(Constants.LOG_TAG, "startTest RemoteException", e);
            }
        }else{
            //no internet connection
            Log.w(Constants.LOG_TAG,"Not testing, because of missing internet connection.");
            showNoInternetConnectionDialog();
        }
    }
    private void uploadResults(){
        //clearTable();
        statusTextView.setText("Uploading...");
        startTestButton.setEnabled(false);
        /*if(!requestSdcardPermission())
            return;*/
        try {
            mITestExecutorService.upload(true, true, callbacks);
        } catch (RemoteException e) {
            Log.e(Constants.LOG_TAG, "uploadResults RemoteException", e);
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


    private JSONObject loadTestResultsWhenNeeded() throws RemoteException, JSONException {
        Log.d(Constants.LOG_TAG,"loadTestResultsWhenNeeded()");
        if(testResults == null){
            String testResultsStr = mITestExecutorService.evaluateVulnerabilitiesTests();
            //Log.e(Constants.LOG_TAG, testResultsStr);
            if(testResultsStr != null ){//&& !testResultsStr.contains("Exception")) { //FIXME
                Log.i(Constants.LOG_TAG,"Trying to convert to JSON: "+testResultsStr);
                testResults = new JSONObject(testResultsStr);
                testResultsStr = null;
            }
        }
        return testResults;
    }
    private void showPatchlevelDateNoTable(){
        showMetaInformation(null);
        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        showMetaInformation("Claimed patch level: <b>" + refPatchlevelDate +"</b>");
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable()");
        //Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable(): w=" + webViewContent.getWidth() + "  h=" + webViewContent.getHeight() + "  innerW=" + webViewContent.getChildAt(0).getWidth() + "  innerH=" + webViewContent.getChildAt(0).getHeight());
        try{
            loadTestResultsWhenNeeded();
            if(testResults == null) {
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
                for (int i = 0; i < vulnerabilitiesForCategory.length(); i++) {
                    JSONObject vulnerability = vulnerabilitiesForCategory.getJSONObject(i);
                    int color = getVulnerabilityIndicatorColor(vulnerability, refPatchlevelDate);
                    statusColors.add(color);
                    switch(color){
                        case Constants.COLOR_PATCHED:
                            resultChart.increasePatched(1);
                            break;
                        case Constants.COLOR_INCONCLUSIVE:
                            resultChart.increaseInconclusive(1);
                            break;
                        case Constants.COLOR_MISSING:
                            resultChart.increaseMissing(1);
                            break;
                        case Constants.COLOR_NOTAFFECTED:
                            resultChart.increaseNotAffected(1);
                            break;
                    }
                }

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
            state = ActivityState.PATCHLEVEL_DATES;

            if(noCVETestsForApiLevelMessage != null){
                showNoCVETestsForApiLevelDialog(noCVETestsForApiLevelMessage);
            }

            resultChart.invalidate();
            resultChart.setVisibility(View.VISIBLE);
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
            loadTestResultsWhenNeeded();
            JSONArray vulnerabilitiesForPatchlevelDate = testResults.getJSONArray(category);

            WebView wv = new WebView(MainActivity.this);
            StringBuilder html = new StringBuilder();
            html.append("<html><body><table style=\"border-collapse:collapse;\">\n");

            for(int i=0;i<vulnerabilitiesForPatchlevelDate.length();i++) {
                JSONObject vulnerability = vulnerabilitiesForPatchlevelDate.getJSONObject(i);
                String identifier = vulnerability.getString("identifier");
                String identifierColor = toColorString(getVulnerabilityIndicatorColor(vulnerability,refPatchlevelDate));
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

            state = ActivityState.VULNERABILITY_LIST;
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
            if (vulnerability.isNull("fixed") || vulnerability.isNull("vulnerable") || vulnerability.isNull("notAffected")) {
                return Constants.COLOR_INCONCLUSIVE;
            } else if(!vulnerability.isNull("notAffected") && vulnerability.getBoolean("notAffected")){
                return Constants.COLOR_NOTAFFECTED;
            } else if (vulnerability.getBoolean("fixed") && !vulnerability.getBoolean("vulnerable")) {
                return Constants.COLOR_PATCHED;
            } else if (!vulnerability.getBoolean("fixed") && vulnerability.getBoolean("vulnerable")) {
                boolean missed = false;
                String vulnerabilityPatchlevelDate = null;
                if (vulnerability.has("patchlevelDate")) {
                    vulnerabilityPatchlevelDate = vulnerability.getString("patchlevelDate");
                }
                if (vulnerabilityPatchlevelDate != null && vulnerabilityPatchlevelDate.startsWith("201") && refPatchlevelDate != null && refPatchlevelDate.startsWith("20")) {
                    if (vulnerabilityPatchlevelDate.compareTo(refPatchlevelDate) <= 0)
                        missed = true;
                }
                if (missed) {
                    return Constants.COLOR_MISSING;
                } else {
                    return Constants.COLOR_MISSING;
                    //identifierView.setBackgroundColor(0xFFFF8000); // Orange in ARGB notation
                }
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
        if(state == ActivityState.VULNERABILITY_LIST)
            showPatchlevelDateNoTable();
        else
            finish();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(state == ActivityState.PATCHLEVEL_DATES)
            showPatchlevelDateNoTable();
        else if(state == ActivityState.VULNERABILITY_LIST){
            showDetailsNoTable(currentPatchlevelDate);
        }
    }
    public void showNoInternetConnectionDialog(){
        if(!noInternetDialogShowing) {

            showMetaInformation("");

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setTitle("No internet connection!");
            builder.setMessage("Your device is not connected to the Internet.\nPlease establish a connection and try starting a test again.");
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
                    try {
                        mITestExecutorService.startWork(true, true, true, false, false, callbacks);
                    }catch(RemoteException e){
                        Log.e(Constants.LOG_TAG,"RemoteException when starting test:"+e.getMessage());
                    }
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
