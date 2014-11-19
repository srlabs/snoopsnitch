package de.srlabs.msd.qdmon;

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
		public void recordingStateChanged() throws RemoteException {
			(new Handler(Looper.getMainLooper())).post(new Runnable(){
				public void run() {
					callback.recordingStateChanged();
				};
			});
		}

	}
	MyMsdServiceCallbackStub msdCallback = new MyMsdServiceCallbackStub();
	private boolean connected = false;
	private boolean dummy;
	private AnalysisEventDataInterface data = null;

	public MsdServiceHelper(Context context, MsdServiceCallback callback, boolean dummy){
		this.context = context;
		this.callback = callback;
		this.dummy = dummy;
		startService();
	}
	private void startService(){
		if(dummy){
			context.startService(new Intent(context, DummyMsdService.class));
			context.bindService(new Intent(context, DummyMsdService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
			data = new DummyAnalysisEventData();
		} else{
			context.startService(new Intent(context, MsdService.class));
			context.bindService(new Intent(context, MsdService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
			data = new AnalysisEventData(context);
		}
	}
	public boolean isConnected(){
		return connected ;
	}
	public boolean startRecording(){
		Log.i(TAG,"MsdServiceHelper.startRecording() called");
		boolean result = false;
		try {
			if (data instanceof DummyAnalysisEventData) {
				DummyAnalysisEventData dummyData = (DummyAnalysisEventData) data;
				long currentTime = System.currentTimeMillis();
				dummyData.addDynamicDummyEvents(currentTime);
				mIMsdService.addDynamicDummyEvents(currentTime);
			}
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
		if (data instanceof DummyAnalysisEventData) {
			DummyAnalysisEventData dummyData = (DummyAnalysisEventData) data;
			dummyData.clearPendingEvents();
		}
		Log.i(TAG,"MsdServiceHelper.stopRecording() returns " + result);
		return result;
	}
	public boolean isRecording(){
		Log.i(TAG,"MsdServiceHelper.isRecording() called");
		if(!connected)
			return false;
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
			} catch (RemoteException e) {
				handleFatalError("RemoteException while calling mIMsdService.registerCallback(msdCallback) or mIMsdService.isRecording() in MsdServiceHelper.MyServiceConnection.onServiceConnected()", e);
			}
			connected = true;
			callback.recordingStateChanged();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG,"MsdServiceHelper.MyServiceConnection.onServiceDisconnected() called");
			context.unbindService(this);
			mIMsdService = null;
			connected = false;
			startService();
			// Service connection was lost, so let's call recordingStopped
			callback.recordingStateChanged();
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
}
