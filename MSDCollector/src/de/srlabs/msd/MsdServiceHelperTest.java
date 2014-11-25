package de.srlabs.msd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.srlabs.msd.qdmon.MsdServiceCallback;
import de.srlabs.msd.qdmon.MsdServiceHelper;
import de.srlabs.msd.qdmon.StateChangedReason;
import de.srlabs.msd.upload.UploadServiceHelper;
import de.srlabs.msd.upload.UploadState;
import de.srlabs.msd.upload.UploadStateCallback;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msd.R;

public class MsdServiceHelperTest extends Activity implements MsdServiceCallback{
	private Button btnStart;
	private Button btnStop;
	private Button btnUpload;
	private TextView textView1;
	protected MsdServiceHelper msdServiceHelper;

	private void appendLogMsg(String newMsg){
		newMsg = newMsg.trim();
		textView1.append(newMsg + "\n");
		// find the amount we need to scroll.  This works by
		// asking the TextView's internal layout for the position
		// of the final line and then subtracting the TextView's height
		// http://stackoverflow.com/questions/3506696/auto-scrolling-textview-in-android-to-bring-text-into-view
		try{
			final int scrollAmount = textView1.getLayout().getLineTop(textView1.getLineCount()) - textView1.getHeight();
			// if there is no need to scroll, scrollAmount will be <=0
			if (scrollAmount > 0)
				textView1.scrollTo(0, scrollAmount);
			else
				textView1.scrollTo(0, 0);
		} catch(NullPointerException e){}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_msd_service_helper_test);
		this.btnStart = (Button) findViewById(R.id.btnStart);
		this.btnStop = (Button) findViewById(R.id.btnStop);
		this.btnUpload = (Button) findViewById(R.id.btnUpload);
		this.textView1 = (TextView)findViewById(R.id.textView1);
		msdServiceHelper = new MsdServiceHelper(MsdServiceHelperTest.this, MsdServiceHelperTest.this, false); // The last parameter determines whether to use the dummy service or not.
		this.btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(msdServiceHelper.isRecording()){
					appendLogMsg("Already recording");
					return;
				}
				String comptatibility = DeviceCompatibilityChecker.checkDeviceCompatibility();
				if(comptatibility != null){
					AlertDialog.Builder builder = new AlertDialog.Builder(MsdServiceHelperTest.this);
					builder
					.setTitle("Your phone is not compatible")
					.setMessage(comptatibility)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() 
					{
						@Override
						public void onClick(DialogInterface dialog, int which) 
						{       
							dialog.dismiss();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
				boolean result = msdServiceHelper.startRecording();
				appendLogMsg("startRecording() returned " + result);
			}
		});
		this.btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!msdServiceHelper.isRecording()){
					appendLogMsg("Recording not running");
					return;
				}
				msdServiceHelper.stopRecording();
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


	// @Override
	// protected void onPause() {
	// 	// Terminate the UI if the Activity goes to the background so that we can test reconnecting to an (already recording) Service
	// 	System.exit(1);
	// }

	@Override
	public void stateChanged(StateChangedReason reason) {
		final String msg = "recordingStateChanged connected=" + msdServiceHelper.isConnected() + "  recording=" + msdServiceHelper.isRecording();
		appendLogMsg(msg);
	}

	@Override
	public void internalError(String msg) {
		appendLogMsg("internalError(" + msg + ")");
	}
}
