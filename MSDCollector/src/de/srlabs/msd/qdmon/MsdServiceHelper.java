package de.srlabs.msd.qdmon;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import de.srlabs.msd.analysis.AnalysisCallback;

public class MsdServiceHelper {
	private static String TAG = "msd-upload-service-helper";
	private Context context;
	private ServiceConnection serviceConnection = new MyServiceConnection();
	private Messenger msgMsdService;
	private Messenger     returnMessenger     = new Messenger(new ReturnHandler());
	private MsdServiceCallback callback;
	
	public MsdServiceHelper(Context context, MsdServiceCallback callback){
		this.context = context;
		this.callback = callback;
	}
	public void startRecording(){
		// TODO
	}
	public void stopRecording(){
		// TODO
	}
	public boolean isRecording(){
		// TODO;
		return false;
	}
	public void restartRecording(){
		stopRecording();
		startRecording();
	}
	public void registerAnalysisCallback(AnalysisCallback callback){
		// TODO
	}
	public void unregisterAnalysisCallback(AnalysisCallback callback){
		// TODO
	}

	class MyServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO
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
			default:
				Log.e(TAG,"ReturnHandler: Unknown message " + msg.what);
			}
		}
	}
}
