package de.srlabs.snoopsnitch.analysis;

import java.util.Vector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.upload.DumpFile;
import de.srlabs.snoopsnitch.upload.FileState;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;


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
	private boolean valid;
	private double score;
	private double a1;
	private double a2;
	private double a4;
	private double a5;
	private double k1;
	private double k2;
	private double c1;
	private double c2;
	private double c3;
	private double c4;
	private double c5;
	private double t1;
	private double t3;
	private double t4;
	private double r1;
	private double r2;
	private double f1;
	SQLiteDatabase db;

	public ImsiCatcher(long startTime, long endTime, long id, int mcc,
			int mnc, int lac, int cid, double latitude, double longitude, boolean valid, double score,
			double a1, double a2, double a4, double a5, double k1, double k2, double c1, double c2,
			double c3, double c4, double c5, double t1, double t3, double t4, double r1, double r2,
			double f1, Context context) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.id = id;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.valid = valid;
		this.latitude = latitude;
		this.longitude = longitude;
		this.score = score;
		this.a1 = a1;
		this.a2 = a2;
		this.a4 = a4;
		this.a5 = a5;
		this.k1 = k1;
		this.k2 = k2;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		this.c4 = c4;
		this.c5 = c5;
		this.t1 = t1;
		this.t3 = t3;
		this.t4 = t4;
		this.r1 = r1;
		this.r2 = r2;
		this.f1 = f1;
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
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

	public boolean isValid() {
		return valid;
	}

	/**
	 * Score for the IMSI catcher
	 * @return
	 */
	public double getScore() {
		return score;
	}

	public double getA1() {
		return a1;
	}

	public double getA2() {
		return a2;
	}

	public double getA4() {
		return a4;
	}

	public double getA5() {
		return a5;
	}

	public double getK1() {
		return k1;
	}

	public double getK2() {
		return k2;
	}

	public double getC1() {
		return c1;
	}

	public double getC2() {
		return c2;
	}

	public double getC3() {
		return c3;
	}

	public double getC4() {
		return c4;
	}

	public double getC5() {
		return c5;
	}

	public double getT1() {
		return t1;
	}

	public double getT3() {
		return t3;
	}

	public double getT4() {
		return t4;
	}

	public double getR1() {
		return r1;
	}

	public double getR2() {
		return r2;
	}

	public double getF1() {
		return f1;
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

	public String getLocation() {
		if (valid){
			return Double.toString(latitude) + " | " + Double.toString(longitude);
		} else {
			return "-";
		}
	}
}