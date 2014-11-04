package de.srlabs.msd.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Vector;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.qdmon.MsdService;

public class UploadServiceHelper {
	private static String TAG = "msd-upload-service-helper";
	private Context context;
	private UploadStateCallback callback;
	private ServiceConnection serviceConnection = new MyServiceConnection();
	private Messenger msgMsdService;
	private Messenger     returnMessenger     = new Messenger(new ReturnHandler());
	
	public void startUploading(Context context, UploadStateCallback callback){
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
		MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(context);
		SQLiteDatabase db = msdSQLiteOpenHelper.getReadableDatabase();
		Cursor c = db.query("pending_uploads", null, null, null, null, null, "_id");
		Vector<String> files = new Vector<String>();
		long totalSize = 0;
		while(c.moveToNext()){
			String filename = c.getString(c.getColumnIndexOrThrow("filename"));
			files.add(filename);
			File f = new File(context.getFilesDir() + "/" + filename);
			totalSize += f.length();
		}
		c.close();
		db.close();
		UploadState result = new UploadState(UploadState.State.IDLE, files.toArray(new String[0]), totalSize, 0, null);
		return result;
	}
	/**
	 * Create some dummy files for testing the upload functionality
	 * @param context
	 * @param nFiles
	 */
	public static void createDummyUploadFiles(Context context, int nFiles){
		String prefix = "DUMMY_" + System.currentTimeMillis() + "_";
		for(int i=0;i<nFiles;i++){
			try {
				String filename = prefix + i;
				PrintStream ps = new PrintStream(context.openFileOutput(filename,0));
				ps.println("Dummy upload file");
				for(int j=0;j<1000;j++){
					ps.println("Dummy line " + j);
				}
				ps.close();
				addPendingUpload(context, filename, true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	public static void addPendingUpload(Context context, String filename, boolean deleteAfterUploading){
		MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(context);
		SQLiteDatabase db = msdSQLiteOpenHelper.getReadableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("filename", filename);
		cv.put("delete_after_uploading",deleteAfterUploading ? 1:0);
		db.insert("pending_uploads", null, cv);
		db.close();
	}
}
