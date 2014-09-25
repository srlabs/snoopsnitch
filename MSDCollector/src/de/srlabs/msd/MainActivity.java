package de.srlabs.msd;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import de.srlabs.msdcollector.R;

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
		@Override
		public void handleMessage(final Message msg) {
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					switch (msg.what) {
					case MsdService.MSG_RECORDING_STATE:
						appendLogMsg("RECORDING_STATE: " + msg.arg1);
						break;
					case MsdService.MSG_ERROR_STR:
						appendLogMsg(msg.obj.toString());
						break;
					case MsdService.MSG_NEW_SESSION:
						appendLogMsg("Received MSG_NEW_SESSION, would now trigger IMSI catcher analysis");
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
