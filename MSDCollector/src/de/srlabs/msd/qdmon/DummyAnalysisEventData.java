package de.srlabs.msd.qdmon;

import java.util.Vector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.RAT;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.analysis.GSMmap;
import de.srlabs.msd.util.MsdDatabaseManager;
import de.srlabs.msd.util.Utils;

public class DummyAnalysisEventData implements AnalysisEventDataInterface {
	private Vector<SMS> existingSms = new Vector<SMS>();
	private Vector<SMS> dynamicSms = new Vector<SMS>();
	private long nextSmsId = 1;
	private long nextImsiId = 1;
	private Vector<ImsiCatcher> existingImsiCatchers = new Vector<ImsiCatcher>();
	private Vector<ImsiCatcher> dynamicImsiCatchers = new Vector<ImsiCatcher>();
	private SQLiteDatabase db;

	public DummyAnalysisEventData(Context context){

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		this.db = MsdDatabaseManager.getInstance().openDatabase();

		GSMmap gsmmap = new GSMmap(context);
		if (!gsmmap.dataPresent()) {
			try {
				String data = Utils.readFromAssets(context, "data.js");
				gsmmap.parse(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// A few dummy binary/silent SMS in the past
		existingSms.add(new SMS(1413387583L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.BINARY_SMS));
		existingSms.add(new SMS(1413819629L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.SILENT_SMS));
		existingSms.add(new SMS(1414251645L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.BINARY_SMS));
		existingSms.add(new SMS(1414683660L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.SILENT_SMS));
		// A few dummy IMSI Catchers in the past
		existingImsiCatchers.add(new ImsiCatcher(1413301619L*1000L, 1413301619L*1000L + 120, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, 3.2));
		existingImsiCatchers.add(new ImsiCatcher(1413733700L*1000L, 1413733700L*1000L + 240, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, 4.6));
		existingImsiCatchers.add(new ImsiCatcher(1414165711L*1000L, 1414165711L*1000L + 60,  nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, 3.9));
		existingImsiCatchers.add(new ImsiCatcher(1414597721L*1000L, 1414597721L*1000L + 400, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, 2.2));
	}
	public void addDynamicDummyEvents(long startRecordingTime){
		// One binary SMS 5 seconds after starting to record
		dynamicSms.add(new SMS(startRecordingTime + 5000, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.BINARY_SMS));
		dynamicSms.add(new SMS(startRecordingTime + 8000, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.SILENT_SMS));
		
		// One IMSI Catcher 15 seconds after starting to record
		dynamicImsiCatchers.add(new ImsiCatcher(startRecordingTime + 15000, startRecordingTime + 16000, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, 4.2));
	}


	@Override
	public SMS getSMS(long id){
		for(SMS sms:existingSms){
			if(sms.getId() == id)
				return sms;
		}
		for(SMS sms:dynamicSms){
			if(sms.getTimestamp() > System.currentTimeMillis())
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
		for(SMS sms:dynamicSms){
			if(sms.getTimestamp() > System.currentTimeMillis())
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
		for(ImsiCatcher imsi:dynamicImsiCatchers){
			if(imsi.getEndTime() > System.currentTimeMillis())
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
		for(ImsiCatcher imsi:dynamicImsiCatchers){
			if(imsi.getEndTime() > System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(imsi.getEndTime() >= startTime && imsi.getStartTime() <= endTime)
				result.add(imsi);
		}
		return result;
	}
	public Vector<SMS> getDynamicSms() {
		return dynamicSms;
	}
	public Vector<ImsiCatcher> getPendingImsiCatchers() {
		return dynamicImsiCatchers;
	}
	public void clearDynamicEvents() {
		dynamicSms = new Vector<SMS>();
		dynamicImsiCatchers = new Vector<ImsiCatcher>();
	}
	@Override
	public Risk getScores() {
		// Vodafone Germany
		return new Risk(db, 262, 2);
	}
	@Override
	public RAT getCurrentRAT() {
		// Simulate 2G only for now
		return RAT.RAT_2G;
	}
}