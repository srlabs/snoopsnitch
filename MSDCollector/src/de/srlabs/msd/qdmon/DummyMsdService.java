package de.srlabs.msd.qdmon;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.Event;
import de.srlabs.msd.upload.DumpFile;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.MsdDatabaseManager;

public class DummyMsdService extends Service{
	public static final String    TAG                   = "dummy-msd-service";

	private final MyMsdServiceStub mBinder = new MyMsdServiceStub();

	private  boolean recording;
	private MsdServiceNotifications msdServiceNotifications = new MsdServiceNotifications(this);
	private Handler msdServiceMainThreadHandler = new Handler();
	
	private DummyDataRunnable dummyDataRunnable;
	private DumpFile df;
	private PrintStream dummyLogPrintStream = null;
	private DummyAnalysisEventData dummyData = null;

	private long timeCallbacksDone = 0;

	private long serviceStartTime;
	
	class MyMsdServiceStub extends IMsdService.Stub {
		private Vector<IMsdServiceCallback> callbacks = new Vector<IMsdServiceCallback>();

		@Override
		public boolean isRecording() throws RemoteException {
			return recording;
		}

		@Override
		public boolean startRecording() throws RemoteException {
			return DummyMsdService.this.startRecording();
		}

		@Override
		public boolean stopRecording() throws RemoteException {
			return DummyMsdService.this.shutdown();
		}

		@Override
		public void registerCallback(IMsdServiceCallback callback) throws RemoteException {
			if(!callbacks.contains(callback))
				callbacks.add(callback);
		}
		@Override
		public long getServiceStartTime() throws RemoteException {
			return serviceStartTime;
		}

		@Override
		public void writeLog(String logData) throws RemoteException {
			Log.i(TAG,"DummyMsdService.MyMsdServiceStub.writeLog: " + logData);
		}

		@Override
		public long getExtraRecordingId() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean startExtraRecording(String filename) throws RemoteException {
			return true;
		}

		@Override
		public boolean endExtraRecording(boolean markForUpload)
				throws RemoteException {
			return true;			
		}

		@Override
		public void triggerUploading() throws RemoteException {
			Log.i(TAG,"triggerUploading() called");
		}

		@Override
		public long reopenAndUploadDebugLog() throws RemoteException {
			Log.i(TAG,"reopenAndUploadDebugLog() called");
			return 0;
		}

		@Override
		public int getParserNetworkGeneration() throws RemoteException {
			return 0;
		}

		@Override
		public void startActiveTest() throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void stopActiveTest() throws RemoteException {
			// TODO Auto-generated method stub
			
		}
	};
	private class DummyDataRunnable implements Runnable{
		private boolean recordingStopped = false;
		@Override
		public void run() {
			if(recordingStopped)
				return;
			long currentTime = System.currentTimeMillis();
			if(df == null || currentTime - df.getStart_time() > 10000){
				MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(DummyMsdService.this));
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
				if(df != null){
					dummyLogPrintStream.close();
					df.endRecording(db);
				}
				Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				c.setTimeInMillis(currentTime);
				// Calendar.MONTH starts counting with 0
				String filename = String.format(Locale.US, "DUMMY_qdmon_%04d-%02d-%02d_%02d-%02d-%02dUTC.smime",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
				df = new DumpFile(filename, DumpFile.TYPE_ENCRYPTED_QDMON);
				df.setEnd_time(currentTime + 10000);
				df.insert(db);
				try {
					dummyLogPrintStream = new PrintStream(DummyMsdService.this.openFileOutput(filename, 0));
				} catch (FileNotFoundException e) {
					Log.e("DummyMsdServiceHelper","Failed to open dummy output " + filename,e);
				}
			}
			dummyLogPrintStream.println("DummyDataRunnable running at " + currentTime);
			getDynamicSms();
			// Enable this code to simulate an internal service error
//			if(System.currentTimeMillis() > startRecordingTime + 10000){
//				msdServiceNotifications.showInternalErrorNotification("Dummy internal error 10 seconds after starting to record", null);
//				shutdown();
//				System.exit(1);
//			}
			msdServiceMainThreadHandler.postDelayed(dummyDataRunnable, 1000);
		}
	}
	public void getDynamicSms(){
		long currentTime = System.currentTimeMillis();
		int numSilentSms = 0, numBinarySms = 0;
		long lastSmsId = 0;

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(this));
		for(Event sms:dummyData.getDynamicSms()){
			if(sms.getTimestamp() > timeCallbacksDone && sms.getTimestamp() <= currentTime){
				dummyLogPrintStream.println("doPendingCallbacks(): Simulating sms at " + currentTime);
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
				df.updateSms(db,true);
				MsdDatabaseManager.getInstance().closeDatabase();
				if(sms.getType() == Event.Type.BINARY_SMS)
					numBinarySms++;
				else
					numSilentSms++;
				lastSmsId = sms.getId();
			}
		}
		if(numSilentSms > 0 || numBinarySms > 0){
			msdServiceNotifications.showSmsNotification(numSilentSms, numBinarySms, lastSmsId);
			sendStateChanged(StateChangedReason.SMS_DETECTED);
		}
		Vector<ImsiCatcher> notificationCachers = new Vector<ImsiCatcher>();
		for(ImsiCatcher imsi:dummyData.getPendingImsiCatchers()){
			if(imsi.getEndTime() > timeCallbacksDone && imsi.getEndTime() <= currentTime){
				dummyLogPrintStream.println("doPendingCallbacks(): Simulating IMSI Catcher at " + currentTime);
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
				df.updateImsi(db,true);
				MsdDatabaseManager.getInstance().closeDatabase();
				notificationCachers.add(imsi);
			}
		}
		if(notificationCachers.size() > 0){
			msdServiceNotifications.showImsiCatcherNotification(notificationCachers.size(), notificationCachers.lastElement().getId());
			sendStateChanged(StateChangedReason.CATCHER_DETECTED);
		}
		timeCallbacksDone = currentTime;
	}
	private boolean startRecording() {
		recording = true;
		timeCallbacksDone = serviceStartTime;
		dummyDataRunnable = new DummyDataRunnable();
		msdServiceMainThreadHandler.post(dummyDataRunnable);
		doStartForeground();
		sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
		return true;
	}
	
	private boolean shutdown() {
		dummyDataRunnable.recordingStopped = true;
		dummyDataRunnable = null;
		doStopForeground();
		sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
		return true;
	}
	private void sendStateChanged(StateChangedReason reason){
		Vector<IMsdServiceCallback> callbacksToRemove = new Vector<IMsdServiceCallback>();
		for(IMsdServiceCallback callback:mBinder.callbacks){
			try {
				callback.stateChanged(reason.name());
			} catch (DeadObjectException e) {
				Log.i(TAG,"DeadObjectException in MsdService.sendStateChanged() => unregistering callback");
				callbacksToRemove.add(callback);
			} catch (RemoteException e) {
				Log.e(TAG,"Exception in MsdService.sendStateChanged() => callback.recordingStateChanged();");
			}
		}
		mBinder.callbacks.removeAll(callbacksToRemove);
	}
	private void doStartForeground() {
		Notification notification = msdServiceNotifications.getForegroundNotification();
		startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, notification);
	}
	private void doStopForeground(){
		stopForeground(true);
	}
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG,"MsdService.onBind() called");
		return mBinder;
	}
	@Override
	public void onCreate() {
		super.onCreate();
		dummyData = new DummyAnalysisEventData(this);
		serviceStartTime = System.currentTimeMillis();
		dummyData.addDynamicDummyEvents(serviceStartTime);
	}
}
