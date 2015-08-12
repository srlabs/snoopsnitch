package de.srlabs.snoopsnitch.qdmon;

import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.PowerManager;
import android.util.Log;
import de.srlabs.snoopsnitch.util.Utils;

public class MsdSQLiteOpenHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "msd.db";
	private static final int DATABASE_VERSION = 22;
	private static final boolean verbose = false;
	private Context context;
	public MsdSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	public static void readSQLAsset(Context context, SQLiteDatabase db, String file, Boolean verbose) throws SQLException, IOException {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, file);

		Long tmp = System.currentTimeMillis();

		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + ") called");

		try {
			wl.acquire();
			db.beginTransaction();
			String sql = Utils.readFromAssets(context, file);
			if (verbose){
				Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + "): " + sql);
			}
			for(String statement:sql.split(";")){
				if(statement.trim().length() > 0 && !statement.trim().startsWith("/*!")) {
					if (verbose){
						Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + "): statement=" + statement);
					}
					long startTime = System.currentTimeMillis();
					db.execSQL(statement);
					if(verbose){
						long durationMs = System.currentTimeMillis() - startTime;
						Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + "): statement took " + durationMs);
					}
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			wl.release();
		}
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.readSQLAsset(" + file + ") done, took " + (System.currentTimeMillis() - tmp) + "ms");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.onCreate() called");
		try{
			readSQLAsset(context, db, "si.sql", verbose);
			readSQLAsset(context, db, "si_loc.sql", verbose);
			readSQLAsset(context, db, "sm.sql", verbose);
			readSQLAsset(context, db, "cell_info.sql", verbose);
			readSQLAsset(context, db, "sms.sql", verbose);
			readSQLAsset(context, db, "config.sql", verbose);
			readSQLAsset(context, db, "mcc.sql", verbose);
			readSQLAsset(context, db, "mnc.sql", verbose);
			readSQLAsset(context, db, "hlr_info.sql", verbose);
			readSQLAsset(context, db, "analysis_tables.sql", verbose);
			readSQLAsset(context, db, "local.sqlx", verbose);
			readSQLAsset(context, db, "files.sql", verbose);
		} catch(Exception e){
			Log.e("MSD","Failed to create database",e);
		}
		Log.i(MsdService.TAG,"MsdSQLiteOpenHelper.onCreate() done");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		if (oldVersion == 16 && newVersion > oldVersion) {
			db.execSQL("ALTER TABLE gsmmap_inter ADD COLUMN size DOUBLE DEFAULT 0.0");
			db.execSQL("ALTER TABLE gsmmap_imper ADD COLUMN size DOUBLE DEFAULT 0.0");
			db.execSQL("ALTER TABLE gsmmap_track ADD COLUMN size DOUBLE DEFAULT 0.0");
			db.execSQL("ALTER TABLE gsmmap_inter3G ADD COLUMN size DOUBLE DEFAULT 0.0");
			db.execSQL("ALTER TABLE gsmmap_imper3G ADD COLUMN size DOUBLE DEFAULT 0.0");

			// Force rebuild of GSMmap database entries
			db.execSQL("DELETE FROM gsmmap_operators");
		}

		// A number of new fields have been introduced into the sms_meta table and a
		// new table sid_appid was added.
		if (oldVersion <= 17 && newVersion > oldVersion) {
			// We add the new columns, but do not convert the old content.
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN concat_frag smallint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN concat_total smallint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN src_port smallint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN dst_port smallint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_iei tinyint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_enc tinyint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_enc_algo tinyint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_sign tinyint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_sign_algo tinyint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_counter tinyint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_counter_value CHAR(10) DEFAULT NULL");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_tar CHAR(6) DEFAULT NULL");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN ota_por smallint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN udh_length smallint NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE sms_meta ADD COLUMN real_length smallint NOT NULL DEFAULT 0");

			//  New table sid_appid added
			db.execSQL("CREATE TABLE sid_appid (sid integer PRIMARY KEY, appid char(8) NOT NULL)");
		}

		// Several tables were changed in version 18
		if (oldVersion <= 18 && newVersion > oldVersion) {

			try {
				readSQLAsset(context, db, "upgrade_18.sqlx", verbose);
				readSQLAsset(context, db, "sm.sql", verbose);
				readSQLAsset(context, db, "mcc.sql", verbose);
				readSQLAsset(context, db, "mnc.sql", verbose);
				readSQLAsset(context, db, "analysis_tables.sql", verbose);
			} catch(Exception e){
				Log.e("MSD","Failed to update database",e);
			}

		}
		if (oldVersion <= 21 && newVersion > oldVersion) {
			try {
				readSQLAsset(context, db, "config.sql", verbose);
			} catch(Exception e){
				Log.e("MSD","Failed to update database",e);
			}
		}
	}
}
