package de.srlabs.msd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.srlabs.msd.qdmon.MsdService;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.upload.UploadServiceHelper;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msdcollector.R;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;

public class MainActivity extends Activity {
	private ServiceConnection serviceConnection = new MyServiceConnection();
	private Messenger msgMsdService;
	private Button btnStart;
	private Button btnStop;
	private Messenger     returnMessenger     = new Messenger(new ReturnHandler());
	private Button btnCleanup;
	private TextView textView1;

	class MyServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(MsdService.TAG,"MyServiceConnection.onServiceConnected()");
			msgMsdService = new Messenger(service);
			Message msg = Message.obtain(null, MsdService.MSG_REGISTER_CLIENT);
			msg.replyTo = returnMessenger;
			try {
				msgMsdService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			unbindService(this);
		}
	};
	class ReturnHandler extends Handler {
		private long last = 0;
		@Override
		public void handleMessage(final Message msg) {
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					switch (msg.what) {
					case MsdService.MSG_RECORDING_STATE:
						appendLogMsg("RECORDING_STATE: " + msg.arg1);
						break;
					case MsdService.MSG_ERROR_STR:
						Bundle b = (Bundle) msg.obj;
						String errorMsg = b.getString("ERROR_MSG");
						appendLogMsg(errorMsg);
						break;
					case MsdService.MSG_NEW_SESSION:

						Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

						// Run analysis every 10 seconds
						if (start.getTimeInMillis() - last > 10000)
						{
							String time = String.format(Locale.US, "%02d:%02d:%02d",
								start.get(Calendar.HOUR_OF_DAY),
								start.get(Calendar.MINUTE),
								start.get(Calendar.SECOND));

							// MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(MainActivity.this);
							// SQLiteDatabase db = msdSQLiteOpenHelper.getWritableDatabase();
							// msdSQLiteOpenHelper.readSQLAsset(db, "catcher_analysis.sql", false);
							// msdSQLiteOpenHelper.readSQLAsset(db, "sms_analysis.sql", false);
							// msdSQLiteOpenHelper.readSQLAsset(db, "sm_2g.sql", false);
							// msdSQLiteOpenHelper.readSQLAsset(db, "sm_3g.sql", false);
							// db.close();

							Calendar done = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
							last = done.getTimeInMillis();

							appendLogMsg(time + ": Analysis took " +
								(last - start.getTimeInMillis()) + "ms");
						}
						break;
					default:
						appendLogMsg("ReturnHandler: Unknown message " + msg.what);
					}
				}
			});
		}
	}
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
		setContentView(R.layout.activity_main);
		this.btnStart = (Button) findViewById(R.id.btnStart);
		this.btnStop = (Button) findViewById(R.id.btnStop);
		this.btnCleanup = (Button) findViewById(R.id.btnCleanup);
		this.textView1 = (TextView)findViewById(R.id.textView1);
		startService(new Intent(this, MsdService.class));
		bindService(new Intent(this, MsdService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
		this.btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String comptatibility = DeviceCompatibilityChecker.checkDeviceCompatibility();
				if(comptatibility != null){
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
				Message msg = Message.obtain(null, MsdService.MSG_START_RECORDING);
				msg.replyTo = returnMessenger;
				try {
					msgMsdService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		this.btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Message msg = Message.obtain(null, MsdService.MSG_STOP_RECORDING);
				msg.replyTo = returnMessenger;
				try {
					msgMsdService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		this.btnCleanup.setOnClickListener(new OnClickListener() {
			private UploadServiceHelper uploadServiceHelper;

			@Override
			public void onClick(View v) {
								Message msg = Message.obtain(null, MsdService.MSG_TRIGGER_CLEANUP);
								msg.replyTo = returnMessenger;
								try {
									msgMsdService.send(msg);
								} catch (RemoteException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
//				if(uploadServiceHelper == null){
//					UploadServiceHelper.createDummyUploadFiles(MainActivity.this, 3);
//					uploadServiceHelper = new UploadServiceHelper();
//					uploadServiceHelper.startUploading(MainActivity.this, new UploadStateCallback() {
//						@Override
//						public void uploadStateChanged(UploadState state) {
//							textView1.setText(state.toString());
//						}
//					});
//				} else{
//					uploadServiceHelper.stopUploading();
//					uploadServiceHelper = null;
//				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
