package de.srlabs.snoopsnitch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import de.srlabs.snoopsnitch.active_test.ActiveTestCallback;
import de.srlabs.snoopsnitch.active_test.ActiveTestHelper;
import de.srlabs.snoopsnitch.active_test.ActiveTestResults;
import de.srlabs.snoopsnitch.util.MsdLog;
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
        loadWebView();
        activeTestHelper = new ActiveTestHelper(this, activeTestCallback);
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
                    activeTestHelper.showConfirmDialogAndStart(false);
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
        if(operatorSettingsIntent.resolveActivity(getPackageManager()) == null){
            //intent not available, so hide Network button
            this.btnNetwork.setVisibility(View.GONE);
        }
        else {
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
}
