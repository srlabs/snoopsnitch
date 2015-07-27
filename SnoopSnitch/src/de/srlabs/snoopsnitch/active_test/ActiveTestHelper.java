package de.srlabs.snoopsnitch.active_test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdService;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;

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
	private Dialog confirmDialog = null;

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
		@Override
		public void deviceIncompatibleDetected() throws RemoteException {
			callback.deviceIncompatibleDetected();
		}
	}
	public ActiveTestHelper(Activity activity, ActiveTestCallback callback){
		this.context = activity;
		this.callback = callback;
		startService();
	}

	private void startService() {
		context.startService(new Intent(context, ActiveTestService.class));
		context.bindService(new Intent(context, ActiveTestService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public boolean startActiveTest(String ownNumber){
		try {
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
			handleFatalError("Exception while running mIActiveTestService.stopTest(ownNumber)", e);
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
		final String lastConfirmedOwnNumber = MsdConfig.getOwnNumber(context);
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
			private boolean alreadyClicked = false;
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if(alreadyClicked)
					return;
				alreadyClicked = true;
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
				MsdConfig.setOwnNumber(context, confirmedOwnNumber);
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
		if(confirmDialog != null && confirmDialog.isShowing())
			return;

		final boolean uploadDisabled = MsdConfig.getActiveTestDisableUpload(context);

		String positiveButtonText;
		String networkTestMessage;
		if(uploadDisabled) {
			positiveButtonText = context.getResources().getString(R.string.alert_button_ok);
			networkTestMessage =
				context.getResources().getString(R.string.alert_networktest_message);
		} else {
			positiveButtonText = context.getString(R.string.test_and_upload);
			networkTestMessage =
				context.getResources().getString(R.string.alert_networktest_message) + "\n" +
				context.getResources().getString(R.string.alert_networktest_privacy_disclaimer);
		}
		confirmDialog = MsdDialog.makeConfirmationDialog(context, networkTestMessage, new OnClickListener() 
		{
			private boolean alreadyClicked = false;
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				if(alreadyClicked)
					return;
				alreadyClicked = true;
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
		positiveButtonText, context.getString(R.string.alert_button_cancel), false);
		confirmDialog.show();
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
