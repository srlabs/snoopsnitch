package de.srlabs.snoopsnitch.qdmon;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.srlabs.snoopsnitch.analysis.Risk;

public class MsdServiceAnalysis {

	private static String TAG = "MsdServiceAnalysis";

	private static int getLast(SQLiteDatabase db, String tableName){
		try{
			Cursor c = db.rawQuery("SELECT * FROM " +  tableName, null);
			int result = c.getCount();
			c.close();
			return result;
		} catch(SQLException e){
			throw new IllegalStateException("SQLException in getLast(" + tableName + ",): ", e);
		}
	}

	public static int runCatcherAnalysis(Context context, SQLiteDatabase db) throws Exception{
		int before, after;

		before = getLast(db, "catcher");
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "catcher_analysis.sql", false);
		after = getLast(db, "catcher");

		if (after != before)
		{
			int numResults = after - before;

			// New analysis results
			Log.i(TAG,"CatcherAnalysis: " + numResults + " new catcher results");

			return after - before;
		}
		return 0;
	}

	public static int runEventAnalysis(Context context, SQLiteDatabase db) throws Exception{
		int before, after;

		String[] event_cols = new String[]
				{"sum(CASE WHEN event_type > 0 THEN 1 ELSE 0 END)"};

		before = getLast(db, "events");
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "event_analysis.sql", false);
		after = getLast(db, "events");

		if (after > before)
		{
			Cursor c = db.query
					("events",
					 event_cols,
					 "id > ? AND id <= ?",
					 new String[] {String.valueOf(before), String.valueOf(after)},
					 null, null, null);

			if (!c.moveToFirst()){
				throw new IllegalStateException("Invalid event result");
			}
			int numResults = c.getInt(0);
			c.close();
			Log.i(TAG,"EventAnalysis: " + numResults + " new result(s)");

			if (numResults > 0)
			{
				return numResults;
			}
		}
		return 0;
	}

	public static void log(Risk before, Risk after){
		if (after.changed(before)) {
			Log.i(TAG,"Security Analysis: new values");
		} else {
			Log.i(TAG,"Security Analysis: values unchanged");
		}
	}

	public static boolean runSecurityAnalysis(Context context, SQLiteDatabase db) throws Exception{

		boolean result = false;
		Operator op = new Operator(context);

		if (op.isValid()){
			Log.i(TAG,"Security Analysis for mcc=" + op.getMcc() + ", mnc=" + op.getMnc());
			Risk before = new Risk(db, op);
			MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_2g.sql", false);
			MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_3g.sql", false);
			Risk after = new Risk(db, op);
			log(before, after);

			result = after.changed(before);
		}

		return result;
	}
}
