package de.srlabs.msd.analysis;

import java.util.Vector;

import de.srlabs.msd.upload.DumpFile;
import android.database.sqlite.SQLiteDatabase;

public interface AnalysisEvent {
	public static final int STATE_AVAILABLE = 0;
	public static final int STATE_UPLOADED = 1;
	public static final int STATE_DELETED = 2;
	public int getUploadState(SQLiteDatabase db);
	public Vector<DumpFile> getFiles(SQLiteDatabase db);
	public void markForUpload(SQLiteDatabase db);
}
