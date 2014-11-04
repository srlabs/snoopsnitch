package de.srlabs.msd.qdmon;

import java.util.Vector;

import android.content.Context;
import android.os.Handler;
import de.srlabs.msd.analysis.AnalysisCallback;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;

public class DummyMsdServiceHelper implements MsdServiceHelperInterface {
	Vector<SMS> allSms = new Vector<SMS>();
	Vector<ImsiCatcher> allImsiCatchers = new Vector<ImsiCatcher>();
	long timeCallbacksDone = 0;
	Vector<AnalysisCallback> analysisCallbacks = new Vector<AnalysisCallback>();
	private boolean recording = false;
	private Context context = null;
	private MsdServiceCallback callback = null;
	private Handler handler = null;
	private DummyDataRunnable dummyDataRunnable = new DummyDataRunnable();
	public DummyMsdServiceHelper(Context context, MsdServiceCallback callback){
		this.context = context;
		this.callback = callback;
		handler = new Handler();
		// A few dummy binary/silent SMS in the past
		allSms.add(new SMS(1413387583L*1000L, 1, 262, 1, 2, 3, "012345678", SMS.Type.BINARY_SMS));
		allSms.add(new SMS(1413819629L*1000L, allSms.lastElement().getId()+1, 262, 1, 2, 3, "012345678", SMS.Type.SILENT_SMS));
		allSms.add(new SMS(1414251645L*1000L, allSms.lastElement().getId()+1, 262, 1, 2, 3, "012345678", SMS.Type.BINARY_SMS));
		allSms.add(new SMS(1414683660L*1000L, allSms.lastElement().getId()+1, 262, 1, 2, 3, "012345678", SMS.Type.SILENT_SMS));
		// A few dummy IMSI Catchers in the past
		allImsiCatchers.add(new ImsiCatcher(1413301619L*1000L, 1, 262, 1, 2, 3, 0.2));
		allImsiCatchers.add(new ImsiCatcher(1413733700L*1000L, allImsiCatchers.lastElement().getId() + 1, 262, 1, 2, 3, 0.6));
		allImsiCatchers.add(new ImsiCatcher(1414165711L*1000L, allImsiCatchers.lastElement().getId() + 1, 262, 1, 2, 3, 0.9));
		allImsiCatchers.add(new ImsiCatcher(1414597721L*1000L, allImsiCatchers.lastElement().getId() + 1, 262, 1, 2, 3, 0.2));
	}
	private class DummyDataRunnable implements Runnable{
		@Override
		public void run() {
			if(!recording)
				return;
			doPendingCallbacks();
			handler.postDelayed(dummyDataRunnable, 1000);
		}
	}

	@Override
	public boolean startRecording(){
		long currentTime = System.currentTimeMillis();
		timeCallbacksDone = currentTime;
		// One binary SMS 5 seconds after starting to record
		allSms.add(new SMS(currentTime + 5000, allSms.lastElement().getId()+1, 262, 1, 2, 3, "012345678", SMS.Type.BINARY_SMS));
		
		// One IMSI Catcher 10 seconds after starting to record
		allImsiCatchers.add(new ImsiCatcher(currentTime + 10000, allImsiCatchers.lastElement().getId() + 1, 262, 1, 2, 3, 0.2));
		
		recording = true;
		callback.recordingStarted();
		handler.postDelayed(dummyDataRunnable, 1000);
		return true;
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
	public boolean isRecording() {
		return recording;
	}

	@Override
	public boolean stopRecording(){
		recording = false;
		callback.recordingStopped();
		return true;
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
	
	public void doPendingCallbacks(){
		if(!recording)
			return;
		long currentTime = System.currentTimeMillis();
		for(SMS sms:allSms){
			if(sms.getTimestamp() > timeCallbacksDone && sms.getTimestamp() <= currentTime){
				for(AnalysisCallback callback:analysisCallbacks){
					callback.smsDetected(sms);
				}
			}
		}
		for(ImsiCatcher imsi:allImsiCatchers){
			if(imsi.getEndTime() > timeCallbacksDone && imsi.getEndTime() <= currentTime){
				for(AnalysisCallback callback:analysisCallbacks){
					callback.imsiCatcherDetected(imsi);
				}
			}
		}
		timeCallbacksDone = currentTime;
	}

	@Override
	public SMS getSMS(long id){
		for(SMS sms:allSms){
			if(sms.getId() == id)
				return sms;
		}
		return null;
	}

	@Override
	public Vector<SMS> getSMS(long startTime, long endTime){
		Vector<SMS> result = new Vector<SMS>();
		for(SMS sms:allSms){
			if(sms.getTimestamp() < System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(sms.getTimestamp() >= startTime && sms.getTimestamp() <= endTime)
				result.add(sms);
		}
		return result;
	}

	@Override
	public ImsiCatcher getImsiCatcher(long id){
		for(ImsiCatcher imsi:allImsiCatchers){
			if(imsi.getId() == id)
				return imsi;
		}
		return null;
	}

	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime){
		Vector<ImsiCatcher> result = new Vector<ImsiCatcher>();
		for(ImsiCatcher imsi:allImsiCatchers){
			if(imsi.getEndTime() < System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(imsi.getEndTime() >= startTime && imsi.getStartTime() <= endTime)
				result.add(imsi);
		}
		return result;
	}
}
