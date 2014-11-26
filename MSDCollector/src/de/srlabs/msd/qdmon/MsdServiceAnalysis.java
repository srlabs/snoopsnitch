package de.srlabs.msd.qdmon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.analysis.Risk2G;
import de.srlabs.msd.analysis.Risk3G;

public class MsdServiceAnalysis {

	private static String TAG = "MsdServiceAnalysis";

	private static int getLast(SQLiteDatabase db, String tableName, String rowName){
		try{
			Cursor c = db.rawQuery("SELECT MAX(" + rowName + ") FROM " +  tableName, null);
			if (!c.moveToFirst()){
				throw new IllegalStateException("Invalid SQL result");
			}
			return c.getInt(0);
		} catch(SQLException e){
			throw new IllegalStateException("SQLException in getLast(" + tableName + "," + rowName + ",): ", e);
		}
	}

	public static boolean runCatcherAnalysis(Context context, SQLiteDatabase db){
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

	public static boolean runSMSAnalysis(Context context, SQLiteDatabase db){
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

			Log.i(TAG,"SMSAnalysis: " + numResults + " new result(s), " + silent + " silent and " + binary + " binary");

			if (silent > 0 || binary > 0)
			{
				return true;
			}
		}
		return false;
	}

	private static class Operator {
		int mcc;
		int mnc;
		boolean valid = false;

		public int getMcc() {
			return mcc;
		}

		public int getMnc() {
			return mnc;
		}

		public boolean isValid() {
			return valid;
		}

		public Operator(SQLiteDatabase db) {
			Cursor c = db.query("serving_cell_info", new String[] {"max(_id)", "mcc", "mnc"}, null, null, null, null, null);
			if (c.moveToFirst()){
				mcc = c.getInt(1);
				mnc = c.getInt(2);
				valid = true;
			};
		}
	}


	public static void log(Risk2G before, Risk2G after){
		if (after.isValid()) {
			if (!after.equals(before)) {
				Log.i(TAG,"2GAnalysis: new values for " + after.getMonth() +
						": inter " + Double.toString(after.getInter()) +
						", imper " + Double.toString(after.getImper()) +
						", track " + Double.toString(after.getTrack()));
			} else {
				Log.i(TAG,"2GAnalysis: values unchanged");
			}
		} else
		{
			Log.i(TAG,"2GAnalysis: no valid result");
		}
	}

	public static boolean run2GAnalysis(Context context, SQLiteDatabase db){

		boolean result = false;
		Operator op = new Operator(db);

		if (op.isValid()){
			Log.i(TAG,"2GAnalysis for mcc=" + op.getMcc() + ", mnc=" + op.getMnc());
			Risk2G before = new Risk2G(db, op.getMcc(), op.getMnc());
			MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_2g.sql", false);
			Risk2G after = new Risk2G(db, op.getMcc(), op.getMnc());
			log(before, after);

			result = after.isValid() && !after.equals(before);
		}

		return result;
	}

	public static void log(Risk3G before, Risk3G after){
		if (after.isValid()) {
			if (!after.equals(before)) {
				Log.i(TAG,"3GAnalysis: new values for " + after.getMonth() +
						": inter " + Double.toString(after.getInter()) +
						", imper " + Double.toString(after.getImper()));
			} else {
				Log.i(TAG,"3GAnalysis: values unchanged");
			}
		} else
		{
			Log.i(TAG,"3GAnalysis: no valid result");
		}
	}

	public static boolean run3GAnalysis(Context context, SQLiteDatabase db){

		boolean result = false;
		Operator op = new Operator(db);

		if (op.isValid()){
			Log.i(TAG,"3GAnalysis for mcc=" + op.getMcc() + ", mnc=" + op.getMnc());
			Risk3G before = new Risk3G(db, op.getMcc(), op.getMnc());
			MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_3g.sql", false);
			Risk3G after = new Risk3G(db, op.getMcc(), op.getMnc());
			log(before, after);

			result = after.isValid() && !after.equals(before);
		}

		return result;
	}

}
