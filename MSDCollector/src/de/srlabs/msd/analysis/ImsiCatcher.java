package de.srlabs.msd.analysis;

import java.util.Vector;

import android.database.sqlite.SQLiteDatabase;
import de.srlabs.msd.upload.DumpFile;


public class ImsiCatcher implements AnalysisEvent{
	private long startTime;
	private long endTime;
	private long id;
	private int mcc;
	private int mnc;
	private int lac;
	private int cid;
	private double confidence;

	public ImsiCatcher() {
	}
	
	public ImsiCatcher(long startTime, long id, int mcc, int mnc,
			int lac, int cid, double confidence) {
		super();
		this.startTime = startTime;
		this.endTime = startTime + 3000;
		this.id = id;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.confidence = confidence;
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
	/**
	 * Confidence level of this detected IMSI catcher, from 0 to 1
	 * @return
	 */
	public double getConfidence() {
		return confidence;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("ImsiCatcher: ID=" + id);
		// TODO: Add more fields
		return result.toString();
	}
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
	public Vector<DumpFile> getFiles(SQLiteDatabase db){
		return DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, startTime, endTime, 0);
	}
	public void markForUpload(SQLiteDatabase db){
		for(DumpFile file:getFiles(db)){
			file.markForUpload(db);
		}
	}
}
