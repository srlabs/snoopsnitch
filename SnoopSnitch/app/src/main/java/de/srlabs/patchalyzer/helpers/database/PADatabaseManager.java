package de.srlabs.patchalyzer.helpers.database;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

// Class for concurrent database access.
// Cf. http://dmytrodanylyk.com/pages/blog/concurrent-database.html
public class PADatabaseManager {
    private static final String TAG = "PADatabaseManager";
    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static PADatabaseManager instance;
    private static PASQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    private int numFailedDBRetrievingTries = 0;
    private static final int MAX_FAILED_DB_RETRV_TRIES = 3;
    private static final int SECONDS_DELAY_BETWEEN_DB_RETRV_TRY = 1;

    public static synchronized void initializeInstance(PASQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new PADatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    public static synchronized PADatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(PADatabaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }

        return instance;
    }

    public synchronized SQLiteDatabase openDatabase() throws IllegalStateException {
        if (mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            try {
                mDatabase = mDatabaseHelper.getWritableDatabase();
            }
            catch (SQLException e){
                Log.e(TAG,"SQLException when trying to retrieve writeable DB object: "+e.getMessage());

                numFailedDBRetrievingTries++;
                if(numFailedDBRetrievingTries <= MAX_FAILED_DB_RETRV_TRIES){
                    // try again after a predefined delay
                    try {
                        Thread.sleep(1000L * SECONDS_DELAY_BETWEEN_DB_RETRV_TRY);
                    } catch (InterruptedException e1) {
                        Log.w(TAG,"Sleeping before retrying to access DB again was interrupted: " + e1.getMessage());
                    }

                    Log.w(TAG,"Trying to retrieve writeabele DB object again.");
                    return openDatabase();
                }
                else{
                   // cause App crash
                   throw new IllegalStateException("Retrieving writeable DB object not possible.");
                }
            }
        }
        return mDatabase;
    }

    public synchronized SQLiteDatabase openDatabaseReadOnly() throws IllegalStateException {
        if (mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            try {
                mDatabase = mDatabaseHelper.getReadableDatabase();
            }
            catch (SQLException e){
                Log.e(TAG,"SQLException when trying to retrieve readable DB object: "+e.getMessage());

                numFailedDBRetrievingTries++;
                if(numFailedDBRetrievingTries <= MAX_FAILED_DB_RETRV_TRIES){
                    // try again after a predefined delay
                    try {
                        Thread.sleep(1000L * SECONDS_DELAY_BETWEEN_DB_RETRV_TRY);
                    } catch (InterruptedException e1) {
                        Log.w(TAG,"Sleeping before retrying to access DB again was interrupted: " + e1.getMessage());
                    }

                    Log.w(TAG,"Trying to retrieve readable DB object again.");
                    return openDatabaseReadOnly();
                }
                else{
                    // cause App crash
                    throw new IllegalStateException("Retrieving writeable DB object not possible.");
                }
            }
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }
}
