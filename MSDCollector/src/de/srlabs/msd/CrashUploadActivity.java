package de.srlabs.msd;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.upload.DumpFile;
import de.srlabs.msd.util.MsdDatabaseManager;

/**
 * This Activity can be opened from an Android Notification after a fatal error
 * occurred. It works independently from the main App (and therefore in another
 * process).
 * 
 * It provides the possibility to upload the debug logfile for the crash to
 * report the bug.
 * 
 */
public class CrashUploadActivity extends Activity
{
	public static String EXTRA_ERROR_TEXT = "ERROR_TEXT";
	public static String EXTRA_ERROR_ID = "ERROR_ID";
	private String errorText;
	private long fileId;
	private TextView textView1;
	private Button btnUpload;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(CrashUploadActivity.this));
		setContentView(R.layout.activity_crash_upload);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Bundle extras = getIntent().getExtras();
		textView1 = (TextView) findViewById(R.id.textView1);
		btnUpload = (Button) findViewById(R.id.btnUpload);
		errorText = extras.getString(EXTRA_ERROR_TEXT);
		fileId = extras.getLong(EXTRA_ERROR_ID);
		if(fileId == 0){
			// Creating an error log failed, so we have to fall back to adb logcat.
			// TODO: Add an email address for submitting adb logs
			textView1.setText("Something went wrong with collecting a crash report. Please run 'adb logcat -v time > log.txt' and submit the resulting log to TODO\n" + errorText);
			btnUpload.setEnabled(false);
		} else{
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			DumpFile df = DumpFile.get(db, fileId);
			MsdDatabaseManager.getInstance().closeDatabase();
			textView1.setText("Error file: " + df.getFilename() + "\nReport ID: " + df.getReportId() + "\n" + errorText);
		}
		btnUpload.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
				DumpFile df = DumpFile.get(db, fileId);
				df.markForUpload(db);
				MsdDatabaseManager.getInstance().closeDatabase();
				// TODO: Start actual upload activity now, at the moment the file is only marked for upload
			}
		});
	}
}
