package de.srlabs.msd.qdmon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.analysis.Risk;

public class MsdServiceAnalysis {

	private static String TAG = "MsdServiceAnalysis";

	private static int getLast(SQLiteDatabase db, String tableName, String rowName){
		try{
			Cursor c = db.rawQuery("SELECT MAX(" + rowName + ") FROM " +  tableName, null);
			if (!c.moveToFirst()){
				throw new IllegalStateException("Invalid SQL result");
			}
			int result = c.getInt(0);
			c.close();
			return result;
		} catch(SQLException e){
			throw new IllegalStateException("SQLException in getLast(" + tableName + "," + rowName + ",): ", e);
		}
	}

	public static boolean runCatcherAnalysis(Context context, SQLiteDatabase db) throws Exception{
		int before, after;

		before = getLast(db, "catcher", "id");
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "catcher_analysis.sql", false);
		after = getLast(db, "catcher", "id");

		//  We should never have fewer elements after analysis
		if (after < before)
		{
			throw new IllegalStateException("runCatcherAnalysis: Number of results decreased");
		}

		if (after > before)
		{
			int numResults = after - before;

			// New analysis results
			Log.i(TAG,"CatcherAnalysis: " + numResults + " new catcher results");

			return true;
		}
		return false;
	}

	public static boolean runSMSAnalysis(Context context, SQLiteDatabase db) throws Exception{
		int before, after;

		String[] sms_cols = new String[]
				{"sum(CASE WHEN sms_type = 0 THEN 1 ELSE 0 END)",
				 "sum(CASE WHEN sms_type = 1 THEN 1 ELSE 0 END)"};

		before = getLast(db, "sms", "id");
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "sms_analysis.sql", false);
		after = getLast(db, "sms", "id");

		if (after > before)
		{
			int numResults = after - before;
			int silent, binary;

			Cursor c = db.query
					("sms",
					 sms_cols,
					 "id > ? AND id <= ?",
					 new String[] {String.valueOf(before), String.valueOf(after)},
					 null, null, null);

			if (!c.moveToFirst()){
				throw new IllegalStateException("Invalid SQL result");
			}
			silent = c.getInt(1);
			binary = c.getInt(0);
			c.close();
			Log.i(TAG,"SMSAnalysis: " + numResults + " new result(s), " + silent + " silent and " + binary + " binary");

			if (silent > 0 || binary > 0)
			{
				return true;
			}
		}
		return false;
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
		Operator op = new Operator(db);

		if (op.isValid()){
			Log.i(TAG,"Security Analysis for mcc=" + op.getMcc() + ", mnc=" + op.getMnc());
			Risk before = new Risk(db, op.getMcc(), op.getMnc());
			MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_2g.sql", false);
			MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_3g.sql", false);
			Risk after = new Risk(db, op.getMcc(), op.getMnc());
			log(before, after);

			result = after.changed(before);
		}

		return result;
	}
}
