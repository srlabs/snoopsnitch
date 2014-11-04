package de.srlabs.msd.qdmon;

import java.util.Vector;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import de.srlabs.msd.analysis.AnalysisCallback;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;

public class MsdServiceHelper implements MsdServiceHelperInterface{
	private static String TAG = "msd-upload-service-helper";
	private Context context;
	private ServiceConnection serviceConnection = new MyServiceConnection();
	private Messenger msgMsdService;
	private Messenger     returnMessenger     = new Messenger(new ReturnHandler());
	private MsdServiceCallback callback;
	Vector<AnalysisCallback> analysisCallbacks = new Vector<AnalysisCallback>();
	
	public MsdServiceHelper(Context context, MsdServiceCallback callback){
		this.context = context;
		this.callback = callback;
	}
	@Override
	public boolean startRecording(){
		// TODO
		return false;
	}
	@Override
	public boolean stopRecording(){
		// TODO
		return false;
	}
	@Override
	public boolean isRecording(){
		// TODO;
		return false;
	}
	@Override
	public boolean restartRecording(){
		if(!isRecording())
			return false;
		if(!stopRecording())
			return false;
		return startRecording();
	}
	@Override
	public void registerAnalysisCallback(AnalysisCallback aCallback){
		if(analysisCallbacks.contains(aCallback))
			return;
		analysisCallbacks.add(aCallback);
	}

	@Override
	public void unregisterAnalysisCallback(AnalysisCallback aCallback){
		analysisCallbacks.remove(aCallback);
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
	@Override
	public SMS getSMS(long id) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Vector<SMS> getSMS(long startTime, long endTime) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ImsiCatcher getImsiCatcher(long id) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime) {
		// TODO Auto-generated method stub
		return null;
	}
}
