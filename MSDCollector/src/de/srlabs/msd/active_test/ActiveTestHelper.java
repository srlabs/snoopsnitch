package de.srlabs.msd.active_test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import de.srlabs.msd.R;
import de.srlabs.msd.qdmon.MsdService;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.MsdDialog;
import de.srlabs.msd.util.MsdLog;

public class ActiveTestHelper{
	private static String TAG = "msd-active-test-helper";
	private Activity context;
	private ActiveTestCallback callback;
	private boolean dummy;
	private MyServiceConnection serviceConnection = new MyServiceConnection();

	private IActiveTestService mIActiveTestService;
	public boolean connected;
	private MyActiveTestCallback myActiveTestCallback = new MyActiveTestCallback();
	private boolean activeTestRunning;

	class MyServiceConnection implements ServiceConnection {


		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MsdLog.i(MsdService.TAG,"MsdServiceHelper.MyServiceConnection.onServiceConnected()");
			mIActiveTestService = IActiveTestService.Stub.asInterface(service);
			try {
				mIActiveTestService.registerCallback(myActiveTestCallback);
				boolean testRunning = mIActiveTestService.isTestRunning();
				MsdLog.i(TAG,"Initial recording = " + testRunning);
			} catch (RemoteException e) {
				handleFatalError("RemoteException while calling mIMsdService.registerCallback(msdCallback) or mIMsdService.isRecording() in MsdServiceHelper.MyServiceConnection.onServiceConnected()", e);
			}
			connected = true;
			callback.testStateChanged();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			MsdLog.i(TAG,"MsdServiceHelper.MyServiceConnection.onServiceDisconnected() called");
			context.unbindService(this);
			mIActiveTestService = null;
			connected = false;
			startService();
			// Service connection was lost, so let's call recordingStopped
			callback.testStateChanged();
		}
	};
	class  MyActiveTestCallback extends IActiveTestCallback.Stub{
		@Override
		public void testResultsChanged(Bundle b) throws RemoteException {
			try{
				ActiveTestResults results = (ActiveTestResults) b.getSerializable("results");
				// MsdLog.i(TAG,"testResultsChanged:" + results.formatTextTable());
				callback.handleTestResults(results);
			} catch(Exception e){
				MsdLog.e(TAG,"Exception in ActiveTestHelper.MyActiveTestCallback.testResultsChanged()",e);
			}
		}
		@Override
		public void testStateChanged() throws RemoteException {
			isActiveTestRunning();
			callback.testStateChanged();
		}
	}
	public ActiveTestHelper(Activity activity, ActiveTestCallback callback, boolean dummy){
		this.context = activity;
		this.callback = callback;
		this.dummy = dummy;
		startService();
	}

	private void startService() {
		if(dummy){
			context.startService(new Intent(context, DummyActiveTestService.class));
			context.bindService(new Intent(context, DummyActiveTestService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
		} else{
			context.startService(new Intent(context, ActiveTestService.class));
			context.bindService(new Intent(context, ActiveTestService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	public boolean startActiveTest(String ownNumber){
		try {
			mIActiveTestService.setForegroundActivityClass(context.getClass().getName());
			return mIActiveTestService.startTest(ownNumber);
		} catch (Exception e) {
			handleFatalError("Exception while running mIActiveTestService.startTest(ownNumber)", e);
			return false;
		}
	}
	public void stopActiveTest(){
		try {
			mIActiveTestService.stopTest();
		} catch (Exception e) {
			handleFatalError("Exception while running mIActiveTestService.startTest(ownNumber)", e);
		}
	}
	public boolean isConnected(){
		return connected ;
	}
	public boolean isActiveTestRunning(){
		MsdLog.i(TAG,"ActiveTestHelper.isActiveTestRunning() called");
		if(!connected)
			return false;
		activeTestRunning = false;
		try {
			activeTestRunning = mIActiveTestService.isTestRunning();
		} catch (RemoteException e) {
			handleFatalError("RemoteException while calling mIActiveTestService.isTestRunning() in MsdServiceHelper.startRecording()", e);
		}
		MsdLog.i(TAG,"mIActiveTestService.isTestRunning() returns " + activeTestRunning);
		return activeTestRunning;
	}
	private void handleFatalError(String errorMsg, Exception e){
		String msg = errorMsg;
		if(e != null)
			msg += e.getClass().getCanonicalName() + ": " + e.getMessage() + "  Stack: " + Log.getStackTraceString(e);
		MsdLog.e(TAG, msg, e);
		callback.internalError(msg);
	}

	public void clearCurrentFails() {
		try {
			mIActiveTestService.clearCurrentFails();
		} catch (Exception e) {
			handleFatalError("Exception in ActiveTestHelper.clearCurrentFails()",e);
		}
	}

	public void clearCurrentResults() {
		try {
			mIActiveTestService.clearCurrentResults();
		} catch (Exception e) {
			handleFatalError("Exception in ActiveTestHelper.clearCurrentResults()",e);
		}
	}
	
	public void clearResults() {
		try {
			mIActiveTestService.clearResults();
		} catch (Exception e) {
			handleFatalError("Exception in ActiveTestHelper.clearCurrentResults()",e);
		}
	}
	public void queryPhoneNumberAndStart(){
		queryPhoneNumberAndStart(null);
	}
	private void queryPhoneNumberAndStart(String msg){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final String lastConfirmedOwnNumber = prefs.getString(Constants.PREFS_KEY_OWN_NUMBER, "");
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String ownNumber = telephonyManager.getLine1Number().trim();
		MsdLog.i(TAG, "telephonyManager.getLine1Number(): " + ownNumber);
		final EditText editText = new EditText(context);
		editText.setHint("intl. notation, start with '+'");
		editText.setInputType(InputType.TYPE_CLASS_PHONE);
		final String text = lastConfirmedOwnNumber.isEmpty() ? "+" : lastConfirmedOwnNumber;
		editText.setText(text);
		editText.setSelection(text.length());

		final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("Confirm your phone number");
		if(msg != null)
			dialog.setMessage(msg);
		dialog.setView(editText);
		dialog.setPositiveButton("Run", new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				String confirmedOwnNumber = editText.getText().toString().trim();
				if(!( confirmedOwnNumber.startsWith("+") || confirmedOwnNumber.startsWith("00"))){
					queryPhoneNumberAndStart("Please enter an international number (with '+' or '00' in the beginning)");
					return;
				}
				String tmp = "";
				for(int i=0;i<confirmedOwnNumber.length();i++){
					char c = confirmedOwnNumber.charAt(i);
					if(i == 0 && c == '+')
						tmp += c;
					else if(c == ' ' || c == '/')
						continue;
					else if(Character.isDigit(c))
						tmp += c;
					else{
						queryPhoneNumberAndStart("Invalid character in phone number");
						return;
					}
				}
				confirmedOwnNumber = tmp;
				if(confirmedOwnNumber.isEmpty())
					return;
				prefs.edit().putString(Constants.PREFS_KEY_OWN_NUMBER, confirmedOwnNumber).commit();
				startActiveTest(confirmedOwnNumber);
			}
		});
		dialog.setNegativeButton("Cancel", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callback.testStateChanged();
			}
		});
		dialog.show();
	}

	public void showConfirmDialogAndStart(final boolean clearResults){
		final boolean uploadDisabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_active_test_disable_upload", false);
		String positiveButtonText = context.getString(R.string.test_and_upload);
		if(uploadDisabled)
			positiveButtonText = context.getResources().getString(R.string.alert_button_ok);
		MsdDialog.makeConfirmationDialog(context, context.getResources().getString(R.string.alert_networktest_message), new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				if(clearResults)
					clearResults();
				try {
					mIActiveTestService.setUploadDisabled(uploadDisabled);
				} catch (Exception e) {
					handleFatalError("Exception in ActiveTestHelper.showConfirmDialogAndStart()",e);
				}
				queryPhoneNumberAndStart();
			}
		}, null,null,
		positiveButtonText, context.getString(R.string.alert_button_cancel)).show();
	}

	public void applySettings() {
		try {
			if(mIActiveTestService != null)
				mIActiveTestService.applySettings();
		} catch (Exception e) {
			handleFatalError("Exception in ActiveTestHelper.applySettings()",e);
		}
	}
}
