package de.srlabs.snoopsnitch.util;

import java.util.concurrent.atomic.AtomicInteger;

import android.database.sqlite.SQLiteDatabase;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;

// Class for concurrent database access.
// Cf. http://dmytrodanylyk.com/pages/blog/concurrent-database.html
public class MsdDatabaseManager {

    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static MsdDatabaseManager instance;
    private static MsdSQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    public static synchronized void initializeInstance(MsdSQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new MsdDatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    public static synchronized MsdDatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(MsdDatabaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }

        return instance;
    }

    public synchronized SQLiteDatabase openDatabase() {
        if(mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if(mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();

        }
    }
}
