package de.srlabs.msd;

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
import de.srlabs.msd.active_test.ActiveTestCallback;
import de.srlabs.msd.active_test.ActiveTestHelper;
import de.srlabs.msd.active_test.ActiveTestResults;
import de.srlabs.msd.qdmon.MsdServiceCallback;
import de.srlabs.msd.qdmon.MsdServiceHelper;
import de.srlabs.msd.qdmon.StateChangedReason;
import de.srlabs.msd.util.MsdLog;

public class ActiveTestAdvanced extends BaseActivity implements MsdServiceCallback{
	private static final String TAG = "ActiveTestAdvanced";
	private Button btnStartStop;
	private Button btnMode;
	private Button btnNetwork;
	//private TextView activeTestLogView;
	protected MsdServiceHelper msdServiceHelper;
	private ActiveTestHelper activeTestHelper;
	private MyActiveTestCallback activeTestCallback = new MyActiveTestCallback();
	private WebView activeTestWebView;
	private ActiveTestResults lastResults;
	private enum StartButtonMode {
		START, STOP, CONTINUE, STARTOVER
	}
	private StartButtonMode startButtonMode;
	protected boolean recordingStartedForActiveTest;

	class MyActiveTestCallback implements ActiveTestCallback{
		@Override
		public void handleTestResults(ActiveTestResults results) {
			lastResults = results;
			updateWebView();
		}

		@Override
		public void testStateChanged() {
			Log.i("ActiveTestAdvanced", "testStateChanged()");
			updateButtons();
			// Stop recording after the test when recording has been started for the test only.
			if(recordingStartedForActiveTest && !activeTestHelper.isActiveTestRunning()){
				msdServiceHelper.stopRecording();
			}
		}

		@Override
		public void internalError(String msg) {
			// TODO Auto-generated method stub

		}

	}

	private void appendLogMsg(String newMsg){
		//		newMsg = newMsg.trim();
		//		activeTestLogView.append(newMsg + "\n");
		//		// find the amount we need to scroll.  This works by
		//		// asking the TextView's internal layout for the position
		//		// of the final line and then subtracting the TextView's height
		//		// http://stackoverflow.com/questions/3506696/auto-scrolling-textview-in-android-to-bring-text-into-view
		//		try{
		//			final int scrollAmount = activeTestLogView.getLayout().getLineTop(activeTestLogView.getLineCount()) - activeTestLogView.getHeight();
		//			// if there is no need to scroll, scrollAmount will be <=0
		//			if (scrollAmount > 0)
		//				activeTestLogView.scrollTo(0, scrollAmount);
		//			else
		//				activeTestLogView.scrollTo(0, 0);
		//		} catch(NullPointerException e){}
	}

	public void updateWebView() {
		Log.i("ActiveTestAdvanced", "JS:" + lastResults.getUpdateJavascript());
		activeTestWebView.loadUrl("javascript:" + lastResults.getUpdateJavascript());
		Log.i("ActiveTestAdvanced", "TEXT:" + lastResults.formatTextTable());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_active_test_advanced);
		this.btnStartStop = (Button) findViewById(R.id.btnStartStop);
		this.btnMode = (Button) findViewById(R.id.btnMode);
		this.btnNetwork = (Button) findViewById(R.id.btnNetwork);
		this.activeTestWebView = (WebView)findViewById(R.id.activeTestWebView);
		loadWebView();
		msdServiceHelper = getMsdServiceHelperCreator().getMsdServiceHelper();
		activeTestHelper = new ActiveTestHelper(this, activeTestCallback, false);
		this.btnStartStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(startButtonMode == StartButtonMode.START || startButtonMode == StartButtonMode.CONTINUE || startButtonMode == StartButtonMode.STARTOVER){
					// TODO: Starting the service should probably be done in activeTestService?
					if(!msdServiceHelper.isRecording()){
						boolean result = msdServiceHelper.startRecording();
						if(!result){
							return;
						}
						recordingStartedForActiveTest = true;
					}
					if(startButtonMode == StartButtonMode.CONTINUE){
						// Continue: Clear fails and start with current results
						activeTestHelper.clearCurrentFails();
					} else if(startButtonMode == StartButtonMode.STARTOVER){
						// Start over: Clear test results and start again
						activeTestHelper.clearResults();
					}
					//activeTestHelper.startActiveTest("+4915784571666"); // TODO: Query for number, cache results based on IMSI
					activeTestHelper.queryPhoneNumberAndStart();
				} else{ // STOP
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
		this.btnNetwork.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
			}
		});
		updateButtons();
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	@Override
	public void onBackPressed() {
	    Intent intent = new Intent(this, DashboardActivity.class);
	    startActivity(intent);
	}

	protected void updateButtons() {
		Log.i("ActiveTestAdvanced", "updateButtons()");
		boolean testRunning = activeTestHelper.isActiveTestRunning();
		if(testRunning){
			startButtonMode = StartButtonMode.STOP;
			btnStartStop.setText("Stop");
		} else if(lastResults != null){
			if(lastResults.isTestRoundCompleted()){
				startButtonMode = StartButtonMode.STARTOVER;
				btnStartStop.setText("Start over");
			} else if(lastResults.isTestRoundContinueable()){
				startButtonMode = StartButtonMode.CONTINUE;
				btnStartStop.setText("Continue");
			} else{
				startButtonMode = StartButtonMode.START;
				btnStartStop.setText("Start");
			}
		} else{
			startButtonMode = StartButtonMode.START;
			btnStartStop.setText("Start");
		}
	}
	@SuppressLint("SetJavaScriptEnabled")
	private void loadWebView(){
		MsdLog.i(TAG, "loadWebView() called");
		activeTestWebView.getSettings().setJavaScriptEnabled(true);
		// activeTestWebView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		activeTestWebView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return (event.getAction() == MotionEvent.ACTION_MOVE);
			}
		});
		activeTestWebView.setWebViewClient(new WebViewClient(){
			boolean done = false;
			@Override
			public void onPageFinished(WebView view, String url) {
				// Call update javascript code after the page has finished loading
				if(!done){
					MsdLog.i(TAG, "onPageFinished() calls updateWebView()");
					updateWebView();
					done = true;
				}
			}
		});
		activeTestWebView.loadUrl("file:///android_asset/active_test_advanced.html");
	}

	@Override
	public void internalError(String msg) {
		appendLogMsg("internalError(" + msg + ")");
	}

	@Override
	public void stateChanged(StateChangedReason reason) {
		// TODO Auto-generated method stub

	}
}
