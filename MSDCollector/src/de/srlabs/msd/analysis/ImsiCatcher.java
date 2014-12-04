package de.srlabs.msd.analysis;

import java.util.Vector;

import android.database.sqlite.SQLiteDatabase;
import de.srlabs.msd.upload.DumpFile;
import de.srlabs.msd.upload.FileState;
import de.srlabs.msd.util.MsdDatabaseManager;


public class ImsiCatcher implements AnalysisEvent{
	private long startTime;
	private long endTime;
	private long id;
	private int mcc;
	private int mnc;
	private int lac;
	private int cid;
	private double latitude;
	private double longitude;
	private double score;
	SQLiteDatabase db;

	public ImsiCatcher(long startTime, long endTime, long id, int mcc,
			int mnc, int lac, int cid, double latitude, double longitude, double score) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.id = id;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.latitude = latitude;
		this.longitude = longitude;
		this.score = score;
		db = MsdDatabaseManager.getInstance().openDatabase();
	}

	/**
	 * Mark raw data related to this IMSI catcher for upload
	 */
	public void upload() {
		DumpFile.markForUpload(db, DumpFile.TYPE_ENCRYPTED_QDMON, startTime, endTime, 0);
	}

	/**
	 * Return upload state of IMSI catcher object
	 * @return
	 */
	public FileState getUploadState() {
		return DumpFile.getState(db, DumpFile.TYPE_ENCRYPTED_QDMON, startTime, endTime, 0);
	}

	/**
	 * Start time when the IMSI Catcher was detected
	 * @return
	 */
	public long getStartTime() {
		return startTime;
	}
	/**
	 * End time when the IMSI Catcher was detected
	 * @return
	 */
	public long getEndTime() {
		return endTime;
	}
	/**
	 * id column of the silent sms in table session_info, can be used to retrieve the IMSI Catcher again using get(long id)
	 * @return
	 */
	public long getId() {
		return id;
	}
	/**
	 * MCC when the IMSI Catcher was received
	 * @return
	 */
	public int getMcc() {
		return mcc;
	}
	/**
	 * MNC when the IMSI Catcher was received
	 * @return
	 */
	public int getMnc() {
		return mnc;
	}
	public int getLac() {
		return lac;
	}
	public int getCid() {
		return cid;
	}
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	/**
	 * Score for the IMSI catcher
	 * @return
	 */
	public double getScore() {
		return score;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("ImsiCatcher: ID=" + id);
		// TODO: Add more fields
		return result.toString();
	}
	@Override
	public int getUploadState(SQLiteDatabase db){
		boolean uploaded = false;
		for(DumpFile file:getFiles(db)){
			if(file.getState() == DumpFile.STATE_AVAILABLE)
				return STATE_AVAILABLE;
			if(file.getState() == DumpFile.STATE_PENDING || file.getState() == DumpFile.STATE_RECORDING_PENDING || file.getState() == DumpFile.STATE_UPLOADED)
				uploaded = true;
		}
		if(uploaded)
			return STATE_UPLOADED;
		else
			return STATE_DELETED;
	}
	@Override
	public Vector<DumpFile> getFiles(SQLiteDatabase db){
		return DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, startTime, endTime, 0);
	}
	@Override
	public void markForUpload(SQLiteDatabase db){
		for(DumpFile file:getFiles(db)){
			file.markForUpload(db);
		}
	}

	public String getFullCellID() {
		return Integer.toString(mcc) +
				"/" + Integer.toString(mnc) +
				"/" + Integer.toString(lac) +
				"/" + Integer.toString(cid);
	}
}