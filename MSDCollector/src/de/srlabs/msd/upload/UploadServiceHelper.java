package de.srlabs.msd.upload;

import java.io.File;
import java.util.Vector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.qdmon.MsdService;
import de.srlabs.msd.util.MsdDatabaseManager;

public class UploadServiceHelper {
	private static String TAG = "msd-upload-service-helper";
	private Context context;
	private UploadStateCallback callback;
	private boolean uploadRunnung = false;
	private ServiceConnection serviceConnection = new MyServiceConnection();
	private Messenger msgMsdService;
	private Messenger     returnMessenger     = new Messenger(new ReturnHandler());
	
	public void startUploading(Context context, UploadStateCallback callback){
		if(uploadRunnung)
			throw new IllegalStateException("UploadServiceHelper.startUploading() called while already uploading");
		uploadRunnung = true;
		this.context = context;
		this.callback = callback;
		context.startService(new Intent(context, UploadService.class));
		context.bindService(new Intent(context, UploadService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
	}
	public void stopUploading(){
		Message msg = Message.obtain(null, UploadService.MSG_STOP_UPLOADING);
		try {
			msgMsdService.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG,"RemoteException in UploadServiceHelper.MyServiceConnection.onServiceConnected()");
		}
	}


	class MyServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(MsdService.TAG,"MyServiceConnection.onServiceConnected()");
			msgMsdService = new Messenger(service);
			Message msg = Message.obtain(null, UploadService.MSG_START_UPLOADING);
			msg.replyTo = returnMessenger;
			try {
				msgMsdService.send(msg);
			} catch (RemoteException e) {
				Log.e(TAG,"RemoteException in UploadServiceHelper.MyServiceConnection.onServiceConnected()");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			context.unbindService(this);
		}
	};
	class ReturnHandler extends Handler {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case UploadService.MSG_UPLOAD_STATE:
				UploadState state = (UploadState)msg.getData().getSerializable("UPLOAD_STATE");
				uploadRunnung = state.getState() == UploadState.State.RUNNING;
				callback.uploadStateChanged(state);
			default:
				Log.e(TAG,"ReturnHandler: Unknown message " + msg.what);
			}
		}
	}

	/**
	 * This static method creates an UploadState object with the files from the
	 * pending_uploads database table. It can be used by UploadService (to crate
	 * an initial UploadState) as well as the UI to get information about
	 * pending uploads before actually starting the upload.
	 * 
	 * @param context
	 * @return
	 */
	public static UploadState createUploadState(Context context){
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		Vector<DumpFile> files = DumpFile.getFiles(db, "state = " + DumpFile.STATE_PENDING);
		long totalSize = 0;
		// TODO: Maybe we need to add STATE_RECORDING_PENDING and add a mechanism to automatically restart recording
		for(DumpFile file: files){
			File f = new File(context.getFilesDir() + "/" + file.getFilename());
			totalSize += f.length();
		}
		MsdDatabaseManager.getInstance().closeDatabase();
		UploadState result = new UploadState(UploadState.State.IDLE, files.toArray(new DumpFile[0]), totalSize, 0, null);
		return result;
	}
	public boolean isUploadRunnung() {
		return uploadRunnung;
	}
}
