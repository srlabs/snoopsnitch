package de.srlabs.msd.analysis;

import java.util.Vector;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.srlabs.msd.upload.DumpFile;


public class SMS implements AnalysisEvent{
	private long timestamp;
	private long id;
	private int mcc;
	private int mnc;
	private int lac;
	private int cid;
	private double longitude;
	private double latitude;
	private String sender;
	private String smsc;
	public enum Type {
		SILENT_SMS,
		BINARY_SMS,
		INVALID_SMS
		// TODO: Define the required types here
	}
	private Type type;
	public SMS() {
	}
	public SMS(long timestamp, long id, int mcc, int mnc, int lac, int cid,
			double latitude, double longitude, String sender, String smsc, Type type) {
		super();
		this.timestamp = timestamp;
		this.id = id;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.longitude = longitude;
		this.latitude = latitude;
		this.sender = sender;
		this.smsc = smsc;
		this.type = type;
	}

	/**
	 * Timestamp when the silent SMS was received, in millis since 1970
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * id column of the silent sms in table session_info, can be used to retrieve the SMS again using get(long id)
	 * @return
	 */
	public long getId() {
		return id;
	}
	/**
	 * MCC when the SMS was received
	 * @return
	 */
	public int getMcc() {
		return mcc;
	}
	/**
	 * MNC when the SMS was received
	 * @return
	 */
	public int getMnc() {
		return mnc;
	}
	public String getSender() {
		return sender;
	}
	/**
	 * Gets the type of this silent/binary SMS
	 * @return
	 */
	public Type getType() {
		return type;
	}
	public int getLac() {
		return lac;
	}
	public int getCid() {
		return cid;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("SilentSms: ID=" + id + " TYPE=" + type.name());
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
		return DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, timestamp, null, 0);
	}
	public void markForUpload(SQLiteDatabase db){
		Log.e("msd","markForUpload()");
		for(DumpFile file:getFiles(db)){
			Log.e("msd","markForUpload(): Doing file " + file);
			file.markForUpload(db);
		}
	}

	// The SMSC the SMS was sent from
	public String getSmsc() {
		return smsc;
	}
	public double getLongitude() {
		return longitude;
	}
	public double getLatitude() {
		return latitude;
	}
}
