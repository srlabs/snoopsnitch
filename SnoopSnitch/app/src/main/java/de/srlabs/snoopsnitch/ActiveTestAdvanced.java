package de.srlabs.snoopsnitch;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.util.LinkedList;
import java.util.List;

import de.srlabs.snoopsnitch.active_test.ActiveTestCallback;
import de.srlabs.snoopsnitch.active_test.ActiveTestHelper;
import de.srlabs.snoopsnitch.active_test.ActiveTestResults;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.PermissionChecker;
import de.srlabs.snoopsnitch.util.Utils;

public class ActiveTestAdvanced extends BaseActivity {
    private static final String TAG = "ActiveTestAdvanced";
    private Button btnStartStop;
    private Button btnMode;
    private Button btnNetwork;
    //private TextView activeTestLogView;
    private ActiveTestHelper activeTestHelper;
    private MyActiveTestCallback activeTestCallback = new MyActiveTestCallback();
    private WebView activeTestWebView;
    private ActiveTestResults lastResults;

    private enum StartButtonMode {
        START, STOP, CONTINUE, STARTOVER
    }

    private StartButtonMode startButtonMode;

    class MyActiveTestCallback implements ActiveTestCallback {
        @Override
        public void handleTestResults(ActiveTestResults results) {
            lastResults = results;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    updateWebView();
                }
            });
        }

        @Override
        public void testStateChanged() {
            Log.i("ActiveTestAdvanced", "testStateChanged()");
            updateButtons();
        }

        @Override
        public void internalError(String msg) {
            showErrorMsg(msg);
        }

        @Override
        public void deviceIncompatibleDetected() {
            String incompatibilityReason = getResources().getString(R.string.compat_no_baseband_messages_in_active_test);
            Utils.showDeviceIncompatibleDialog(ActiveTestAdvanced.this, incompatibilityReason, new Runnable() {
                @Override
                public void run() {
                    msdServiceHelperCreator.getMsdServiceHelper().stopRecording();
                    quitApplication();
                }
            });
        }
    }

    private void showErrorMsg(String msg) {
        String errorJs = "setErrorLog(" + escape(msg) + ";\n";
        activeTestWebView.loadUrl("javascript:" + errorJs);
    }

    private String escape(String input) {
        if (input == null)
            return "undefined";
        return "\"" + input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    public void updateWebView() {
        if (lastResults == null)
            return; // updateWebView() will be called again when results are available
        activeTestWebView.loadUrl("javascript:" + lastResults.getUpdateJavascript(this.getApplicationContext()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_test_advanced);
        this.btnStartStop = (Button) findViewById(R.id.btnStartStop);
        this.btnMode = (Button) findViewById(R.id.btnMode);
        this.btnNetwork = (Button) findViewById(R.id.btnNetwork);
        this.activeTestWebView = (WebView) findViewById(R.id.activeTestWebView);

        activeTestHelper = new ActiveTestHelper(this, activeTestCallback);
        loadWebView();

        this.btnStartStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startButtonMode == StartButtonMode.START || startButtonMode == StartButtonMode.CONTINUE || startButtonMode == StartButtonMode.STARTOVER) {
                    if (startButtonMode == StartButtonMode.CONTINUE) {
                        // Continue: Clear fails and start with current results
                        activeTestHelper.clearCurrentFails();
                    } else if (startButtonMode == StartButtonMode.STARTOVER) {
                        // Start over: Clear test results and start again
                        activeTestHelper.clearResults();
                    }

                    if (PermissionChecker.checkAndRequestPermissionsForActiveTest(ActiveTestAdvanced.this)) {
                        activeTestHelper.showConfirmDialogAndStart(false);
                    }

                } else { // STOP
                    activeTestHelper.stopActiveTest();
                }
                updateButtons();
            }
        });
        this.btnMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });

        final Intent operatorSettingsIntent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        ActivityInfo activityInfo = operatorSettingsIntent.resolveActivityInfo(getPackageManager(),operatorSettingsIntent.getFlags());
        if(activityInfo != null && activityInfo.enabled && activityInfo.exported){
            //intent available and usable, so show Network button
            this.btnNetwork.setVisibility(View.VISIBLE);
            this.btnNetwork.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(operatorSettingsIntent);
                }
            });
        }

        updateButtons();
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeTestHelper.applySettings();
    }

    protected void updateButtons() {
        Log.i("ActiveTestAdvanced", "updateButtons()");
        boolean testRunning = activeTestHelper.isActiveTestRunning();
        if (testRunning) {
            startButtonMode = StartButtonMode.STOP;
            btnStartStop.setText("Stop");
        } else if (lastResults != null) {
            if (lastResults.isTestRoundCompleted()) {
                startButtonMode = StartButtonMode.STARTOVER;
                btnStartStop.setText("Start over");
            } else if (lastResults.isTestRoundContinueable()) {
                startButtonMode = StartButtonMode.CONTINUE;
                btnStartStop.setText("Continue");
            } else {
                startButtonMode = StartButtonMode.START;
                btnStartStop.setText("Start");
            }
        } else {
            startButtonMode = StartButtonMode.START;
            btnStartStop.setText("Start");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() {
        MsdLog.i(TAG, "loadWebView() called");
        activeTestWebView.getSettings().setJavaScriptEnabled(true);
        // activeTestWebView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
        activeTestWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
        activeTestWebView.setWebViewClient(new WebViewClient() {
            boolean done = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                // Call update javascript code after the page has finished loading
                if (!done) {
                    MsdLog.i(TAG, "onPageFinished() calls updateWebView()");
                    updateWebView();
                    done = true;
                }
            }
        });
        activeTestWebView.loadUrl("file:///android_asset/active_test_advanced.html");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        MsdLog.d("DashboardActivity", "Received Permission request result; code: " + requestCode);
        if (requestCode == PermissionChecker.REQUEST_ACTIVE_TEST_PERMISSIONS) {
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
                    // -> start Active Test!
                    //Log.i(TAG, "Active Test PERMISSIONS: ALL granted!");
                    activeTestHelper.showConfirmDialogAndStart(false);
                } else {

                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }

                    if (showDialog) {
                        showDialogAskingForAllPermissionsActiveTest(getResources().getString(R.string.alert_active_test_permissions_not_granted));
                    } else {
                        // IF permission is denied (and "never ask again" is checked)
                        // Log.e(TAG, mTAG + ": Permission FAILURE: some permissions are not granted. Asking again.");
                        showDialogPersistentDeniedPermissions(getResources().getString(R.string.alert_active_test_permissions_not_granted_persistent));
                    }

                }

            }
        }

    }

    private void showDialogAskingForAllPermissionsActiveTest(String message) {
        MsdDialog.makeConfirmationDialog(this, message,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionChecker.checkAndRequestPermissionsForActiveTest(ActiveTestAdvanced.this);
                    }
                }, null, false).show();
    }

    private void showDialogPersistentDeniedPermissions(String message) {
        /*TODO: Send user to permission settings for SNSN directly? Adapt message accordingly
                     startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
							 Uri.parse("package:de.srlabs.snoopsnitch")));*/
        MsdDialog.makeConfirmationDialog(this, message, null, null, false).show();

    }
}
