package de.srlabs.msd.qdmon;

import java.util.Calendar;
import java.util.Vector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.RAT;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.analysis.Event;
import de.srlabs.msd.analysis.GSMmap;
import de.srlabs.msd.util.MsdDatabaseManager;
import de.srlabs.msd.util.Utils;

public class DummyAnalysisEventData implements AnalysisEventDataInterface {
	private Vector<Event> existingSms = new Vector<Event>();
	private Vector<Event> dynamicSms = new Vector<Event>();
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
				String data = Utils.readFromAssets(context, "app_data.json");
				gsmmap.parse(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -5);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, false, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -8);
        cal.add(Calendar.HOUR, -4);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -8);
        cal.add(Calendar.HOUR, 4);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -12);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -14);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -2);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, false, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));

        cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -24);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, false, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
       
        cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -50);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));

        cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -60);
        existingSms.add(new Event(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
		
		
		// A few dummy binary/silent SMS in the past
		//existingSms.add(new SMS(1413387583L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.BINARY_SMS));
		//existingSms.add(new SMS(1413819629L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.SILENT_SMS));
		//existingSms.add(new SMS(1414251645L*1000L, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.BINARY_SMS));
//		existingSms.add(new SMS(cal.getTimeInMillis(), nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, "012345678", "+4912345678", SMS.Type.SILENT_SMS));
		
		
		
		// A few dummy IMSI Catchers in the past
		existingImsiCatchers.add(new ImsiCatcher(1413301619L*1000L, 1413301619L*1000L + 120, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, false, 3.2, context));
		existingImsiCatchers.add(new ImsiCatcher(1413733700L*1000L, 1413733700L*1000L + 240, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, true, 4.6, context));
		existingImsiCatchers.add(new ImsiCatcher(1414165711L*1000L, 1414165711L*1000L + 60,  nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, true, 3.9, context));
		existingImsiCatchers.add(new ImsiCatcher(1414597721L*1000L, 1414597721L*1000L + 400, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, false, 2.2, context));
	}
	public void addDynamicDummyEvents(long startRecordingTime, Context context){
		// One binary SMS 5 seconds after starting to record
		dynamicSms.add(new Event(startRecordingTime + 5000, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, true, "012345678", "+4912345678", Event.Type.BINARY_SMS, context));
		dynamicSms.add(new Event(startRecordingTime + 8000, nextSmsId++, 262, 1, 3, 2, 52.52437, 13.41053, false, "012345678", "+4912345678", Event.Type.SILENT_SMS, context));
		
		// One IMSI Catcher 15 seconds after starting to record
		dynamicImsiCatchers.add(new ImsiCatcher(startRecordingTime + 15000, startRecordingTime + 16000, nextImsiId++, 262, 1, 2, 3, 52.52437, 13.41053, true, 4.2, context));
	}


	@Override
	public Event getEvent(long id){
		for(Event sms:existingSms){
			if(sms.getId() == id)
				return sms;
		}
		for(Event sms:dynamicSms){
			if(sms.getTimestamp() > System.currentTimeMillis())
				continue; // Ignore dummy events which have not yet been recorded
			if(sms.getId() == id)
				return sms;
		}
		return null;
	}

	@Override
	public Vector<Event> getEvent(long startTime, long endTime){
		Vector<Event> result = new Vector<Event>();
		for(Event sms:existingSms){
			if(sms.getTimestamp() >= startTime && sms.getTimestamp() <= endTime)
				result.add(sms);
		}
		for(Event sms:dynamicSms){
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
	public Vector<Event> getDynamicSms() {
		return dynamicSms;
	}
	public Vector<ImsiCatcher> getPendingImsiCatchers() {
		return dynamicImsiCatchers;
	}
	public void clearDynamicEvents() {
		dynamicSms = new Vector<Event>();
		dynamicImsiCatchers = new Vector<ImsiCatcher>();
	}
	@Override
	public Risk getScores() {
		// Vodafone Germany
//		return new Risk(db, 262, 2);
		return new Risk(db, 405, 47);  // India
//		return new Risk(db, 240, 24);  // Sweden
//		return new Risk(db, 452, 7);   // Vietnam
//		return new Risk(db, 401, 77);  // Kazakhstan 
//		return new Risk(db, 302, 720); // Canada
//		return new Risk(db, 242, 5);   // Norway

	}
	@Override
	public RAT getCurrentRAT() {
		// Simulate 2G only for now
		return RAT.RAT_2G;
	}
}