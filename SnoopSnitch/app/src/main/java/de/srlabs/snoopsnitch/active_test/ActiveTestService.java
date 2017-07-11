package de.srlabs.snoopsnitch.active_test;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
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

import de.srlabs.snoopsnitch.active_test.ActiveTestResults.SingleTestState;
import de.srlabs.snoopsnitch.analysis.GSMmap;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;
import de.srlabs.snoopsnitch.qdmon.Operator;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.PermissionChecker;
import de.srlabs.snoopsnitch.util.Utils;

public class ActiveTestService extends Service {
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
    private StateMachine stateMachine;
    private MyPhoneStateListener phoneStateListener = new MyPhoneStateListener();
    private MySmsReceiver smsReceiver = new MySmsReceiver();
    private String currentExtraRecordingFilename;
    private boolean testRunning = false;
    private MsdServiceHelper msdServiceHelper;
    private ITelephony telephonyService;
    private int currentExtraRecordingStartDiagMsgCount;
    private boolean smsMoDisabled = false;
    private String smsMoNumber = null;
    private int networkGenerationAtTestStart = 0;
    private int numSuccessfulTests;

    class MyPhoneStateListener extends PhoneStateListener {
        /**
         * REQUIRED PERMISSION:
         * READ_PHONE_STATE (if not granted: incomingNumber will be empty string)
         *
         * @param phoneState
         * @param incomingNumber
         */
        @Override
        public void onCallStateChanged(final int phoneState, final String incomingNumber) {
            if (!testRunning)
                return;
            MsdLog.i(TAG, "onCallStateChanged(" + phoneState + "," + incomingNumber + ")");
            if (phoneState == TelephonyManager.CALL_STATE_IDLE) {
                MsdLog.i(TAG, "CALL_STATE_IDLE: " + incomingNumber);
                stateMachine.handleTelIdle();
            } else if (phoneState == TelephonyManager.CALL_STATE_OFFHOOK) {
                MsdLog.i(TAG, "CALL_STATE_OFFHOOK: " + incomingNumber);
                stateMachine.handleTelDialing();
            } else if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
                MsdLog.i(TAG, "CALL_STATE_RINGING: " + incomingNumber);
                stateMachine.handleTelRinging();
            } else
                MsdLog.d(TAG, "unhandled call state: " + phoneState);

        }
    }

    ;

    class MySmsReceiver extends SmsReceiver {
        @Override
        protected void onReceiveSms(final SmsMessage sms) {
            stateMachine.handleIncomingSms(sms);
        }
    }

    class MyActiveTestServiceStub extends IActiveTestService.Stub {
        @Override
        public void registerCallback(IActiveTestCallback callback) throws RemoteException {
            if (!callbacks.contains(callback))
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
        public void setUploadDisabled(boolean uploadDisabled)
                throws RemoteException {
            ActiveTestService.this.uploadDisabled = uploadDisabled;
        }

        /**
         * Called by the UI in onResume so that the displayed settings (online/offline, number of iterations) are applied
         *
         * @throws RemoteException
         */
        @Override
        public void applySettings() throws RemoteException {
            //try to switch to Online again
            ActiveTestService.this.applySettings(true);
        }
    }

    class ProgressTickRunnable implements Runnable {
        @Override
        public void run() {
            updateNetworkOperatorAndRat();
            if (testRunning) {
                stateMachine.progressTick();
            }
            broadcastTestResults();
            handler.postDelayed(this, 1000);
        }
    }

    enum State {
        ROUND_START, CALL_MO, CALL_MO_ACTIVE, SMS_MO, CALL_MT_API, CALL_MT_WAITING, CALL_MT_ACTIVE, SMS_MT_API, SMS_MT_WAITING, PAUSE, END
    }

    class StateMachine {
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
        private boolean forceSkipIncomingSms = false;

        void handleIncomingSms(SmsMessage sms) {
            //debugInfo("handleIncomingSms() received in state " + state.name());
            stateInfo("Received SMS in state " + state.name() + ": " + sms.getMessageBody());
            if (state == State.SMS_MT_WAITING) {
                currentTestSuccess();
                broadcastTestResults();
                if (!testRunning)
                    return;
                iterate();
            } else if (state == State.SMS_MT_API) {
                // The SMS is coming in before the API reports a result, this
                // can happen if the network traffic is delayed or the API is
                // unreliable
                stateInfo("Received SMS while waiting for API Result");
                api.abort();
                api = null;
                currentTestSuccess();
                broadcastTestResults();
                if (!testRunning)
                    return;
                iterate();
            } else {
                stateInfo("Received unexpected Gsmmap test sms in state " + state.name());
            }
        }

        void handleTelRinging() {
            debugInfo("handleTelRinging() received in state " + state.name());
            if (state == State.CALL_MT_WAITING) {
                setState(State.CALL_MT_ACTIVE, "handleTelRinging()", Constants.CALL_MT_ACTIVE_TIMEOUT);
                results.getCurrentTest().updateTimeout(Constants.CALL_MT_ACTIVE_TIMEOUT);
                results.getCurrentTest().stateTestRunning();
                broadcastTestResults();
            } else if (state == State.CALL_MT_API) {
                // The SMS is coming in before the API reports a result, this
                // can happen if the network traffic is delayed or the API is
                // unreliable
                stateInfo("Received call while waiting for API Result");
                api.abort();
                api = null;
                setState(State.CALL_MT_ACTIVE, "handleTelRinging()", Constants.CALL_MT_ACTIVE_TIMEOUT);
                results.getCurrentTest().stateTestRunning();
                broadcastTestResults();
            } else {
                stateInfo("Received unexpected call in state " + state.name());
            }
        }

        void currentTestSuccess() {
            numSuccessfulTests++;
            int numMessages = msdServiceHelper.getDiagMsgCount() - currentExtraRecordingStartDiagMsgCount;
            stateInfo("Number of messages: " + numMessages);
            if (numSuccessfulTests >= 3 && numMessages == 0) {
                // At least 3 successful tests and we have not generated any diag messages yet => The device is incompatible.
                boolean deviceCompatibleDetected = MsdConfig.getDeviceIncompatible(ActiveTestService.this);
                if (!deviceCompatibleDetected) {
                    stateInfo("Detected incompatible device, stopping test");
                    // We have never received an SQL message from the parser
                    // and we have at least 3 tests without diag messages =>
                    // The device is most likely incompatible.
                    ActiveTestService.this.stopTest();
                    MsdConfig.setDeviceIncompatible(ActiveTestService.this, true);
                    Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
                    for (IActiveTestCallback callback : callbacks) {
                        try {
                            callback.deviceIncompatibleDetected();
                        } catch (Exception e) {
                            debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
                            callbacksToRemove.add(callback);
                        }
                    }
                    callbacks.removeAll(callbacksToRemove);
                }
            }
            results.getCurrentTest().success();
        }

        void setState(State newState, String msg, long timeout) {
            if (timeout > 0)
                nextTimeoutMillis = System.currentTimeMillis() + timeout;
            else
                nextTimeoutMillis = 0; // Disable previous timeout
            String logMsg = "setState: " + state.name() + " => " + newState.name();
            if (msg != null)
                logMsg += " : " + msg;
            if (timeout > 0)
                logMsg += " TIMEOUT: " + Utils.formatTimestamp(nextTimeoutMillis);
            stateInfo(logMsg);
            state = newState;
        }

        void handleTelIdle() {
            stateInfo("handleTelIdle() received in state " + state.name());
            if (state == State.CALL_MT_ACTIVE) {
                currentTestSuccess();
                broadcastTestResults();
                if (!testRunning)
                    return;
                if (previousCallMoOnline)
                    iterate();
                else {
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.SMS_MT, Constants.SMS_MT_TIMEOUT);
                    results.getCurrentTest().stateWaiting();
                    setState(State.SMS_MT_WAITING, "Offline mode", Constants.SMS_MT_TIMEOUT);
                    broadcastTestResults();
                }
            } else if (state == State.CALL_MO_ACTIVE) {
                currentTestSuccess();
                broadcastTestResults();
                if (!testRunning)
                    return;
                if (previousCallMoOnline)
                    iterate();
                else {
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.CALL_MT, Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
                    results.getCurrentTest().stateWaiting();
                    setState(State.CALL_MT_WAITING, "Offline mode", Constants.CALL_MT_TIMEOUT);
                    broadcastTestResults();
                }
            }
        }

        void handleTelDialing() {
            stateInfo("handleTelDialing() received in state " + state.name());
            if (state == State.CALL_MO) {
                results.getCurrentTest().stateTestRunning();
                results.getCurrentTest().updateTimeout(Constants.CALL_MO_ACTIVE_TIMEOUT);
                setState(State.CALL_MO_ACTIVE, "handleTelDialing()", Constants.CALL_MO_ACTIVE_TIMEOUT);
            }
        }

        /**
         * Chooses and starts a new test
         */
        void iterate() {
            if (testStopped)
                return;
            if (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                stateInfo("iterate() called but telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE, calling iterate again in one second");
                handler.removeCallbacks(this.iterateRunnable);
                postIterateRunnable(1000);
                return;
            }

            updateOnlineState(true);
            stateInfo("Active Test iteration in Mode: "+ (results.isOnlineMode() ? "Online" : "Offline"));

            if (continiousMode) {
                handleFatalError("Continious mode is not yet implemented");
            } else {
                boolean skipIncomingSms = true;
                Operator operator = new Operator(ActiveTestService.this);
                skipIncomingSms = GSMmap.dataSufficient(operator.getMcc(), operator.getMnc(), getCurrentNetworkRatGeneration());

                if (forceSkipIncomingSms)
                    skipIncomingSms = true;
                // Find the action with the lowest run count and then trigger this action
                int numSmsMo = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.SMS_MO);
                int numCallMo = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.CALL_MO);
                int numSmsMt = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.SMS_MT);
                int numCallMt = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.CALL_MT);
                State nextState = State.SMS_MO;
                int minRunCount = numSmsMo;
                if (smsMoDisabled || numCallMo < minRunCount) {
                    minRunCount = numCallMo;
                    nextState = State.CALL_MO;
                }
                // Offline mode can only trigger CALL_MO, the tests SMS_MT and CALL_MT are automatically done after CALL_MO
                if (!skipIncomingSms && results.isOnlineMode() && numSmsMt < minRunCount) {
                    minRunCount = numSmsMt;
                    nextState = State.SMS_MT_API;
                }
                if (results.isOnlineMode() && numCallMt < minRunCount) {
                    minRunCount = numCallMt;
                    nextState = State.CALL_MT_API;
                }
                if (!results.isOnlineMode() && numCallMt < minRunCount) {
                    // We have to trigger CALL_MO to get to  CALL_MT in offline mode
                    minRunCount = numCallMt;
                    nextState = State.CALL_MO;
                }
                if (!results.isOnlineMode() && numSmsMt < minRunCount) {
                    // We have to trigger CALL_MO to get to  SMS_MT in offline mode
                    minRunCount = numSmsMt;
                    nextState = State.CALL_MO;
                }
                if (minRunCount >= results.getNumIterations()) {
                    nextState = State.END;
                }
                stateInfo("iterate(): numSmsMo=" + numSmsMo + "  numCallMo=" + numCallMo + "  numSmsMt=" + numSmsMt + "  numCallMt=" + numCallMt + "  nextState=" + nextState.name());
                if (nextState == State.SMS_MO) {
                    setState(State.SMS_MO, "iterate()", Constants.SMS_MO_TIMEOUT);
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.SMS_MO, Constants.SMS_MO_TIMEOUT);
                    results.getCurrentTest().stateTestRunning();
                    triggerSmsMo();
                } else if (nextState == State.CALL_MO) {
                    setState(State.CALL_MO, "iterate()", Constants.CALL_MO_TIMEOUT);
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.CALL_MO, Constants.CALL_MO_TIMEOUT + Constants.CALL_MO_ACTIVE_TIMEOUT);
                    results.getCurrentTest().stateWaiting();
                    previousCallMoOnline = results.isOnlineMode();
                    triggerCallMo(results.isOnlineMode() || skipIncomingSms);
                } else if (nextState == State.SMS_MT_API) {
                    setState(State.SMS_MT_API, "iterate()", Constants.API_TIMEOUT);
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.SMS_MT, Constants.API_TIMEOUT + Constants.SMS_MT_TIMEOUT);
                    results.getCurrentTest().stateApiRunning();
                    triggerApiSmsback();
                } else if (nextState == State.CALL_MT_API) {
                    setState(State.CALL_MT_API, "iterate()", Constants.API_TIMEOUT);
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.CALL_MT, Constants.API_TIMEOUT + Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
                    results.getCurrentTest().stateApiRunning();
                    triggerApiCallback();
                } else if (nextState == State.END) {
                    // Wait 500ms before closing the file to get a complete capture of all messages generated by the test.
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            setState(State.END, "iterate() reached state END", 0);
                            results.testRoundComplete();
                            ActiveTestService.this.stopTest();
                        }
                    }, 500);
                    // No further actions to start
                } else {
                    handleFatalError("Invalid nextState in StateMachine.iterate()");
                }
                broadcastTestResults();
            }
        }

        /**
         * Called once per second, sends state updates to UI and aborts tests after timeout
         */
        void progressTick() {
            if (continiousMode && state == State.PAUSE) {
                // Continious mode can be reimplemented here later.
            } else {
                if (nextTimeoutMillis > 0 && System.currentTimeMillis() > nextTimeoutMillis) {
                    nextTimeoutMillis = 0; // Only do the same timeout once
                    handleTimeout();
                }
            }
        }

        void handleTimeout() {
            stateInfo("handleTimeout(state=" + state.name() + ")");
            if (state == State.SMS_MO) {
                results.getCurrentTest().failTimeout();
                iterate();
            } else if (state == State.CALL_MO) {
                results.getCurrentTest().failTimeout();
                iterate();
            } else if (state == State.CALL_MO_ACTIVE) {
                stateInfo("Aborting outgoing call in CALL_MO_ACTIVE");
                try {
                    telephonyService.endCall();
                } catch (RemoteException e) {
                    stateInfo("RemoteException in telephonyService.endCall: "+e);
                }
            } else if (state == State.CALL_MT_API) {
                if (api != null)
                    api.abort();
                results.getCurrentTest().failApiTimeout();
                results.setOnlineMode(false);
                iterate();
            } else if (state == State.CALL_MT_WAITING) {
                results.getCurrentTest().failTimeout();
                if (previousCallMoOnline) {
                    iterate();
                } else {
                    // Wait for the SMS in offline mode even if the callback number has not called back.
                    updateNetworkOperatorAndRat();
                    results.startTest(TestType.SMS_MT, Constants.SMS_MT_TIMEOUT);
                    results.getCurrentTest().stateWaiting();
                    setState(State.SMS_MT_WAITING, "Offline mode", Constants.SMS_MT_TIMEOUT);
                    broadcastTestResults();
                }
            } else if (state == State.CALL_MT_ACTIVE) {
                stateInfo("Soft timeout in CALL_MT_ACTIVE reached, continuing to wait until the phone is idle again");
            } else if (state == State.SMS_MT_API) {
                if (api != null)
                    api.abort();
                results.getCurrentTest().failApiTimeout();
                results.setOnlineMode(false);
                iterate();
            } else if (state == State.SMS_MT_WAITING) {
                results.getCurrentTest().failTimeout();
                iterate();
            } else {
                handleFatalError("handleTimeout in unexpected state " + state.name());
            }
        }

        void handleApiFail(String apiId, String errorStr) {
            debugInfo("handleApiFail() received in state " + state.name());
            if (errorStr != null && errorStr.equals("BLACKLISTED")) {
                results.getCurrentTest().failApiError(apiId, errorStr);
                setState(State.END, "Phone is blacklisted", 0);
                stateInfo("Phone is blacklisted, aborting test");
                results.setBlacklisted(true);
                ActiveTestService.this.stopTest();
                return;
            }
            if (errorStr != null && errorStr.equals("INVALID_NUMBER")) {
                results.getCurrentTest().failApiError(apiId, errorStr);
                setState(State.END, "Invalid number", 0);
                stateInfo("Received INVALID_NUMBER in " + state.name() + ", aborting test");
                results.setInvalidNumber(true);
                ActiveTestService.this.stopTest();
                return;
            }
            if (errorStr != null && errorStr.equals("INVALID_REQUEST")) {
                results.getCurrentTest().failApiError(apiId, errorStr);
                setState(State.END, "Please update SnoopSnitch", 0);
                stateInfo("Received INVALID_REQUEST in " + state.name() + ", aborting test");
                results.setInvalidRequest(true);
                ActiveTestService.this.stopTest();
                return;
            }
            if (state == State.CALL_MT_API) {
                stateInfo("Call API failed, switching to offline mode: " + errorStr);
                results.getCurrentTest().failApiError(apiId, errorStr);
                results.setOnlineMode(false);
                iterate();
            } else if (state == State.SMS_MT_API) {
                if (errorStr != null && errorStr.equals("SMS_SKIPPED")) {
                    stateInfo("Received SMS_SKIPPED response from server");
                    forceSkipIncomingSms = true;
                    iterate();
                } else {
                    stateInfo("SMS API failed, switching to offline mode: " + errorStr);
                    results.getCurrentTest().failApiError(apiId, errorStr);
                    results.setOnlineMode(false);
                    iterate();
                }
            } else {
                handleFatalError("handleApiFail in unexpected state " + state.name());
            }
        }

        void handleApiSuccess(String apiId) {
            debugInfo("handleApiSuccess() received in state " + state.name());
            if (state == State.CALL_MT_API) {
                setState(State.CALL_MT_WAITING, "handleApiSuccess", Constants.CALL_MT_TIMEOUT);
                results.getCurrentTest().setRequestId(apiId);
                results.getCurrentTest().stateWaiting();
                // Update the timeout so that the progress indicator does not contain the API timeout any more
                results.getCurrentTest().updateTimeout(Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
            } else if (state == State.SMS_MT_API) {
                setState(State.SMS_MT_WAITING, "handleApiSuccess", Constants.SMS_MT_TIMEOUT);
                results.getCurrentTest().setRequestId(apiId);
                results.getCurrentTest().stateWaiting();
                // Update the timeout so that the progress indicator does not contain the API timeout any more
                results.getCurrentTest().updateTimeout(Constants.SMS_MT_TIMEOUT);
            } else {
                handleFatalError("handleApiSuccess in unexpected state " + state.name());
            }
            broadcastTestResults();
        }

        public void handleSmsSent() {
            stateInfo("handleSmsSent() received in state " + state.name());
            if (state == State.SMS_MO) {
                currentTestSuccess();
                broadcastTestResults();
                if (!testRunning)
                    return;
                iterate();
            } else {
                // This can happen if sending an SMS is delayed e.g. due to an unreliable network connection.
                stateInfo("handleSmsSent in unexpected state " + state.name());
            }
        }

        public void postIterateRunnable(int delayMillis) {
            handler.postDelayed(this.iterateRunnable, delayMillis);
        }

        public void stopTest() {
            handler.removeCallbacks(this.iterateRunnable);
            testStopped = true;
            if (api != null)
                api.abort();
            SingleTestState currentTest = results.getCurrentTest();
            if (currentTest != null)
                currentTest.fail("Test aborted with stopTest()");
        }

        public void triggerApiCallback() {
            if (this.api != null)
                handleFatalError("triggerApiCallback called but api != null");
            this.api = new ApiCall(ApiCall.Action.CALL, ownNumber, ActiveTestService.this) {
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
            if (this.api != null)
                handleFatalError("triggerApiCallback called but api != null");
            this.api = new ApiCall(ApiCall.Action.SMS, ownNumber, ActiveTestService.this) {
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

    /**
     * This method set the tests running to use the Online API,if switchToOnline is true and there is an Internet connection.
     * The user configured setting to not use the Online API (force Offline), will always override the above mentioned logic.
     * @param switchToOnline
     */
    private void updateOnlineState(boolean switchToOnline){
        if (switchToOnline && isNetworkAvailable())
            results.setOnlineMode(true);
        if (MsdConfig.getActiveTestForceOffline(this)) {
            results.setOnlineMode(false);
        }
    }

    private void applySettings(boolean switchToOnline) {

        updateOnlineState(switchToOnline);

        smsMoDisabled = MsdConfig.getActiveTestSMSMODisabled(this);
        results.setSmsMoDisabled(smsMoDisabled);
        smsMoNumber = MsdConfig.getActiveTestSMSMONumber(this);
        int numIterations = MsdConfig.getActiveTestNumIterations(this);
        stateInfo("applySettings(): numIterations= " + numIterations);
        results.setNumIterations(numIterations);
    }

    @Override
    public IBinder onBind(Intent intent) {
        MsdLog.i(TAG, "ActiveTestService.onBind() called");
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

    private int getCurrentNetworkRatGeneration() {
        int fallbackNetworkGeneration = 3; // Default to 3G for now if we can't determine the network generation
        // Some combinations of phone and network operator do not return a valid newtork type in telephonyManager.getNetworkType().
        if (msdServiceHelper != null && msdServiceHelper.isConnected() && msdServiceHelper.getParserNetworkGeneration() > 0)
            fallbackNetworkGeneration = msdServiceHelper.getParserNetworkGeneration();
        int networkGeneration = Utils.networkTypeToNetworkGeneration(telephonyManager.getNetworkType());
        if (networkGeneration == 0)
            networkGeneration = fallbackNetworkGeneration;
        return networkGeneration;
    }

    private void updateNetworkOperatorAndRat() {
        // If the phone was in LTE mode when starting test, all tests will be
        // counted for LTE even if the phone switches back to 2G/3G during the
        // test.
        int networkGeneration = networkGenerationAtTestStart == 4 ? 4 : getCurrentNetworkRatGeneration();
        results.setNetworkOperatorAndRat(telephonyManager, networkGeneration);
    }

    public void debugInfo(String msg) {
        MsdLog.i(TAG, msg);
    }

    public void stateInfo(String msg) {
        MsdLog.i(TAG, "STATE_INFO: " + msg);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final String action = intent.getAction();
        if (ACTION_SMS_SENT.equals(action)) {
            if (stateMachine != null)
                stateMachine.handleSmsSent();
        }
        return START_NOT_STICKY;
    }

    private boolean startTest(String ownNumber) {
        stateInfo("ActiveTestService.startTest(" + ownNumber + ") called");
        this.ownNumber = ownNumber;
        this.numSuccessfulTests = 0;
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
            handleFatalError("Could not get telephonyService", e);
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
        networkGenerationAtTestStart = getCurrentNetworkRatGeneration();
        updateNetworkOperatorAndRat();
        // If updateNetworkOperatorAndRat() detects that the phone is using an
        // LTE network, it will call stopTest(). In that case, there is no point
        // in continuing startTest().
        if (!testRunning)
            return false;
        startExtraFileRecording();
        stateMachine.postIterateRunnable(0);
        broadcastTestStateChanged();
        broadcastTestResults();
        return true;
    }

    private void stopTest() {
        stateInfo("ActiveTestService.stopTest() called");
        testRunning = false;
        try {
            unregisterReceiver(smsReceiver);
        } catch (Exception e) {
        } // unregisterReceiver throws an Exception if it isn't registered, so let's just ignore it.
        if (currentExtraRecordingFilename != null) {
            if (numSuccessfulTests > 0)
                endExtraFileRecording(true);
            else
                endExtraFileRecording(false);
        }
        telephonyManager.listen(phoneStateListener, 0);
        if (stateMachine != null) {
            stateMachine.stopTest();
            stateMachine = null;
        }
        this.msdServiceHelper.stopActiveTest();
        broadcastTestStateChanged();
        broadcastTestResults();
    }

    private void startExtraFileRecording() {
        if (this.currentExtraRecordingFilename != null) {
            handleFatalError("startExtraFileRecording() called but this.currentExtraRecordingFilename != null, this shouldn't happen");
            return;
        }
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(System.currentTimeMillis());
        int networkGeneration = Utils.networkTypeToNetworkGeneration(telephonyManager.getNetworkType());
        if (networkGeneration == 0)
            networkGeneration = msdServiceHelper.getParserNetworkGeneration();
        if (networkGenerationAtTestStart == 4) {
            networkGeneration = 4;
        }
        String connectionType;
        if (networkGeneration == 2)
            connectionType = "GSM";
        else if (networkGeneration == 3)
            connectionType = "3G";
        else if (networkGeneration == 4)
            connectionType = "LTE";
        else
            connectionType = "UNKNOWN";
        String network = telephonyManager.getNetworkOperator();

        String IMSI = telephonyManager.getSubscriberId();
        String SIM_MCC_MNC = (IMSI == null || IMSI.length() < 6) ? "NOIMSI" : IMSI.substring(0, 6);

        String modelAndAndroidVersion = Build.MODEL.replace('.', '_') + "_Android_" + Build.VERSION.RELEASE.replace('.', '_');
        String filename = String.format(Locale.US,
                "%s.%s.%s.%04d%02d%02d-%02d%02d%02d.%s.%s.log",
                "qdmon",
                modelAndAndroidVersion,
                SIM_MCC_MNC,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                connectionType,
                network);
        this.currentExtraRecordingFilename = filename;
        currentExtraRecordingStartDiagMsgCount = msdServiceHelper.getDiagMsgCount();
        if (uploadDisabled) {
            stateInfo("Would now open dumpfile " + filename);
            msdServiceHelper.startActiveTest(); // Make sure MsdService keeps recording (for the local analysis) even if we don't upload anything
        } else {
            stateInfo("Opening dumpfile " + filename);
            msdServiceHelper.startExtraRecording(filename);
        }
    }

    private void endExtraFileRecording(boolean upload) {
        if (uploadDisabled) {
            if (upload) {
                stateInfo("Would now close and upload " + currentExtraRecordingFilename);
            } else {
                stateInfo("Would now discard " + currentExtraRecordingFilename);
            }
            msdServiceHelper.endExtraRecording(false); // Just to make sure there is no extra recording
            currentExtraRecordingFilename = null;
        } else {
            if (currentExtraRecordingFilename == null)
                throw new IllegalStateException("endExtraFileRecording(" + upload + ") called but currentExtraRecordingFilename == null");
            if (upload) {
                stateInfo("Closing and uploading " + currentExtraRecordingFilename);
            } else {
                stateInfo("Discarding " + currentExtraRecordingFilename);
            }
            msdServiceHelper.endExtraRecording(upload);
            currentExtraRecordingFilename = null;
        }
    }

    /**
     * REQUIRED PERMISSION:
     * CALL_PHONE
     *
     * @param online
     */
    private void triggerCallMo(final boolean online) {
        if (PermissionChecker.isAccessingPhoneStateAllowed(ActiveTestService.this)) {
            final Uri telUri = Uri.parse("tel:" + (online ? Constants.CALL_NUMBER : Constants.CALLBACK_NUMBER));
            MsdLog.i(TAG, "calling out to " + telUri);
            final Intent intent = new Intent(Intent.ACTION_CALL, telUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            MsdLog.w(TAG, "Triggering call is not allowed. User did not grant CALL_PHONE permission.");
        }
    }

    /**
     * REQUIRED PERMISSION:
     * SEND_SMS
     */
    private void triggerSmsMo() {
        if (PermissionChecker.isSendingSMSAllowed(ActiveTestService.this)) {
            final PendingIntent sentIntent = PendingIntent.getService(this, 0, new Intent(ACTION_SMS_SENT,
                    null,
                    this,
                    ActiveTestService.class), 0);
            MsdLog.i(TAG, "Sending sms to invalid destination");
            try {
                SmsManager.getDefault().sendTextMessage(smsMoNumber, null, "This is a test SMS", sentIntent, null);
            } catch (Exception e) {
                results.getCurrentTest().fail("Failed to send SMS to " + smsMoNumber);
                stateMachine.setState(State.END, "Invalid SMS MO number", 0);
                stateInfo("Invalid Number: " + smsMoNumber + ", aborting test");
                results.setInvalidSmsMoNumber(true);
                ActiveTestService.this.stopTest();
            }
        } else {
            MsdLog.w(TAG, "Sendind SMS is not allowed. User did not grant SEND_SMS permission.");
        }
    }

    private void broadcastTestResults() {
        if (callbacks.size() == 0)
            return;
        Bundle b = new Bundle();
        b.putSerializable("results", results);
        Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
        for (IActiveTestCallback callback : callbacks) {
            try {
                callback.testResultsChanged(b);
            } catch (Exception e) {
                debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
                callbacksToRemove.add(callback);
            }
        }
        callbacks.removeAll(callbacksToRemove);
        if (callbacks.size() == 0) {
            stopTestNoCallbacks();
        }
    }

    private void stopTestNoCallbacks() {
        stateInfo("Terminating active test since all callbacks have disappeared");
        stopTest();
        handler.removeCallbacks(progressTickRunnable);
        stopSelf();
    }

    private void broadcastTestStateChanged() {
        if (callbacks.size() == 0)
            return;
        Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
        for (IActiveTestCallback callback : callbacks) {
            try {
                callback.testStateChanged();
            } catch (Exception e) {
                debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
                callbacksToRemove.add(callback);
            }
        }
        callbacks.removeAll(callbacksToRemove);
        if (callbacks.size() == 0) {
            stopTestNoCallbacks();
        }
    }

    private void handleFatalError(String msg) {
        handleFatalError(msg, null);
    }

    private void handleFatalError(String msg, final Throwable e) {
        if (e != null)
            msg += ": " + e.getClass().getCanonicalName() + ": " + e.getMessage() + "  Stack: " + Log.getStackTraceString(e);
        MsdLog.e(TAG, "handleFatalError: " + msg);
        results.setFatalError(msg);
        broadcastTestResults();
        stopTest();
        stopSelf(); // Terminate this service after a fatal error
    }
}
