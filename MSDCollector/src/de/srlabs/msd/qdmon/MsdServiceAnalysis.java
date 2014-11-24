package de.srlabs.msd.qdmon;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.qdmon.MsdServiceNotifications;

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

	public static void runCatcherAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){
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

			// TODO: Notify UI
			//notifications.showImsiCatcherNotification(numResults, after);
		}
	}

	public static void runSMSAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){
		int before, after;

		String[] sms_cols = new String[]
				{"count(CASE WHEN sms_type = 0 THEN 1 ELSE 0 END)",
				 "count(CASE WHEN sms_type = 1 THEN 1 ELSE 0 END)"};

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

			Log.i(TAG,"SMSAnalysis: " + numResults + " new results, " + silent + "silent and " + binary + "binary");

			// TODO: Signal to UI
			//notifications.showSmsNotification(silent, binary, after);
		}
	}

	public static void run2GAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_2g.sql", false);
		// TODO: Signal result to UI
	}

	public static void run3GAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_3g.sql", false);
		// TODO: Signal result to UI
	}

	public static void runAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){

		Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		String time = String.format(Locale.US, "%02d:%02d:%02d",
				start.get(Calendar.HOUR_OF_DAY),
				start.get(Calendar.MINUTE),
				start.get(Calendar.SECOND));
		runCatcherAnalysis(context,db,notifications);
		runSMSAnalysis(context,db,notifications);
		run3GAnalysis(context,db,notifications);
		run2GAnalysis(context,db,notifications);
		Calendar done = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long endTime = done.getTimeInMillis();

		Log.i(TAG,time + ": Analysis took " + (endTime - start.getTimeInMillis()) + "ms");
	}
}