package de.srlabs.patchalyzer.helpers.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.PowerManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PASQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "PASQLiteOpenHelper";
    private static final String DATABASE_NAME = "pa.db";
    private static final int DATABASE_VERSION = 11;
    private static final boolean verbose = false;
    private Context context;

    public PASQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public static void readSQLAsset(Context context, SQLiteDatabase db, String file, Boolean verbose) throws SQLException, IOException {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, file);

        Long tmp = System.currentTimeMillis();

        Log.i(TAG, "PASQLiteOpenHelper.readSQLAsset(" + file + ") called");

        try {
            wl.acquire();
            db.beginTransaction();
            String sql = readFromAssets(context, file);
            if (verbose) {
                Log.i(TAG, "PASQLiteOpenHelper.readSQLAsset(" + file + "): " + sql);
            }
            for (String statement : sql.split(";")) {
                if (statement.trim().length() > 0 && !statement.trim().startsWith("/*!")) {
                    if (verbose) {
                        Log.i(TAG, "PASQLiteOpenHelper.readSQLAsset(" + file + "): statement=" + statement);
                    }
                    long startTime = System.currentTimeMillis();
                    db.execSQL(statement);
                    if (verbose) {
                        long durationMs = System.currentTimeMillis() - startTime;
                        Log.i(TAG, "PASQLiteOpenHelper.readSQLAsset(" + file + "): statement took " + durationMs);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            wl.release();
        }
        Log.i(TAG, "PASQLiteOpenHelper.readSQLAsset(" + file + ") done, took " + (System.currentTimeMillis() - tmp) + "ms");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "PASQLiteOpenHelper.onCreate() called");
        try {
            readSQLAsset(context, db, "basictests.sql",verbose);
        } catch (Exception e) {
            Log.e("MSD", "Failed to create database", e);
        }
        Log.i(TAG, "PASQLiteOpenHelper.onCreate() done");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            readSQLAsset(context, db, "basictests.sql", verbose);
        }catch(Exception e){
            Log.e(TAG,"Failed to upgrade Patchalyzer basic tests tables",e);
        }
    }

    private static String readFromAssets(Context context, String file) throws IOException{
        String mTAG = "readFromAssets";
        InputStream inputStream = context.getAssets().open(file);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException ee) {
            Log.e(TAG, mTAG + ": IOException in readFromAssets():\n" + ee.toString());
        }
        return byteArrayOutputStream.toString();
    }
}
