package de.srlabs.msd.active_test;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import de.srlabs.msd.active_test.ActiveTestResults.SingleTestState;
import de.srlabs.msd.qdmon.MsdServiceHelper;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.MSDServiceHelperCreator;
import de.srlabs.msd.util.MsdLog;
import de.srlabs.msd.util.Utils;

public class ActiveTestService extends Service{
	private static final String TAG = "msd-active-test-service";
	private boolean uploadDisabled = false;
	private final MyActiveTestServiceStub mBinder = new MyActiveTestServiceStub();
	private ActiveTestResults results = new ActiveTestResults();
	private ProgressTickRunnable progressTickRunnable = new ProgressTickRunnable();
	private Handler handler = new Handler();
	private Vector<IActiveTestCallback> callbacks = new Vector<IActiveTestCallback>();
	private TelephonyManager telephonyManager;
	private final static String ACTION_SMS_SENT = "msd-active-test-service_SMS_SENT";
	private String ownNumber;
	private int originalRingerMode;
	private StateMachine stateMachine;
	private MyPhoneStateListener phoneStateListener = new MyPhoneStateListener() ;
	private MySmsReceiver smsReceiver = new MySmsReceiver();
	private String currentExtraRecordingFilename;
	private boolean testRunning;
	boolean previousCheckAlreadyInForeground = true;
	private MsdServiceHelper msdServiceHelper;
	public String foregroundActivityClassName;
	private ITelephony telephonyService;

	class MyPhoneStateListener extends PhoneStateListener{
		@Override
		public void onCallStateChanged(final int phoneState, final String incomingNumber) {
			MsdLog.i(TAG, "onCallStateChanged(" + phoneState + "," + incomingNumber + ")");
			if (phoneState == TelephonyManager.CALL_STATE_IDLE){
				MsdLog.i(TAG,"CALL_STATE_IDLE: " + incomingNumber);
				stateMachine.handleTelIdle();
			} else if (phoneState == TelephonyManager.CALL_STATE_OFFHOOK){
				MsdLog.i(TAG,"CALL_STATE_OFFHOOK: " + incomingNumber);
				stateMachine.handleTelDialing();
			} else if (phoneState == TelephonyManager.CALL_STATE_RINGING){
				MsdLog.i(TAG,"CALL_STATE_RINGING: " + incomingNumber);
				stateMachine.handleTelRinging();
			} else
				MsdLog.d(TAG, "unhandled call state: " + phoneState);
		}
	};
	class MySmsReceiver extends SmsReceiver{
		@Override
		protected void onReceiveSms(final SmsMessage sms) {
			stateMachine.handleIncomingSms(sms);
		}
	}
	class MyActiveTestServiceStub extends IActiveTestService.Stub {
		@Override
		public void registerCallback(IActiveTestCallback callback) throws RemoteException {
			if(!callbacks.contains(callback))
				callbacks.add(callback);
			broadcastTestStateChanged();
			broadcastTestResults();
		}

		@Override
		public boolean startTest(String ownNumber) throws RemoteException {
			return ActiveTestService.this.startTest(ownNumber);
		}

		@Override
		public void stopTest() throws RemoteException {
			ActiveTestService.this.stopTest();
		}

		@Override
		public void clearResults() throws RemoteException {
			results = new ActiveTestResults();
			broadcastTestResults();
		}
		@Override
		public void clearCurrentFails() throws RemoteException {
			results.getCurrentNetworkOperatorRatTestResults().clearFails();
			results.clearErrorLog();
			broadcastTestResults();
		}
		@Override
		public void clearCurrentResults() throws RemoteException {
			results.getCurrentNetworkOperatorRatTestResults().clearResults();
			results.clearErrorLog();
			broadcastTestResults();
		}
		@Override
		public boolean isTestRunning() throws RemoteException {
			return testRunning;
		}

		@Override
		public void setForegroundActivityClass(String className)
				throws RemoteException {
			foregroundActivityClassName = className;
		}

		@Override
		public void setUploadDisabled(boolean uploadDisabled)
				throws RemoteException {
			ActiveTestService.this.uploadDisabled  = uploadDisabled;
		}

		/**
		 * Called by the UI in onResume so that the displayed settings (online/offline, number of iterations) are applied
		 * @throws RemoteException
		 */
		@Override
		public void applySettings() throws RemoteException {
			// Only switch to online again if no test is running
			ActiveTestService.this.applySettings(!isTestRunning());
		}
	}
	class ProgressTickRunnable implements Runnable{
		@Override
		public void run() {
			updateNetworkOperatorAndRat();
			if(testRunning){
				stateMachine.progressTick();
				checkForeground();
			}
			broadcastTestResults();
			handler.postDelayed(this, 1000);
		}
	}
	enum State {
		ROUND_START, CALL_MO, CALL_MO_ACTIVE, SMS_MO, CALL_MT_API, CALL_MT_WAITING, CALL_MT_ACTIVE, SMS_MT_API, SMS_MT_WAITING, PAUSE, END
	}
	class StateMachine{
		State state = State.ROUND_START;
		long nextTimeoutMillis = 0;
		private boolean previousCallMoOnline = false;
		private boolean continiousMode = false;
		/**
		 * Make sure that this test actually stops after stopTest(), even if there is a Runnable
		 */
		private boolean testStopped = false;
		private Runnable iterateRunnable = new Runnable() {
			@Override
			public void run() {
				iterate();
			}
		};
		private ApiCall api;
		void handleIncomingSms(SmsMessage sms){
			//debugInfo("handleIncomingSms() received in state " + state.name());
			stateInfo("Received SMS in state " + state.name() + ": " + sms.getMessageBody());
			if(state == State.SMS_MT_WAITING){
				currentTestSuccess();
				broadcastTestResults();
				iterate();
			} else if(state == State.SMS_MT_API){
				// The SMS is coming in before the API reports a result, this
				// can happen if the network traffic is delayed or the API is
				// unreliable
				stateInfo("Received SMS while waiting for API Result");
				api.abort();
				api = null;
				currentTestSuccess();
				broadcastTestResults();
				iterate();
			} else{
				stateInfo("Received unexpected Gsmmap test sms in state " + state.name());
			}
		}
		void handleTelRinging() {
			debugInfo("handleTelRinging() received in state " + state.name());
			if(state == State.CALL_MT_WAITING){
				setState(State.CALL_MT_ACTIVE, "handleTelRinging()", Constants.CALL_MT_ACTIVE_TIMEOUT);
				results.getCurrentTest().updateTimeout(Constants.CALL_MT_ACTIVE_TIMEOUT);
				results.getCurrentTest().stateTestRunning();
				broadcastTestResults();
			} else if(state == State.CALL_MT_API){
				// The SMS is coming in before the API reports a result, this
				// can happen if the network traffic is delayed or the API is
				// unreliable
				stateInfo("Received call while waiting for API Result");
				api.abort();
				api = null;
				setState(State.CALL_MT_ACTIVE, "handleTelRinging()", Constants.CALL_MT_ACTIVE_TIMEOUT);
				results.getCurrentTest().stateTestRunning();
				broadcastTestResults();
			} else{
				stateInfo("Received unexpected call in state " + state.name());
			}
		}
		void currentTestSuccess(){
			endExtraFileRecording(true);
			results.getCurrentTest().success();
		}

		void setState(State newState, String msg, long timeout){
			if(timeout > 0)
				nextTimeoutMillis = System.currentTimeMillis() + timeout;
			else
				nextTimeoutMillis = 0; // Disable previous timeout
			String logMsg = "setState: " + state.name() + " => " + newState.name();
			if(msg != null)
				logMsg += " : " + msg;
			if(timeout > 0)
				logMsg += " TIMEOUT: " + Utils.formatTimestamp(nextTimeoutMillis);
			stateInfo(logMsg);
			state = newState;
		}
		void handleTelIdle(){
			stateInfo("handleTelIdle() received in state " + state.name());
			if(state == State.CALL_MT_ACTIVE){
				currentTestSuccess();
				broadcastTestResults();
				if(previousCallMoOnline)
					iterate();
				else{
					updateNetworkOperatorAndRat();
					results.startTest(TestType.SMS_MT, Constants.SMS_MT_TIMEOUT);
					results.getCurrentTest().stateWaiting();
					startExtraFileRecording(TestType.SMS_MT);
					setState(State.SMS_MT_WAITING, "Offline mode",Constants.SMS_MT_TIMEOUT);
					broadcastTestResults();
				}
			} else if(state == State.CALL_MO_ACTIVE){
				currentTestSuccess();
				broadcastTestResults();
				if(previousCallMoOnline)
					iterate();
				else{
					updateNetworkOperatorAndRat();
					results.startTest(TestType.CALL_MT, Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
					results.getCurrentTest().stateWaiting();
					startExtraFileRecording(TestType.CALL_MT);
					setState(State.CALL_MT_WAITING, "Offline mode",Constants.CALL_MT_TIMEOUT);
					broadcastTestResults();
				}
			}
		}
		void handleTelDialing(){
			stateInfo("handleTelDialing() received in state " + state.name());
			if(state == State.CALL_MO){
				results.getCurrentTest().stateTestRunning();
				results.getCurrentTest().updateTimeout(Constants.CALL_MO_ACTIVE_TIMEOUT);
				setState(State.CALL_MO_ACTIVE, "handleTelDialing()", Constants.CALL_MO_ACTIVE_TIMEOUT);
			}
		}
		/**
		 * Chooses and starts a new test
		 */
		void iterate(){
			if(testStopped)
				return;
			if(telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE){
				stateInfo("iterate() called but telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE, calling iterate again in one second");
				handler.removeCallbacks(this.iterateRunnable);
				postIterateRunnable(1000);
				return;
			}
			if(continiousMode){
				handleFatalError("Continious mode is not yet implemented");
			} else{
				// Find the action with the lowest run count and then trigger this action
				int numSmsMo = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.SMS_MO);
				int numCallMo = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.CALL_MO);
				int numSmsMt = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.SMS_MT);
				int numCallMt = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.CALL_MT);
				State nextState = State.SMS_MO;
				int minRunCount = numSmsMo;
				if(numCallMo < minRunCount){
					minRunCount = numCallMo;
					nextState = State.CALL_MO;
				}
				// Offline mode can only trigger CALL_MO, the tests SMS_MT and CALL_MT are automatically done after CALL_MO
				if(results.isOnlineMode() && numSmsMt < minRunCount){
					minRunCount = numSmsMt;
					nextState = State.SMS_MT_API;
				}
				if(results.isOnlineMode() && numCallMt < minRunCount){
					minRunCount = numCallMt;
					nextState = State.CALL_MT_API;
				}
				if(!results.isOnlineMode() && numCallMt < minRunCount){
					// We have to trigger CALL_MO to get to  CALL_MT in offline mode
					minRunCount = numCallMt;
					nextState = State.CALL_MO;
				}
				if(!results.isOnlineMode() && numSmsMt < minRunCount){
					// We have to trigger CALL_MO to get to  SMS_MT in offline mode
					minRunCount = numSmsMt;
					nextState = State.CALL_MO;
				}
				if(minRunCount >= results.getNumIterations()){
					nextState = State.END;
				}
				stateInfo("iterate(): numSmsMo=" + numSmsMo + "  numCallMo=" + numCallMo + "  numSmsMt=" + numSmsMt + "  numCallMt=" + numCallMt + "  nextState=" + nextState.name());
				if(nextState == State.SMS_MO){
					setState(State.SMS_MO, "iterate()",Constants.SMS_MO_TIMEOUT);
					updateNetworkOperatorAndRat();
					results.startTest(TestType.SMS_MO,Constants.SMS_MO_TIMEOUT);
					results.getCurrentTest().stateTestRunning();
					startExtraFileRecording(TestType.SMS_MO);
					triggerSmsMo();
				} else if(nextState == State.CALL_MO){
					setState(State.CALL_MO, "iterate()",Constants.CALL_MO_TIMEOUT);
					updateNetworkOperatorAndRat();
					results.startTest(TestType.CALL_MO,Constants.CALL_MO_TIMEOUT + Constants.CALL_MO_ACTIVE_TIMEOUT);
					results.getCurrentTest().stateWaiting();
					previousCallMoOnline = results.isOnlineMode();
					startExtraFileRecording(TestType.CALL_MO);
					triggerCallMo(results.isOnlineMode());
				} else if(nextState == State.SMS_MT_API){
					setState(State.SMS_MT_API, "iterate()",Constants.API_TIMEOUT);
					updateNetworkOperatorAndRat();
					results.startTest(TestType.SMS_MT,Constants.API_TIMEOUT + Constants.SMS_MT_TIMEOUT);
					results.getCurrentTest().stateApiRunning();
					startExtraFileRecording(TestType.SMS_MT);
					triggerApiSmsback();
				} else if(nextState == State.CALL_MT_API){
					setState(State.CALL_MT_API, "iterate()",Constants.API_TIMEOUT);
					updateNetworkOperatorAndRat();
					results.startTest(TestType.CALL_MT,Constants.API_TIMEOUT + Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
					results.getCurrentTest().stateApiRunning();
					startExtraFileRecording(TestType.CALL_MT);
					triggerApiCallback();
				} else if(nextState == State.END){
					setState(State.END,"iterate() reached state END",0);
					results.testRoundComplete();
					ActiveTestService.this.stopTest();
					// No further actions to start
				} else{
					handleFatalError("Invalid nextState in StateMachine.iterate()");
				}
				broadcastTestResults();
			}
		}
		/**
		 * Called once per second, sends state updates to UI and aborts tests after timeout
		 */
		void progressTick(){
			if(continiousMode && state == State.PAUSE){
				// Continious mode can be reimplemented here later.
			} else{
				if(nextTimeoutMillis > 0 && System.currentTimeMillis() > nextTimeoutMillis){
					nextTimeoutMillis = 0; // Only do the same timeout once
					handleTimeout();
				}
			}
		}
		void handleTimeout(){
			stateInfo("handleTimeout(state=" + state.name() + ")");
			if(state == State.SMS_MO){
				results.getCurrentTest().failTimeout();
				endExtraFileRecording(false);
				iterate();
			} else if(state == State.CALL_MO){
				results.getCurrentTest().failTimeout();
				endExtraFileRecording(false);
				iterate();
			} else if(state == State.CALL_MO_ACTIVE){
				stateInfo("Aborting outgoing call in CALL_MO_ACTIVE");
				try {
					telephonyService.endCall();
				} catch (RemoteException e) {
					handleFatalError("RemoteException in telephonyService.endCall()");
				}
			} else if(state == State.CALL_MT_API){
				results.getCurrentTest().failApiTimeout();
				endExtraFileRecording(false);
				results.setOnlineMode(false);
				iterate();
			} else if(state == State.CALL_MT_WAITING){
				endExtraFileRecording(false);
				results.getCurrentTest().failTimeout();
				if(previousCallMoOnline){
					iterate();
				} else{
					// Wait for the SMS in offline mode even if the callback number has not called back.
					updateNetworkOperatorAndRat();
					results.startTest(TestType.SMS_MT, Constants.SMS_MT_TIMEOUT);
					results.getCurrentTest().stateWaiting();
					startExtraFileRecording(TestType.SMS_MT);
					setState(State.SMS_MT_WAITING, "Offline mode",Constants.SMS_MT_TIMEOUT);
					broadcastTestResults();
				}
			} else if(state == State.CALL_MT_ACTIVE){
				stateInfo("Soft timeout in CALL_MT_ACTIVE reached, continuing to wait until the phone is idle again");
			} else if(state == State.SMS_MT_API){
				results.getCurrentTest().failApiTimeout();
				endExtraFileRecording(false);
				results.setOnlineMode(false);
				iterate();
			} else if(state == State.SMS_MT_WAITING){
				results.getCurrentTest().failTimeout();
				endExtraFileRecording(false);
				iterate();
			} else{
				handleFatalError("handleTimeout in unexpected state " + state.name());
			}
		}
		void handleApiFail(String apiId, String errorStr){
			debugInfo("handleApiFail() received in state " + state.name());				
			if(errorStr != null && errorStr.equals("BLACKLISTED")){
				results.getCurrentTest().failApiError(apiId, errorStr);
				endExtraFileRecording(false);
				setState(State.END, "Phone is blacklisted", 0);
				stateInfo("Phone is blacklisted, aborting test");
				results.setBlacklisted(true);
				ActiveTestService.this.stopTest();
				return;
			}
			if(state == State.CALL_MT_API){
				stateInfo("Call API failed, switching to offline mode: " + errorStr);
				results.getCurrentTest().failApiError(apiId, errorStr);
				endExtraFileRecording(false);
				results.setOnlineMode(false);
				iterate();
			} else if(state == State.SMS_MT_API){
				stateInfo("SMS API failed, switching to offline mode: " + errorStr);
				results.getCurrentTest().failApiError(apiId, errorStr);
				endExtraFileRecording(false);
				results.setOnlineMode(false);
				iterate();
			} else{
				handleFatalError("handleApiFail in unexpected state " + state.name());
			}
		}
		void handleApiSuccess(String apiId){
			debugInfo("handleApiSuccess() received in state " + state.name());
			if(state == State.CALL_MT_API){
				setState(State.CALL_MT_WAITING, "handleApiSuccess", Constants.CALL_MT_TIMEOUT);
				results.getCurrentTest().setRequestId(apiId);
				results.getCurrentTest().stateWaiting();
				// Update the timeout so that the progress indicator does not contain the API timeout any more
				results.getCurrentTest().updateTimeout(Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
			} else if(state == State.SMS_MT_API){
				setState(State.SMS_MT_WAITING, "handleApiSuccess", Constants.SMS_MT_TIMEOUT);
				results.getCurrentTest().setRequestId(apiId);
				results.getCurrentTest().stateWaiting();
				// Update the timeout so that the progress indicator does not contain the API timeout any more
				results.getCurrentTest().updateTimeout(Constants.SMS_MT_TIMEOUT);
			} else{
				handleFatalError("handleApiSuccess in unexpected state " + state.name());
			}
			broadcastTestResults();
		}
		public void handleSmsSent() {
			stateInfo("handleSmsSent() received in state " + state.name());
			if(state == State.SMS_MO){
				currentTestSuccess();
				iterate();
			} else{
				// This can happen if sending an SMS is delayed e.g. due to an unreliable network connection.
				stateInfo("handleSmsSent in unexpected state " + state.name());
			}
		}
		public void postIterateRunnable(int delayMillis) {
			handler.postDelayed(this.iterateRunnable , delayMillis);
		}
		public void stopTest(){
			handler.removeCallbacks(this.iterateRunnable);
			testStopped = true;
			if(api != null)
				api.abort();
			SingleTestState currentTest = results.getCurrentTest();
			if(currentTest != null)
				currentTest.fail("Test aborted with stopTest()");
			if(currentExtraRecordingFilename != null)
				endExtraFileRecording(false);
		}
		public void triggerApiCallback() {
			if(this.api != null)
				handleFatalError("triggerApiCallback called but api != null");
			this.api = new ApiCall(ApiCall.Action.CALL,ownNumber,ActiveTestService.this) {
				@Override
				protected void onSuccess(String requestId) {
					api = null;
					handleApiSuccess(requestId);
				}

				@Override
				protected void onFail(String requestId, String errorStr) {
					api = null;
					handleApiFail(requestId, errorStr);
				}
			};
			this.api.start();
		}
		public void triggerApiSmsback() {
			if(this.api != null)
				handleFatalError("triggerApiCallback called but api != null");
			this.api = new ApiCall(ApiCall.Action.SMS,ownNumber,ActiveTestService.this) {
				@Override
				protected void onSuccess(String requestId) {
					api = null;
					handleApiSuccess(requestId);
				}

				@Override
				protected void onFail(String requestId, String errorStr) {
					api = null;
					handleApiFail(requestId, errorStr);
				}
			};
			this.api.start();
		}
	}
	// http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	private void applySettings(boolean switchToOnline){
		if(switchToOnline && isNetworkAvailable())
			results.setOnlineMode(true);
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("settings_active_test_force_offline", false)){
			results.setOnlineMode(false);
		}
		int numIterations = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("settings_active_test_num_iterations","5"));
		stateInfo("applySettings(): numIterations= " + numIterations);
		results.setNumIterations(numIterations);
	}
	@Override
	public IBinder onBind(Intent intent) {
		MsdLog.i(TAG,"ActiveTestService.onBind() called");
		Thread.setDefaultUncaughtExceptionHandler(
				new Thread.UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						handleFatalError("Uncaught Exception in ActiveTestService Thread " + t.getClass(), e);
					}
				});
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		msdServiceHelper = MSDServiceHelperCreator.getInstance(this, true).getMsdServiceHelper();
		updateNetworkOperatorAndRat();
		applySettings(true);
		broadcastTestResults();
		handler.postDelayed(progressTickRunnable, 1000);
		return mBinder;
	}

	public void updateNetworkOperatorAndRat() {
		try{
			int fallbackGeneration = 0;
			if(msdServiceHelper != null && msdServiceHelper.isConnected())
				fallbackGeneration = msdServiceHelper.getParserNetworkGeneration();
			results.setNetworkOperatorAndRat(telephonyManager, fallbackGeneration);
		} catch(IllegalArgumentException e){
			handleFatalError("LTE is not yet supported for the active test module. Please configure your phone to use 2G/3G only.");
		}
	}

	public void debugInfo(String msg) {
		MsdLog.i(TAG,msg);
	}

	public void stateInfo(String msg) {
		MsdLog.i(TAG,"STATE_INFO: " + msg);
	}
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		final String action = intent.getAction();
		if (ACTION_SMS_SENT.equals(action)) {
			if(stateMachine != null)
				stateMachine.handleSmsSent();
		}
		return START_NOT_STICKY;
	}

	private boolean startTest(String ownNumber){
		this.ownNumber = ownNumber;
		this.msdServiceHelper.startActiveTest();
		// http://stackoverflow.com/questions/599443/how-to-hang-up-outgoing-call-in-android
		try {
			// Java reflection to gain access to TelephonyManager's
			// ITelephony getter
			Log.v(TAG, "Get getTeleService...");
			Class<?> c = Class.forName(telephonyManager.getClass().getName());
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			telephonyService = (ITelephony) m.invoke(telephonyManager);
		} catch (Exception e) {
			handleFatalError("Could not get telephonyService",e);
		}
		stateMachine = new StateMachine();
		results.setOnlineMode(true);
		applySettings(true);
		results.isOnlineMode();
		this.testRunning = true;
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		final IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		intentFilter.setPriority(Integer.MAX_VALUE); // we mean it
		registerReceiver(smsReceiver, intentFilter);
		updateNetworkOperatorAndRat();
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		originalRingerMode = audio.getRingerMode();
		audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		stateMachine.postIterateRunnable(0);
		broadcastTestStateChanged();
		broadcastTestResults();
		return true;
	}
	private void stopTest(){
		unregisterReceiver(smsReceiver);
		if(stateMachine != null){
			stateMachine.stopTest();
			stateMachine = null;
		}
		if(this.currentExtraRecordingFilename != null)
			endExtraFileRecording(false);
		this.msdServiceHelper.stopActiveTest();
		testRunning = false;
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		audio.setRingerMode(originalRingerMode);
		telephonyManager.listen(phoneStateListener, 0);
		broadcastTestStateChanged();
		broadcastTestResults();
	}
	private String determineCell() {
		final StringBuilder cell = new StringBuilder();

		final CellLocation cellLocation = telephonyManager.getCellLocation();
		if (cellLocation instanceof GsmCellLocation) {
			final GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
			cell.append(Integer.toHexString(gsmCellLocation.getLac()));
			cell.append("-");
			cell.append(Integer.toHexString(gsmCellLocation.getCid()));
		} else if (cellLocation instanceof CdmaCellLocation) {
			// final CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
			// network.append(Integer.toString(cdmaCellLocation.getNetworkId()));
			// network.append("-");
			// network.append(Integer.toString(cdmaCellLocation.getBaseStationId()));
		} else if(cellLocation == null){
			return null;
		} else {
			throw new IllegalStateException(cellLocation.getClass().getName());
		}

		return cell.toString();
	}
	private void startExtraFileRecording(TestType type){
		if(this.currentExtraRecordingFilename != null){
			handleFatalError("startExtraFileRecording() called but this.currentExtraRecordingFilename != null, this shouldn't happen");
			return;
		}
		final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		calendar.setTimeInMillis(System.currentTimeMillis());
		int networkGeneration = Utils.networkTypeToNetworkGeneration(telephonyManager.getNetworkType());
		if(networkGeneration == 0)
			networkGeneration = msdServiceHelper.getParserNetworkGeneration();
		String connectionType;
		if(networkGeneration == 2)
			connectionType = "GSM";
		else if(networkGeneration == 3)
			connectionType = "3G";
		else if(networkGeneration == 4)
			connectionType = "LTE";
		else
			connectionType = "UNKNWON";
		String network = telephonyManager.getNetworkOperator() + "-" + determineCell();
		int iteration = results.getCurrentTest().getNum();
		String filename = String.format(Locale.US,
				"%s.%s.%04d%02d%02d-%02d%02d%02d.%s.%s.%s.%d.log",
				"qdmon",
				Build.MODEL,
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH),
				calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND),
				connectionType,
				network,
				type.name(),
				iteration);
		this.currentExtraRecordingFilename = filename;
		if(uploadDisabled){
			stateInfo("Would now open dumpfile " + filename);
			msdServiceHelper.startActiveTest(); // Make sure MsdService keeps recording (for the local analysis) even if we don't upload anything
		} else{
			stateInfo("Opening dumpfile " + filename);
			msdServiceHelper.startExtraRecording(filename);
		}
	}
	private void endExtraFileRecording(boolean upload){
		if(uploadDisabled){
			if(upload){
				stateInfo("Would now close and upload " + currentExtraRecordingFilename);
			} else{
				stateInfo("Would now discard " + currentExtraRecordingFilename);			
			}
			msdServiceHelper.endExtraRecording(upload); // Just to make sure there is no extra recording
			currentExtraRecordingFilename = null;
			return;
		}
		if(currentExtraRecordingFilename == null)
			throw new IllegalStateException("endExtraFileRecording(" + upload + ") called but currentExtraRecordingFilename == null");
		if(upload){
			stateInfo("Closing and uploading " + currentExtraRecordingFilename);
		} else{
			stateInfo("Discarding " + currentExtraRecordingFilename);			
		}
		msdServiceHelper.endExtraRecording(upload);
		currentExtraRecordingFilename = null;
	}
	private void triggerCallMo(final boolean online) {
		final Uri telUri = Uri.parse("tel:" + (online ? Constants.CALL_NUMBER : Constants.CALLBACK_NUMBER));
		MsdLog.i(TAG, "calling out to " + telUri);
		final Intent intent = new Intent(Intent.ACTION_CALL, telUri);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	private void triggerSmsMo() {
		final PendingIntent sentIntent = PendingIntent.getService(this, 0, new Intent(ACTION_SMS_SENT,
				null,
				this,
				ActiveTestService.class), 0);
		MsdLog.i(TAG, "Sending sms to invalid destination");
		SmsManager.getDefault().sendTextMessage("0", null, "This is a test sms", sentIntent, null);
	}
	private void broadcastTestResults() {
		if(callbacks.size() == 0)
			return;
		Bundle b = new Bundle();
		b.putSerializable("results", results);
		Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
		for(IActiveTestCallback callback:callbacks){
			try {
				callback.testResultsChanged(b);
			} catch (Exception e) {
				debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
				callbacksToRemove.add(callback);
			}
		}
		callbacks.removeAll(callbacksToRemove);
		if(callbacks.size() == 0){
			stopTestNoCallbacks();
		}
	}
	private void stopTestNoCallbacks(){
		stateInfo("Terminating active test since all callbacks have disappeared");
		stopTest();
		handler.removeCallbacks(progressTickRunnable);
		stopSelf();		
	}
	private void broadcastTestStateChanged() {
		if(callbacks.size() == 0)
			return;
		Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
		for(IActiveTestCallback callback:callbacks){
			try {
				callback.testStateChanged();
			} catch (Exception e) {
				debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
				callbacksToRemove.add(callback);
			}
		}
		callbacks.removeAll(callbacksToRemove);
		if(callbacks.size() == 0){
			stopTestNoCallbacks();
		}
	}
	private void handleFatalError(String msg){
		handleFatalError(msg,null);
	}
	private void handleFatalError(String msg, final Throwable e){
		if(e != null)
			msg += ": " + e.getClass().getCanonicalName() + ": " + e.getMessage() + "  Stack: " + Log.getStackTraceString(e);
		MsdLog.e(TAG,"handleFatalError: " + msg);
		results.setFatalError(msg);
		broadcastTestResults();
		stopTest();
		stopSelf(); // Terminate this service after a fatal error
	}
	private void checkForeground(){
		// http://stackoverflow.com/questions/5504632/how-can-i-tell-if-android-app-is-running-in-the-foreground
		ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);
		boolean alreadyInForeground = false;
		MsdLog.d(TAG, "RUNNING: " + services.get(0).topActivity.getPackageName().toString());
		MsdLog.d(TAG, "EXPECTED: " + getApplicationContext().getPackageName().toString());
		if (services.get(0).topActivity.getPackageName().toString().equalsIgnoreCase(getApplicationContext().getPackageName().toString())) {
			alreadyInForeground = true;
		}
		MsdLog.d(TAG, "stayInForegroundRunnable.run(): alreadyInForeground=" + alreadyInForeground);
		if(!alreadyInForeground && !previousCheckAlreadyInForeground){
			try {
				Class<?> c = Class.forName(foregroundActivityClassName);
				Intent intent = new Intent(getApplicationContext(), c);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
				//intent.setComponent(new ComponentName(getApplicationContext().getPackageName(), c.getName()));
				getApplication().startActivity(intent);
			} catch (ClassNotFoundException e) {
				handleFatalError("Class.forName(foregroundActivityClassName) failed", e);
			}
		}
		previousCheckAlreadyInForeground = alreadyInForeground;
	}
}
