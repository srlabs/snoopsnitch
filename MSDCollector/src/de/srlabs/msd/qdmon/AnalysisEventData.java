package de.srlabs.msd.qdmon;

import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;
import android.util.Log;
import de.srlabs.msd.analysis.GSMmap;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.RAT;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.analysis.SMS.Type;
import de.srlabs.msd.util.MsdDatabaseManager;
import de.srlabs.msd.util.Utils;

public class AnalysisEventData implements AnalysisEventDataInterface{
	private SQLiteDatabase db;
	private Context context;

	public AnalysisEventData(Context context) {

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		this.db = MsdDatabaseManager.getInstance().openDatabase();
		this.context = context;

		GSMmap gsmmap = new GSMmap(context);

		// TODO: Factor out GSMmap data handling into own class.
		if (!gsmmap.dataPresent()) {
			try {
				gsmmap.parse(Utils.readFromAssets(context, "data.js"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
			throw new IllegalStateException("Requesting non-existing SMS " + Long.toString(id));
		}
		SMS result = smsFromCursor (c);
		c.close();
		return result;
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
		c.close();
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
		ImsiCatcher result = catcherFromCursor (c);
		c.close();
		return result;
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
		c.close();
		return result;
	}

	@Override
	public Risk getScores() {
		Operator operator = new Operator(db);
		return new Risk(db, operator.getMcc(), operator.getMnc());
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
