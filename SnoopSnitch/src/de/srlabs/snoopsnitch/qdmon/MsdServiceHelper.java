package de.srlabs.snoopsnitch.qdmon;

import de.srlabs.snoopsnitch.qdmon.IMsdService;
import de.srlabs.snoopsnitch.qdmon.IMsdServiceCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

public class MsdServiceHelper{
	private static String TAG = "msd-service-helper";
	private Context context;
	private MyServiceConnection serviceConnection = new MyServiceConnection();
	private MsdServiceCallback callback;
	public IMsdService mIMsdService;
	class  MyMsdServiceCallbackStub extends IMsdServiceCallback.Stub{
		@Override
		public void stateChanged(final String reason) throws RemoteException {
			(new Handler(Looper.getMainLooper())).post(new Runnable(){
				@Override
				public void run() {
					callback.stateChanged(StateChangedReason.valueOf(reason));
				};
			});
		}

		@Override
		public void internalError() throws RemoteException {
			System.exit(0);
		}
	}
	MyMsdServiceCallbackStub msdCallback = new MyMsdServiceCallbackStub();
	private boolean connected = false;
	private AnalysisEventDataInterface data = null;
	private StringBuffer logDataBuffer;

	public MsdServiceHelper(Context context, MsdServiceCallback callback){
		this.context = context;
		this.callback = callback;
		startService();
	}
	private void startService(){
		context.startService(new Intent(context, MsdService.class));
		context.bindService(new Intent(context, MsdService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
		data = new AnalysisEventData(context);
	}
	public boolean isConnected(){
		return connected ;
	}
	public boolean startRecording(){
		Log.i(TAG,"MsdServiceHelper.startRecording() called");
		boolean result = false;
		try {
			result = mIMsdService.startRecording();
		} catch (Exception e) {
			handleFatalError("Exception in MsdServiceHelper.startRecording()", e);
		}
		Log.i(TAG,"MsdServiceHelper.startRecording() returns " + result);
		return result;
	}
	public boolean stopRecording(){
		Log.i(TAG,"MsdServiceHelper.stopRecording() called");
		boolean result = false;
		try {
			result = mIMsdService.stopRecording();
		} catch (RemoteException e) {
			handleFatalError("RemoteException while calling mIMsdService.isRecording() in MsdServiceHelper.startRecording()", e);
		}
		Log.i(TAG,"MsdServiceHelper.stopRecording() returns " + result);
		return result;
	}
	public boolean isRecording(){
		if(!connected){
			Log.i(TAG,"MsdServiceHelper.isRecording(): Not connected => false");
			return false;
		}
		boolean result = false;
		try {
			result = mIMsdService.isRecording();
		} catch (RemoteException e) {
			handleFatalError("RemoteException while calling mIMsdService.isRecording() in MsdServiceHelper.startRecording()", e);
		}
		Log.i(TAG,"MsdServiceHelper.isRecording() returns " + result);
		return result;
	}
	class MyServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(MsdService.TAG,"MsdServiceHelper.MyServiceConnection.onServiceConnected()");
			mIMsdService = IMsdService.Stub.asInterface(service);
			try {
				mIMsdService.registerCallback(msdCallback);
				boolean recording = mIMsdService.isRecording();
				Log.i(TAG,"Initial recording = " + recording);

				// Write pending log data when the service is connected
				if(logDataBuffer != null){
					mIMsdService.writeLog("START Pending messages:\n");
					mIMsdService.writeLog(logDataBuffer.toString());
					mIMsdService.writeLog("END Pending messages:\n");
					logDataBuffer = null;
				}
			} catch (RemoteException e) {
				handleFatalError("RemoteException in MsdServiceHelper.MyServiceConnection.onServiceConnected()", e);
			}
			connected = true;
			callback.stateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG,"MsdServiceHelper.MyServiceConnection.onServiceDisconnected() called");
			context.unbindService(this);
			mIMsdService = null;
			connected = false;
			startService();
			// Service connection was lost, so let's call recordingStopped
			callback.stateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
		}
	};

	private void handleFatalError(String errorMsg, Exception e){
		String msg = errorMsg;
		if(e != null)
			msg += e.getClass().getCanonicalName() + ": " + e.getMessage();
		Log.e(TAG, msg, e);
		callback.internalError(msg);
	}
	public AnalysisEventDataInterface getData(){
		return data;
	}
	public void writeLog(String logData) {
		if(isConnected())
			try {
				mIMsdService.writeLog(logData);
			} catch (RemoteException e) {
				handleFatalError("RemoteException in writeLog()",e);
			}
		else{
			if(logDataBuffer == null)
				logDataBuffer = new StringBuffer();
			logDataBuffer.append(logData);
		}
	}
	public void triggerUploading() {
		try {
			mIMsdService.triggerUploading();
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.triggerUploading()", e);
		}
	}
	public long reopenAndUploadDebugLog(){
		try {
			return mIMsdService.reopenAndUploadDebugLog();
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.reopenAndUploadDebugLog()", e);
			return 0;
		}
	}
	public int getParserNetworkGeneration(){
		try {
			return mIMsdService.getParserNetworkGeneration();
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
			return 0;
		}
	}
	public void endExtraRecording(boolean upload) {
		try {
			mIMsdService.endExtraRecording(upload);
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
		}
	}
	public void startExtraRecording(String filename) {
		try {
			mIMsdService.startExtraRecording(filename);
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
		}
	}
	public void startActiveTest(){
		try {
			mIMsdService.startActiveTest();
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
		}
	}
	public void stopActiveTest(){
		try {
			mIMsdService.stopActiveTest();
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
		}
	}
	public int getDiagMsgCount() {
		try {
			return mIMsdService.getDiagMsgCount();
		} catch (RemoteException e) {
			handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
			return 0;
		}
	}
	public void stopService() {
		try {
			mIMsdService.exitService();
			context.unbindService(serviceConnection);
		} catch (Exception e) {
			handleFatalError("Exception in MsdServiceHelper.stopService()", e);
		}
	}
	public long getLastAnalysisTimeMs(){
		try {
			return mIMsdService.getLastAnalysisTimeMs();
		} catch (Exception e) {
			handleFatalError("Exception in MsdServiceHelper.getLastAnalysisTimeMs()", e);
			return 0;
		}
	}
}
