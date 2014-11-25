package de.srlabs.msd.qdmon;

import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.analysis.SMS.Type;
import de.srlabs.msd.util.MsdDatabaseManager;

public class AnalysisEventData implements AnalysisEventDataInterface{
	private Context context;
	private SQLiteDatabase db;

	public AnalysisEventData(Context context) {

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));

		this.context = context;
		this.db = MsdDatabaseManager.getInstance().openDatabase();
	}

	private static String[] sms_cols =
			new String[] {"strftime('%s',timestamp)", "id", "mcc", "mnc", "lac", "cid", "latitude", "longitude", "smsc", "msisdn", "sms_type"};

	static private void logCatcher(ImsiCatcher c) {

			Log.i("CATCHER","Catcher: " + c.getStartTime() +
					", ID="  + c.getId() +
					", MCC=" + c.getMcc() +
					", MNC=" + c.getMnc() +
					", LAC=" + c.getLac() +
					", CID=" + c.getCid() +
					", Score=" + c.getScore()
					);
	}

	static private SMS smsFromCursor(Cursor c) {
		Type sms_type;

		switch (c.getInt(10))
		{
			// Binary SMS
			case 0:	sms_type = Type.BINARY_SMS;
					break;

			// Silent SMS
			case 1:	sms_type = Type.SILENT_SMS;
					break;

			// Invalid type from database
			default: sms_type = Type.INVALID_SMS;
		}

		return new SMS
				(c.getLong(0),		// timestamp
				 c.getLong(1),		// id
				 c.getInt(2),		// mcc
				 c.getInt(3),		// mnc
				 c.getInt(4),		// lac
				 c.getInt(5),		// cid
				 c.getDouble(6),	// latitude
				 c.getDouble(7),	// longitude
				 c.getString(8),	// smsc
				 c.getString(9),	// msisdn
				 sms_type			// SMS type
				);
	}

	@Override
	public SMS getSMS(long id) {
		Cursor c = db.query("sms", sms_cols, "id = ?", new String[] {Long.toString(id)}, null, null, null);
		if(!c.moveToFirst()) {
			throw new IllegalStateException("Requesting non-existing SMS");
		}
		return smsFromCursor (c);
	}

	@Override
	public Vector<SMS> getSMS(long startTime, long endTime) {

		Vector<SMS> result = new Vector<SMS>();

		Cursor c = db.query("sms", sms_cols, "strftime('%s',timestamp) >= ? AND strftime('%s',timestamp) <= ?",
				new String[] {Long.toString(startTime), Long.toString(endTime)}, null, null, null);

		if(c.moveToFirst()) {
			do {
				result.add(smsFromCursor(c));
			} while (c.moveToNext());
		}
		return result;
	}

	private static String[] catcher_cols =
			new String[] {"strftime('%s',timestamp)", "strftime('%s',timestamp) + duration/1000", "id", "mcc", "mnc", "lac", "cid", "latitude", "longitude", "score"};

	static private ImsiCatcher catcherFromCursor(Cursor c) {

		return new ImsiCatcher
				(c.getLong(0),		// startTime
				 c.getLong(1),		// endTime
				 c.getInt(2),		// id
				 c.getInt(3),		// mcc
				 c.getInt(4),		// mnc
				 c.getInt(5),		// lac
				 c.getInt(6),		// cid
				 c.getDouble(7),	// latitude
				 c.getDouble(8),	// longitude
				 c.getDouble(9) 	// score
				);
	}

	@Override
	public ImsiCatcher getImsiCatcher(long id) {
		Cursor c = db.query("catcher", catcher_cols, "id = ?", new String[] {Long.toString(id)}, null, null, null);
		if(!c.moveToFirst()) {
			throw new IllegalStateException("Requesting non-existing IMSI catcher");
		}
		return catcherFromCursor (c);
	}

	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime) {

		ImsiCatcher catcher;
		Vector<ImsiCatcher> result = new Vector<ImsiCatcher>();

		Cursor c = db.query("catcher", catcher_cols, "strftime('%s',timestamp) >= ? AND strftime('%s',timestamp) <= ?",
				new String[] {Long.toString(startTime), Long.toString(endTime)}, null, null, null);

		if(c.moveToFirst()) {
			do {
				catcher = catcherFromCursor(c);
				logCatcher(catcher);
				result.add(catcher);
			} while (c.moveToNext());
		}
		return result;
	}
}
