package de.srlabs.msd.qdmon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MsdServiceAnalysis {
	private static String TAG = "MsdServiceAnalysis";
	public static void runAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){
		Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		String time = String.format(Locale.US, "%02d:%02d:%02d",
				start.get(Calendar.HOUR_OF_DAY),
				start.get(Calendar.MINUTE),
				start.get(Calendar.SECOND));
		readSQLAsset(context, db, "catcher_analysis.sql", false);
		readSQLAsset(context, db, "sms_analysis.sql", false);
		readSQLAsset(context, db, "sm_2g.sql", false);
		readSQLAsset(context, db, "sm_3g.sql", false);
		db.close();

		Calendar done = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long endTime = done.getTimeInMillis();

		Log.i(TAG,time + ": Analysis took " +(endTime - start.getTimeInMillis()) + "ms");
	}
	private static void readSQLAsset(Context context, SQLiteDatabase db, String file, Boolean verbose) {
		Log.i(TAG,"MsdServiceAnalysis.readSQLAsset(" + file + ") called");
		db.execSQL("BEGIN TRANSACTION;");
		try {
			InputStream sqlInputStream = context.getAssets().open(file);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			int i;
			try {
				i = sqlInputStream.read();
				while (i != -1)
				{
					byteArrayOutputStream.write(i);
					i = sqlInputStream.read();
				}
				sqlInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			String sql = byteArrayOutputStream.toString();
			if (verbose){
				Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + "): " + sql);
			}
			for(String statement:sql.split(";")){
				if(statement.trim().length() > 0 && !statement.trim().startsWith("/*!")) {
					if (verbose){
						Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + "): statement=" + statement);
					}
					db.execSQL(statement);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		db.execSQL("COMMIT;");
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + ") done");
	}
}
