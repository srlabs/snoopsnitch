package de.srlabs.msd.qdmon;

import java.util.Vector;

import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;

public class DummyAnalysisEventData implements AnalysisEventDataInterface {
	private Vector<SMS> existingSms = new Vector<SMS>();
	private Vector<SMS> pendingSms = new Vector<SMS>();
	private long nextSmsId = 1;
	private long nextImsiId = 1;
	private Vector<ImsiCatcher> existingImsiCatchers = new Vector<ImsiCatcher>();
	private Vector<ImsiCatcher> pendingImsiCatchers = new Vector<ImsiCatcher>();

	public DummyAnalysisEventData(){
		// A few dummy binary/silent SMS in the past
		existingSms.add(new SMS(1413387583L*1000L, nextSmsId++, 262, 1, 2, 3, "012345678", SMS.Type.BINARY_SMS));
		existingSms.add(new SMS(1413819629L*1000L, nextSmsId++, 262, 1, 2, 3, "012345678", SMS.Type.SILENT_SMS));
		existingSms.add(new SMS(1414251645L*1000L, nextSmsId++, 262, 1, 2, 3, "012345678", SMS.Type.BINARY_SMS));
		existingSms.add(new SMS(1414683660L*1000L, nextSmsId++, 262, 1, 2, 3, "012345678", SMS.Type.SILENT_SMS));
		// A few dummy IMSI Catchers in the past
		existingImsiCatchers.add(new ImsiCatcher(1413301619L*1000L, nextImsiId++, 262, 1, 2, 3, 0.2));
		existingImsiCatchers.add(new ImsiCatcher(1413733700L*1000L, nextImsiId++, 262, 1, 2, 3, 0.6));
		existingImsiCatchers.add(new ImsiCatcher(1414165711L*1000L, nextImsiId++, 262, 1, 2, 3, 0.9));
		existingImsiCatchers.add(new ImsiCatcher(1414597721L*1000L, nextImsiId++, 262, 1, 2, 3, 0.2));
	}
	public void addDynamicDummyEvents(long startRecordingTime){
		// One binary SMS 5 seconds after starting to record
		pendingSms.add(new SMS(startRecordingTime + 5000, nextSmsId++, 262, 1, 2, 3, "012345678", SMS.Type.BINARY_SMS));
		
		// One IMSI Catcher 15 seconds after starting to record
		pendingImsiCatchers.add(new ImsiCatcher(startRecordingTime + 15000, nextImsiId++, 262, 1, 2, 3, 0.2));
	}


	@Override
	public SMS getSMS(long id){
		for(SMS sms:existingSms){
			if(sms.getId() == id)
				return sms;
		}
		for(SMS sms:pendingSms){
			if(sms.getTimestamp() < System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(sms.getId() == id)
				return sms;
		}
		return null;
	}

	@Override
	public Vector<SMS> getSMS(long startTime, long endTime){
		Vector<SMS> result = new Vector<SMS>();
		for(SMS sms:existingSms){
			if(sms.getTimestamp() >= startTime && sms.getTimestamp() <= endTime)
				result.add(sms);
		}
		for(SMS sms:pendingSms){
			if(sms.getTimestamp() < System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(sms.getTimestamp() >= startTime && sms.getTimestamp() <= endTime)
				result.add(sms);
		}
		return result;
	}

	@Override
	public ImsiCatcher getImsiCatcher(long id){
		for(ImsiCatcher imsi:existingImsiCatchers){
			if(imsi.getId() == id)
				return imsi;
		}
		for(ImsiCatcher imsi:pendingImsiCatchers){
			if(imsi.getEndTime() < System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(imsi.getId() == id)
				return imsi;
		}
		return null;
	}

	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime){
		Vector<ImsiCatcher> result = new Vector<ImsiCatcher>();
		for(ImsiCatcher imsi:existingImsiCatchers){
			if(imsi.getEndTime() >= startTime && imsi.getStartTime() <= endTime)
				result.add(imsi);
		}
		for(ImsiCatcher imsi:pendingImsiCatchers){
			if(imsi.getEndTime() < System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(imsi.getEndTime() >= startTime && imsi.getStartTime() <= endTime)
				result.add(imsi);
		}
		return result;
	}
	public Vector<SMS> getPendingSms() {
		return pendingSms;
	}
	public Vector<ImsiCatcher> getPendingImsiCatchers() {
		return pendingImsiCatchers;
	}
	public void clearPendingEvents() {
		pendingSms = new Vector<SMS>();
		pendingImsiCatchers = new Vector<ImsiCatcher>();
	}
}
