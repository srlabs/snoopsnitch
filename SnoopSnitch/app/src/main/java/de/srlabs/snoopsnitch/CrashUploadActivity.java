package de.srlabs.snoopsnitch;

import java.io.File;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.qdmon.MsdServiceCallback;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;
import de.srlabs.snoopsnitch.upload.DumpFile;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.MsdDialog;

/**
 * This Activity can be opened from an Android Notification after a fatal error
 * occurred. It works independently from the main App (and therefore in another
 * process).
 * 
 * It provides the possibility to upload the debug logfile for the crash to
 * report the bug.
 * 
 */
public class CrashUploadActivity extends Activity implements MsdServiceCallback
{
	private static String TAG = "CrashUploadActivity";
	public static String EXTRA_ERROR_TEXT = "ERROR_TEXT";
	public static String EXTRA_ERROR_ID = "ERROR_ID";
	private String errorText;
	private long fileId;
	private MsdServiceHelper helper;
	private boolean triggerUploadingPending = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(CrashUploadActivity.this));
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Bundle extras = getIntent().getExtras();
		errorText = extras.getString(EXTRA_ERROR_TEXT);
		fileId = extras.getLong(EXTRA_ERROR_ID);
		boolean noLogsAvailable = false;
		if(fileId == 0){
			noLogsAvailable = true;
		} else{
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			DumpFile df = DumpFile.get(db, fileId);
			MsdDatabaseManager.getInstance().closeDatabase();
			if(df == null){
				noLogsAvailable = true;
				Log.i(TAG, "Row " + fileId + " not found");
			} else if(df.getState() == DumpFile.STATE_UPLOADED){
				String msg = "The debug log for this problem has already been uploaded.\n" + errorText;
				MsdDialog.makeNotificationDialog(this, msg, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						quitApplication();
					}
				}, false).show();
			} else{
				File f = new File(getFilesDir() + "/" + df.getFilename());
				Log.i(TAG,"Full path: " + getFilesDir() + "/" + df.getFilename() + "  size=" + f.length());
				if(!f.exists() || f.length() < 100){ // The debug log must contain an smime and a gzip header, so we can ignore files below 100 bytes.
					noLogsAvailable = true;
					Log.i(TAG, "File does not exist or size < 100 bytes");
				} else{
					String msg = "Error file: " + df.getFilename() + "\nReport ID: " + df.getReportId() + "\n" + errorText;
					MsdDialog.makeConfirmationDialog(this, msg, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							upload();
						}
					}, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							cancelNotification();
							quitApplication();
						}
					}, new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							// Don't delete notification when pressing the back button
							quitApplication();
						}
					}, "Upload", "Cancel", true).show();
				}
			}
		}
		if(noLogsAvailable){
			String msg = "Something went wrong with collecting the problem report. Please run 'adb logcat -v time > log.txt' and submit the resulting log to gsmmap@lists.srlabs.de\n" + errorText;
			MsdDialog.makeNotificationDialog(this, msg, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					cancelNotification();
					quitApplication();
				}
			}, false).show();
		}
	}

	private void upload() {
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(this));
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		DumpFile df = DumpFile.get(db, fileId);
		Log.i(TAG, "calling markForUpload for file " + fileId + " name=" + df.getFilename() + " current state: " + df.getState());
		df.markForUpload(db);
		MsdDatabaseManager.getInstance().closeDatabase();
		triggerUploadingPending  = true;
		helper = new MsdServiceHelper(this, this);
	}

	@Override
	public void stateChanged(StateChangedReason reason) {
		if(triggerUploadingPending && helper.isConnected()){
			helper.triggerUploading();
			triggerUploadingPending = false;
			cancelNotification();
			quitApplication();
		}
	}

	@Override
	public void internalError(String msg) {
	}
	private void cancelNotification(){
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_ID_INTERNAL_ERROR);
	}
	protected void quitApplication ()
	{
		finish();
		System.exit(0);
	}
}
