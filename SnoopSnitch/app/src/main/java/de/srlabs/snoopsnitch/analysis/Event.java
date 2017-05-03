package de.srlabs.snoopsnitch.analysis;

import java.util.Vector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.upload.DumpFile;
import de.srlabs.snoopsnitch.upload.FileState;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;


public class Event implements AnalysisEvent{
	private long timestamp;
	private long id;
	private int mcc;
	private int mnc;
	private int lac;
	private int cid;
	private double longitude;
	private double latitude;
	private boolean valid;
	private String sender;
	private String smsc;
	public enum Type {
		SILENT_SMS,
		BINARY_SMS,
		NULL_PAGING,
		INVALID_EVENT
	}
	private Type type;
	SQLiteDatabase db;

	public Event(long timestamp, long id, int mcc, int mnc, int lac, int cid,
			double latitude, double longitude, boolean valid, String sender, String smsc, Type type, Context context) {
		super();
		this.timestamp = timestamp;
		this.id = id;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.longitude = longitude;
		this.latitude = latitude;
		this.valid = valid;
		this.sender = sender;
		this.smsc = smsc;
		this.type = type;
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		db = MsdDatabaseManager.getInstance().openDatabase();
	}

	/**
	 * Mark raw data related to this event for upload
	 */
	public void upload() {
		DumpFile.markForUpload(db, DumpFile.TYPE_ENCRYPTED_QDMON, timestamp, null, 0);
	}

	/**
	 * Return upload state of Event object
	 * @return
	 */
	@Override
	public FileState getUploadState() {
		return DumpFile.getState(db, DumpFile.TYPE_ENCRYPTED_QDMON, timestamp, null, 0);
	}

	/**
	 * Timestamp when the event was received, in millis since 1970
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * id column of the event in table session_info, can be used to retrieve the event again using get(long id)
	 * @return
	 */
	public long getId() {
		return id;
	}
	/**
	 * MCC when the event was received
	 * @return
	 */
	public int getMcc() {
		return mcc;
	}
	/**
	 * MNC when the event was received
	 * @return
	 */
	public int getMnc() {
		return mnc;
	}
	public String getSender() {
		return sender;
	}
	/**
	 * Gets the type of this event
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
		StringBuffer result = new StringBuffer("Event: ID=" + id + " TYPE=" + type.name());
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
		return DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, timestamp, null, 0);
	}
	@Override
	public void markForUpload(SQLiteDatabase db){
		for(DumpFile file:getFiles(db)){
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

	public boolean isValid() {
		return valid;
	}

	public String getFullCellID() {
		return Integer.toString(mcc) +
				"/" + Integer.toString(mnc) +
				"/" + Integer.toString(lac) +
				"/" + Integer.toString(cid);
	}

	public String getLocation() {
		if (valid){
			return Double.toString(latitude) + " | " + Double.toString(longitude);
		} else {
			return "-";
		}
	}
}
