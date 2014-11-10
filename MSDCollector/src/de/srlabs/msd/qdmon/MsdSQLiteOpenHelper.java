package de.srlabs.msd.qdmon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MsdSQLiteOpenHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "msd.db";
	private static final int DATABASE_VERSION = 7;
	private Context context;
	public MsdSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	public void readSQLAsset(SQLiteDatabase db, String file, Boolean verbose) {
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + ") called");
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
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + ") done");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.onCreate() called");
		readSQLAsset(db, "si.sql", true);
		readSQLAsset(db, "cell_info.sql", true);
		readSQLAsset(db, "sms.sql", true);
		readSQLAsset(db, "config.sql", true);
		readSQLAsset(db, "mcc.sql", true);
		readSQLAsset(db, "mnc.sql", true);
		readSQLAsset(db, "hlr_info.sql", true);
		readSQLAsset(db, "local.sqlx", true);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// The si.sql statement already contains a "DROP TABLE IF EXISTS" statement
		// Upgrading the database will delete all data
		//db.execSQL("DROP TABLE IF EXISTS session_info;");
		onCreate(db);
	}

}
