package de.srlabs.msd;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;

/**
 * This Service does the actual file uploading. It is controlled via the
 * database (which contains a table of pending file uploads filled by other
 * components of the App) and Messages. It should be running in a separate
 * Android process (via android:process in the Android Manifest) so that the UI
 * doesn't block while uploading.
 * 
 * Please use the class UploadServiceHelper to communicate with this Service.
 * 
 */
public class UploadService extends Service{
	public static final String TAG = "msd-upload-service";
	/**
	 * This message triggers uploading of all pending uploads in the database
	 * table pending_uploads.
	 */
	public static final int MSG_START_UPLOADING = 1;
	/**
	 * Stop the running upload operation.
	 */
	public static final int MSG_STOP_UPLOADING = 2;
	/**
	 * Sent from UploadService to the client Messenger (msg.replyTo of the
	 * MSG_START_UPLOADING message which started the uploading).
	 */
	public static final int MSG_UPLOAD_STATE = 3;
	
	private UploadState uploadState;
	private boolean stopUploading = false;
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG,"MsdService.onBind() called");
		return this.serviceMessenger.getBinder();
	}
	final Messenger serviceMessenger = new Messenger(new IncomingHandler());
	public Messenger replyTo;
	public UploadThread uploadThread;
	@SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_START_UPLOADING:
				stopUploading = false;
				Log.i(TAG,"UploadService.IncomingHandler.handleMessage(MSG_START_UPLOADING)");
				if(uploadThread != null){
					Log.e(TAG, "UploadService received MSG_START_UPLOADING but uploadThread!=null");
					return;
				}
				replyTo = msg.replyTo;
				uploadThread = new UploadThread();
				uploadThread.start();
				break;
			case MSG_STOP_UPLOADING:
				Log.i(TAG,"UploadService.IncomingHandler.handleMessage(MSG_STOP_UPLOADING)");
				stopUploading = true;
				break;
			default:
				Log.e(TAG,"UploadService.IncomingHandler.handleMessage(unknown message: " + msg.what + ")");
			}
		}
	}
	class UploadThread extends Thread{
		@Override
		public void run() {
			do_uploads();
		}
	}
	public void do_uploads() {
		uploadState = UploadServiceHelper.createUploadState(this);
		uploadState.setState(UploadState.State.RUNNING);
		for(String filename: uploadState.getAllFiles()){
			uploadFile(filename);
			if(stopUploading && uploadState.getState() != UploadState.State.FAILED){
				uploadState.setState(UploadState.State.STOPPED);
				sendState();
				break;
			}
			sendState();
			if(uploadState.getState() == UploadState.State.FAILED)
				break;
		}
		if(uploadState.getState() == UploadState.State.RUNNING){
			// All files uploaded and no error
			uploadState.setState(UploadState.State.COMPLETED);
			sendState();
		}
		this.uploadThread = null;
		this.stopSelf();
	}
	private void sendState() {
		Message msg = Message.obtain(null,MSG_UPLOAD_STATE);
		Bundle b = new Bundle();
		b.putSerializable("UPLOAD_STATE", uploadState);
		msg.setData(b);
		try {
			replyTo.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG,"RemoteException in UploadService.sendState()",e);
		}
	}
	private void uploadFile(String filename) {
		try {
			if(stopUploading)
				return;
			HttpsURLConnection connection = Utils.openUrlWithPinning(this, Constants.UPLOAD_URL);
			connection.setConnectTimeout((int) Constants.CONNECT_TIMEOUT);
			connection.setReadTimeout((int) Constants.READ_TIMEOUT);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("content-type",
					"multipart/form-data;boundary=" + Constants.MULTIPART_BOUNDARY);
			connection.setInstanceFollowRedirects(false);
			connection.setDoOutput(true);
			connection.connect();

			FileInputStream is = openFileInput(filename);

			final OutputStream os = connection.getOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(os);

			out.write("--" + Constants.MULTIPART_BOUNDARY + Constants.CRLF);
			out.write("Content-Disposition: form-data; name=\"opaque\"" + Constants.CRLF);
			out.write(Constants.CRLF);
			out.write("1" + Constants.CRLF);

			out.write("--" + Constants.MULTIPART_BOUNDARY + Constants.CRLF);
			out.write("Content-Disposition: form-data; name=\"bursts\"; filename=\"" + Utils.getAppId() + "_" + filename
					+ "\""
					+ Constants.CRLF);
			out.write(Constants.CRLF);
			out.flush();
			if(stopUploading)
				return;
			byte[] buffer = new byte[32 * 1024];
			int n = 0;
			int counter = 0;
			while (-1 != (n = is.read(buffer))) {
				counter += n;
				Log.d(TAG, "Upload counter: " + counter);
				os.write(buffer, 0, n);
				os.flush();
				if(stopUploading)
					return;
			}

			out.write(Constants.CRLF);
			out.write("--" + Constants.MULTIPART_BOUNDARY + "--" + Constants.CRLF);
			out.flush();

			if(stopUploading)
				return;
			final int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(this);
				SQLiteDatabase db = msdSQLiteOpenHelper.getWritableDatabase();
				Cursor c = db.query("pending_uploads",null,"filename=?", new String[]{filename},null,null,null);
				if(! c.moveToFirst()){
					uploadState.error("Failed to find pending_upload for file " + filename + " in database");
					return;
				}
				boolean delete = c.getInt(c.getColumnIndex("delete_after_uploading")) == 0 ? false : true;
				if(delete)
					deleteFile(filename);
				int numDeleted = db.delete("pending_uploads","filename=?", new String[]{filename});
				if(numDeleted != 1){
					uploadState.error("Failed to delete file " + filename + " in pending_upload");
					return;
				}
				uploadState.addCompletedFile(filename, counter);
				Log.i(TAG, "uploading file " + filename + " succeeded");
			} else{
				String errorStr = "Invalid response code: " + responseCode + " while uplaoding " + filename;
				uploadState.error(errorStr);
				Log.e(TAG,errorStr);
			}
		} catch (Exception e) {
			String errorStr = "Exception while uploading " + filename + ": " + e.getMessage();
			uploadState.error(errorStr);
			Log.e(TAG,errorStr,e);
		}
	}

}
