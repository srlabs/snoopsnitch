package de.srlabs.msd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.srlabs.msd.analysis.AnalysisCallback;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.qdmon.DummyMsdServiceHelper;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.qdmon.MsdServiceCallback;
import de.srlabs.msd.qdmon.MsdServiceHelperInterface;
import de.srlabs.msd.upload.UploadServiceHelper;
import de.srlabs.msd.upload.UploadState;
import de.srlabs.msd.upload.UploadStateCallback;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msdcollector.R;

public class MsdServiceHelperTest extends Activity implements MsdServiceCallback, AnalysisCallback{
	private Button btnStart;
	private Button btnStop;
	private Button btnUpload;
	private TextView textView1;
	protected MsdServiceHelperInterface msdServiceHelper;

	private void appendLogMsg(String newMsg){
		newMsg = newMsg.trim();
		textView1.append(newMsg + "\n");
		// find the amount we need to scroll.  This works by
		// asking the TextView's internal layout for the position
		// of the final line and then subtracting the TextView's height
		// http://stackoverflow.com/questions/3506696/auto-scrolling-textview-in-android-to-bring-text-into-view
		final int scrollAmount = textView1.getLayout().getLineTop(textView1.getLineCount()) - textView1.getHeight();
		// if there is no need to scroll, scrollAmount will be <=0
		if (scrollAmount > 0)
			textView1.scrollTo(0, scrollAmount);
		else
			textView1.scrollTo(0, 0);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_msd_service_helper_test);
		this.btnStart = (Button) findViewById(R.id.btnStart);
		this.btnStop = (Button) findViewById(R.id.btnStop);
		this.btnUpload = (Button) findViewById(R.id.btnUpload);
		this.textView1 = (TextView)findViewById(R.id.textView1);
		this.btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String comptatibility = DeviceCompatibilityChecker.checkDeviceCompatibility();
				if(comptatibility != null){
					AlertDialog.Builder builder = new AlertDialog.Builder(MsdServiceHelperTest.this);
					builder
					.setTitle("Your phone is not compatible")
					.setMessage(comptatibility)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog, int which) 
						{       
							dialog.dismiss();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
				msdServiceHelper = new DummyMsdServiceHelper(MsdServiceHelperTest.this, MsdServiceHelperTest.this);
				msdServiceHelper.registerAnalysisCallback(MsdServiceHelperTest.this);
				msdServiceHelper.startRecording();
			}
		});
		this.btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(msdServiceHelper == null){
					appendLogMsg("Recording not yet started");
					return;
				}
				msdServiceHelper.stopRecording();
				msdServiceHelper = null;
			}
		});
		this.btnUpload.setOnClickListener(new OnClickListener() {
			private UploadServiceHelper uploadServiceHelper;

			@Override
			public void onClick(View v) {
				if(uploadServiceHelper == null || !uploadServiceHelper.isUploadRunnung()){
					uploadServiceHelper = new UploadServiceHelper();
					uploadServiceHelper.startUploading(MsdServiceHelperTest.this, new UploadStateCallback() {
						@Override
						public void uploadStateChanged(UploadState state) {
							textView1.setText(state.toString());
						}
					});
				} else{
					uploadServiceHelper.stopUploading();
					uploadServiceHelper = null;
				}
			}
		});
	}

	@Override
	public void recordingStarted() {
		appendLogMsg("recordingStarted()");
	}

	@Override
	public void recordingStopped() {
		appendLogMsg("recordingStopped()");
	}

	@Override
	public void internalError(String errorMsg) {
		appendLogMsg("internalError: " + errorMsg);
	}

	@Override
	public void error(int id) {
		appendLogMsg("error: " + id);
	}

	@Override
	public void smsDetected(SMS sms) {
		appendLogMsg("SMS: " + sms);
		MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(this);
		SQLiteDatabase db = msdSQLiteOpenHelper.getReadableDatabase();
		sms.markForUpload(db);
		db.close();
	}

	@Override
	public void imsiCatcherDetected(ImsiCatcher imsiCatcher) {
		appendLogMsg("IMSI: " + imsiCatcher);
		MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(this);
		SQLiteDatabase db = msdSQLiteOpenHelper.getReadableDatabase();
		imsiCatcher.markForUpload(db);
		db.close();
	}
}
