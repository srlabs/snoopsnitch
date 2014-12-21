package de.srlabs.snoopsnitch.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.qdmon.MsdService;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.Utils;

public class MsdServiceUploadThread extends Thread {
	private static final String TAG = "MsdServiceUploadThread";
	private UploadState uploadState;
	private MsdService msdService;
	private boolean stopUploading;
	private boolean newUploadRoundRequested = false;
	
	public MsdServiceUploadThread(MsdService msdService) {
		this.msdService = msdService;
	}
	public void requestUploadRound(){
		newUploadRoundRequested = true;
	}
	
	@Override
	public void run() {
		newUploadRoundRequested = false;
		MsdLog.i(TAG, "MsdServiceUploadThread starting first upload round");
		do_pending_uploads();
		while(newUploadRoundRequested){
			MsdLog.i(TAG, "MsdServiceUploadThread starting another upload round due to newUploadRoundRequested");
			newUploadRoundRequested = false;
			do_pending_uploads();
		}
		MsdLog.i(TAG, "MsdServiceUploadThread terminating");
	}
	public void do_pending_uploads() {
		uploadState = createUploadState(msdService);
		if(uploadState.getAllFiles().length == 0){
			MsdLog.i(TAG, "do_pending_uploads(): Nothing to upload");
			return;
		}
		uploadState.setState(UploadState.State.RUNNING);
		for(DumpFile file: uploadState.getAllFiles()){
			uploadFile(file);
			if(stopUploading && uploadState.getState() != UploadState.State.FAILED){
				uploadState.setState(UploadState.State.STOPPED);
				break;
			}
			if(uploadState.getState() == UploadState.State.FAILED)
				break;
		}
		if(uploadState.getState() == UploadState.State.RUNNING){
			// All files uploaded and no error
			uploadState.setState(UploadState.State.COMPLETED);
		}
	}
	private void uploadFile(DumpFile file) {
		try {
			if(stopUploading)
				return;
			String uploadFileName = MsdConfig.getAppId(msdService) + "_" + file.getReportId() + "_" + file.getFilename();
			MsdLog.i(TAG, "Starting to upload file " + file.getFilename() + " as " + uploadFileName);
			HttpsURLConnection connection = Utils.openUrlWithPinning(msdService, Constants.UPLOAD_URL);
			connection.setConnectTimeout((int) Constants.CONNECT_TIMEOUT);
			connection.setReadTimeout((int) Constants.READ_TIMEOUT);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("content-type",
					"multipart/form-data;boundary=" + Constants.MULTIPART_BOUNDARY);
			connection.setInstanceFollowRedirects(false);
			connection.setDoOutput(true);
			connection.connect();

			FileInputStream is = msdService.openFileInput(file.getFilename());

			final OutputStream os = connection.getOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(os);

			out.write("--" + Constants.MULTIPART_BOUNDARY + Constants.CRLF);
			out.write("Content-Disposition: form-data; name=\"opaque\"" + Constants.CRLF);
			out.write(Constants.CRLF);
			out.write("1" + Constants.CRLF);

			out.write("--" + Constants.MULTIPART_BOUNDARY + Constants.CRLF);
			out.write("Content-Disposition: form-data; name=\"bursts\"; filename=\""  + uploadFileName + "\"" + Constants.CRLF);
			out.write(Constants.CRLF);
			out.flush();
			if(stopUploading)
				return;
			byte[] buffer = new byte[32 * 1024];
			int n = 0;
			int counter = 0;
			while (-1 != (n = is.read(buffer))) {
				counter += n;
				MsdLog.i(TAG,"Upload counter: " + counter);
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
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
				if(!file.updateState(db, DumpFile.STATE_PENDING,  DumpFile.STATE_UPLOADED, null)){
					logUploadError("Failed to change state for file " + file.getFilename() + " from STATE_PENDING to STATE_UPLOADED");
					return;
				}
				MsdDatabaseManager.getInstance().closeDatabase();
				msdService.deleteFile(file.getFilename());
				uploadState.addCompletedFile(file, counter);
				MsdLog.i(TAG, "uploading file " + file.getFilename() + " succeeded");
			} else{
				String errorStr = "Invalid response code: " + responseCode + " while uplaoding " + file.getFilename();
				logUploadError(errorStr);
			}
		} catch (Exception e) {
			String errorStr = "Exception while uploading " + file.getFilename() + ": " + e.getMessage();
			logUploadError(errorStr);
		}
	}

	void logUploadError(String errorStr){
		uploadState.error(errorStr);
		MsdLog.e(TAG,errorStr);
	}
	public static UploadState createUploadState(Context context){
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		Vector<DumpFile> files = DumpFile.getFiles(db, "state = " + DumpFile.STATE_PENDING);
		long totalSize = 0;
		for(DumpFile file: files){
			File f = new File(context.getFilesDir() + "/" + file.getFilename());
			totalSize += f.length();
		}
		MsdDatabaseManager.getInstance().closeDatabase();
		UploadState result = new UploadState(UploadState.State.IDLE, files.toArray(new DumpFile[0]), totalSize, 0, null);
		return result;
	}
}
