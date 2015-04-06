package de.srlabs.snoopsnitch.qdmon;

import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;
import de.srlabs.snoopsnitch.analysis.Event;
import de.srlabs.snoopsnitch.analysis.GSMmap;
import de.srlabs.snoopsnitch.analysis.ImsiCatcher;
import de.srlabs.snoopsnitch.analysis.RAT;
import de.srlabs.snoopsnitch.analysis.Risk;
import de.srlabs.snoopsnitch.analysis.Event.Type;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.Utils;

public class AnalysisEventData implements AnalysisEventDataInterface{
	private SQLiteDatabase db;
	private Context context;

	public AnalysisEventData(Context context) {

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		this.db = MsdDatabaseManager.getInstance().openDatabase();
		this.context = context;

		GSMmap gsmmap = new GSMmap(context);

		if (!gsmmap.dataPresent()) {
			try {
				String jsonData = Utils.readFromFileOrAssets(context, "app_data.json");
				gsmmap.parse(jsonData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static String[] events_cols =
			new String[] {"strftime('%s',timestamp)", "id", "mcc", "mnc", "lac", "cid", "latitude", "longitude", "valid", "msisdn", "smsc", "event_type"};

	static private Event eventFromCursor(Cursor c, Context context) {
		Type event_type;

		switch (c.getInt(11))
		{
			// Binary SMS
			case 1:	event_type = Type.BINARY_SMS;
					break;

			// Silent SMS
			case 2:	event_type = Type.SILENT_SMS;
					break;

			// Null paging
			case 3:	event_type = Type.NULL_PAGING;
					break;

			// Invalid type from database
			default: event_type = Type.INVALID_EVENT;
		}

		return new Event
				(c.getLong(0) * 1000L,	// timestamp
				 c.getLong(1),			// id
				 c.getInt(2),			// mcc
				 c.getInt(3),			// mnc
				 c.getInt(4),			// lac
				 c.getInt(5),			// cid
				 c.getDouble(6),		// latitude
				 c.getDouble(7),		// longitude
				 c.getShort(8) > 0,		// valid
				 c.getString(9),		// msisdn
				 c.getString(10),		// smsc
				 event_type,			// event type
				 context);
	}

	@Override
	public Event getEvent(long id) {
		Cursor c = db.query("events", events_cols, "id = ?", new String[] {Long.toString(id)}, null, null, null);
		if(!c.moveToFirst()) {
			throw new IllegalStateException("Requesting non-existing event" + Long.toString(id));
		}
		Event result = eventFromCursor (c, context);
		c.close();
		return result;
	}

	@Override
	public Vector<Event> getEvent(long startTime, long endTime) {

		Vector<Event> result = new Vector<Event>();

		Cursor c = db.query("events", events_cols, "strftime('%s',timestamp) >= ? AND strftime('%s',timestamp) <= ?",
				new String[] {Long.toString(startTime/1000), Long.toString(endTime/1000)}, null, null, null);

		if(c.moveToFirst()) {
			do {
				result.add(eventFromCursor(c, context));
			} while (c.moveToNext());
		}
		c.close();
		return result;
	}

	private static String[] catcher_cols =
			new String[]
					{"strftime('%s',timestamp)",
					 "strftime('%s',timestamp) + duration/1000",
					 "id",
					 "mcc",
					 "mnc",
					 "lac",
					 "cid",
					 "latitude",
					 "longitude",
					 "valid",
					 "score",
					 "a1",
					 "a2",
					 "a4",
					 "a5",
					 "k1",
					 "k2",
					 "c1",
					 "c2",
					 "c3",
					 "c4",
					 "c5",
					 "t1",
					 "t3",
					 "t4",
					 "r1",
					 "r2",
					 "f1"};

	static private ImsiCatcher catcherFromCursor(Cursor c, Context context) {

		return new ImsiCatcher
				(c.getLong(0)*1000L,	// startTime
				 c.getLong(1)*1000L,	// endTime
				 c.getInt(2),			// id
				 c.getInt(3),			// mcc
				 c.getInt(4),			// mnc
				 c.getInt(5),			// lac
				 c.getInt(6),			// cid
				 c.getDouble(7),		// latitude
				 c.getDouble(8),		// longitude
				 c.getShort(9) > 0,		// valid
				 c.getDouble(10),		// score
				 c.getDouble(11),		// a1
				 c.getDouble(12),		// a2
				 c.getDouble(13),		// a4
				 c.getDouble(14),		// a5
				 c.getDouble(15),		// k1
				 c.getDouble(16),		// k2
				 c.getDouble(17),		// c1
				 c.getDouble(18),		// c2
				 c.getDouble(19),		// c3
				 c.getDouble(20),		// c4
				 c.getDouble(21),		// c5
				 c.getDouble(22),		// t1
				 c.getDouble(23),		// t3
				 c.getDouble(24),		// t4
				 c.getDouble(25),		// r1
				 c.getDouble(26),		// r2
				 c.getDouble(27),		// f1
				 context);
	}

	@Override
	public ImsiCatcher getImsiCatcher(long id) {
		Cursor c = db.query("catcher", catcher_cols, "id = ?", new String[] {Long.toString(id)}, null, null, null);
		if(!c.moveToFirst()) {
			throw new IllegalStateException("Requesting non-existing IMSI catcher");
		}
		ImsiCatcher result = catcherFromCursor (c, context);
		c.close();
		return result;
	}

	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime) {

		ImsiCatcher catcher;
		Vector<ImsiCatcher> result = new Vector<ImsiCatcher>();

		Cursor c = db.query("catcher", catcher_cols, "strftime('%s',timestamp) >= ? AND strftime('%s',timestamp) <= ?",
				new String[] {Long.toString(startTime/1000), Long.toString(endTime/1000)}, null, null, null);

		if(c.moveToFirst()) {
			do {
				catcher = catcherFromCursor(c, context);
				result.add(catcher);
			} while (c.moveToNext());
		}
		c.close();
		return result;
	}

	@Override
	public Risk getScores() {
		Operator operator = new Operator(context);
		return new Risk(db, operator);
	}

	@Override
	public RAT getCurrentRAT() {
	    TelephonyManager mTelephonyManager = (TelephonyManager)
	            context.getSystemService(Context.TELEPHONY_SERVICE);
	    int networkType = mTelephonyManager.getNetworkType();
	    switch (Utils.networkTypeToNetworkGeneration(networkType)) {
	    	case 0:  return RAT.RAT_UNKNOWN;
	    	case 2:  return RAT.RAT_2G;
	    	case 3:  return RAT.RAT_3G;
	    	case 4:  return RAT.RAT_LTE;
	    	default: return RAT.RAT_UNKNOWN;
	    }
	}
}
