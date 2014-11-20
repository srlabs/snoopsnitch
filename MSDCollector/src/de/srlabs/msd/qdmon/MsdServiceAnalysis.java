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

import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;

public class MsdServiceAnalysis {

	private static String TAG = "MsdServiceAnalysis";

	public static void runAnalysis(Context context, SQLiteDatabase db, MsdServiceNotifications notifications){

		Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		String time = String.format(Locale.US, "%02d:%02d:%02d",
				start.get(Calendar.HOUR_OF_DAY),
				start.get(Calendar.MINUTE),
				start.get(Calendar.SECOND));
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "catcher_analysis.sql", false);
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "sms_analysis.sql", false);
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_2g.sql", false);
		MsdSQLiteOpenHelper.readSQLAsset(context, db, "sm_3g.sql", false);

		Calendar done = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long endTime = done.getTimeInMillis();

		Log.i(TAG,time + ": Analysis took " +(endTime - start.getTimeInMillis()) + "ms");
	}
}
