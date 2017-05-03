package de.srlabs.snoopsnitch.upload;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.Utils;

/**
 * This class provides some manual object relational mapping to entries of the database table files.
 *
 */
public class DumpFile {
	private long id = -1;
	private String filename;
	private long start_time;
	private long end_time;
	private int file_type;
	public static final int TYPE_DEBUG_LOG = 1;
	public static final int TYPE_ENCRYPTED_QDMON = 2;
	public static final int TYPE_METADATA = 3;
	public static final int TYPE_LOCATION_INFO = 4;
	public static final int TYPE_BUG_REPORT = 5;
	private boolean sms = false;
	private boolean imsi_catcher = false;
	private boolean crash = false;
	private int state;
	public static final int STATE_RECORDING = 1;
	public static final int STATE_AVAILABLE = 2;
	public static final int STATE_PENDING = 3;
	public static final int STATE_UPLOADED = 4;
	public static final int STATE_DELETED = 5;
	public static final int STATE_RECORDING_PENDING = 6;
	
	public DumpFile(String filename, int type) {
		this(filename,type,System.currentTimeMillis(),0);
	}
	public DumpFile(String filename, int type, long startTime, long endTime) {
		if(endTime == 0)
			endTime = startTime;
		this.filename = filename;
		this.file_type = type;
		this.state = STATE_RECORDING;
		this.start_time = startTime;
		this.end_time = endTime;
	}

	public DumpFile(Cursor c) {
		id = c.getLong(c.getColumnIndex("_id"));
		filename = c.getString(c.getColumnIndex("filename"));
		start_time = Timestamp.valueOf(c.getString(c.getColumnIndex("start_time"))).getTime();
		end_time = Timestamp.valueOf(c.getString(c.getColumnIndex("end_time"))).getTime();
		file_type = c.getInt(c.getColumnIndex("file_type"));
		sms = c.getInt(c.getColumnIndex("sms")) != 0;
		imsi_catcher = c.getInt(c.getColumnIndex("imsi_catcher")) != 0;
		crash = c.getInt(c.getColumnIndex("crash")) != 0;
		state = c.getInt(c.getColumnIndex("state"));
	}

	public static DumpFile get(SQLiteDatabase db, long id){
		Vector<DumpFile> files = getFiles(db, "_id = " + id);
		if(files.size() == 0)
			return null;
		return files.firstElement();
	}
	/**
	 * Gets all files between time1 and time2 from the database. time2 can be null to get only files containing time1.
	 * @param db An SQLiteDatabase object to get the files from
	 * @param type Optional, get files of a specific type only
	 * @param time1 Start time
	 * @param time2 End time
	 * @param rangeSeconds Extend the range from time1 to time2 by rangeSeconds seconds to get the surrounding dumps as well
	 * @return
	 */
	public static Vector<DumpFile> getFiles(SQLiteDatabase db, Integer type, long time1, Long time2, Integer rangeSeconds){
		if(time2 == null)
			time2 = time1;
		if(time2 < time1){
			long tmp = time1;
			time1 = time2;
			time2 = tmp;
		}
		if(rangeSeconds != null){
			time1 -= rangeSeconds * 1000L;
			time2 += rangeSeconds * 1000L;
		}
		String selection = "end_time >= '" + (new Timestamp(time1)).toString() + "' AND start_time <= '" + (new Timestamp(time2)).toString() + "'";
		if(type != null)
			selection += " AND file_type = " + type;
		return getFiles(db, selection);
	}
	public static Vector<DumpFile> getFiles(SQLiteDatabase db, String selection){
		Vector<DumpFile> result = new Vector<DumpFile>();
		Cursor c = db.query("files", null, selection, null, null, null, "_id");
		while(c.moveToNext()){
			DumpFile entry = new DumpFile(c);
			result.add(entry);
		}
		c.close();
		return result;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("DumpFile ID=" + id + "  filename=" + filename);
		result.append("  start=" + Utils.formatTimestamp(start_time) + "  end=" + Utils.formatTimestamp(end_time));
		result.append("  file_type=" + typeToString(file_type));
		result.append("  state=" + stateToString(state));
		result.append("  sms=" + sms + "  imsi_catcher=" + imsi_catcher);
		return result.toString();
	}
	public static String typeToString(int type){
		if(type == TYPE_DEBUG_LOG)
			return "Debug log";
		else if(type == TYPE_ENCRYPTED_QDMON)
			return "Encrypted qdmon dump";
		else if(type == TYPE_METADATA)
			return "Metadata";
		else if(type == TYPE_LOCATION_INFO)
			return "Location info";
		else if(type == TYPE_BUG_REPORT)
			return "Bug report";
		else
			return "Invalid Dumpfile type " + type;
	}
	public static String stateToString(int state){
		if(state == STATE_RECORDING)
			return "Recording";
		else if(state == STATE_AVAILABLE)
			return "Available";
		else if(state == STATE_PENDING)
			return "Pending for upload";
		else if(state == STATE_UPLOADED)
			return "Uploaded";
		else if(state == STATE_DELETED)
			return "Deleted";
		else if(state == STATE_RECORDING_PENDING)
			return "Recording and pending for upload";
		else
			return "Invalid state " + state;
	}
	public void recordingStopped(){
		if(state == STATE_RECORDING){
			state = STATE_AVAILABLE;
		} else if(state == STATE_RECORDING_PENDING){
			state = STATE_PENDING;
		} else{
			throw new IllegalStateException("recordingSopped can only be called in STATE_RECORDING and STATE_RECORDING_PENDING. Current State: " + stateToString(state) + " (" + state + ")");
		}
		end_time = System.currentTimeMillis();
	}
	/**
	 * Inserts this object to the database
	 * @param db
	 */
	public void insert(SQLiteDatabase db){
		if(id != -1)
			throw new IllegalStateException("Dumpfile " + filename + " already exists in database, please use update() instead of insert()");
		ContentValues values = this.makeContentValues();
		id = db.insertOrThrow("files",null,values);
		if( id == -1)
			throw new IllegalStateException("Failed to insert file " + filename + " into database");
	}
	private ContentValues makeContentValues() {
		ContentValues result = new ContentValues();
		result.put("filename",filename);
		result.put("start_time", (new Timestamp(start_time)).toString());
		result.put("end_time", (new Timestamp(end_time)).toString());
		result.put("file_type",file_type);
		result.put("sms",sms ? 1:0);
		result.put("imsi_catcher",imsi_catcher ? 1:0);
		result.put("crash",crash ? 1:0);
		result.put("state", state);
		return result;
	}
//	TODO
//	/**
//	 * Updates an existing object (identified by the id field) in the database
//	 * @param db
//	 */
//	public void update(SQLiteDatabase db){
//		ContentValues values = this.makeContentValues();
//		int numRows = db.update("files", values, "_id = " + id, null);
//		if(numRows != 1)
//			throw new IllegalStateException("Update statement failed, id " + id + " in table files not found");
//	}
	public long getEnd_time() {
		return end_time;
	}

	public void setEnd_time(long end_time) {
		this.end_time = end_time;
	}

	public boolean isSms() {
		return sms;
	}

	public void setSms(boolean sms) {
		this.sms = sms;
	}

	public boolean isImsi_catcher() {
		return imsi_catcher;
	}

	public boolean isCrash(){
		return crash;
	}
	public void setImsi_catcher(boolean imsi_catcher) {
		this.imsi_catcher = imsi_catcher;
	}

	public long getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public long getStart_time() {
		return start_time;
	}

	public int getFile_type() {
		return file_type;
	}

	public int getState() {
		return state;
	}

	/**
	 * Gets combined upload state of all files between time1 and time2 from the database. time2 can be null to get only files containing time1.
	 * @param db An SQLiteDatabase object to get the files from
	 * @param type Optional, get files of a specific type only
	 * @param time1 Start time
	 * @param time2 End time
	 * @param rangeSeconds Extend the range from time1 to time2 by rangeSeconds seconds to get the surrounding dumps as well
	 * @return
	 */
	public static FileState getState(SQLiteDatabase db, Integer type, long time1, Long time2, Integer rangeSeconds){

		int recording = 0, available = 0, pending = 0, uploaded = 0, deleted = 0, recording_pending = 0, invalid = 0, total = 0;

		for (DumpFile file:getFiles(db, type, time1, time2, rangeSeconds)) {
			switch (file.getState()) {
				case STATE_RECORDING:   	  recording++; break;
				case STATE_AVAILABLE:   	  available++; break;
				case STATE_PENDING:     	  pending++; break;
				case STATE_UPLOADED:    	  uploaded++; break;
				case STATE_DELETED:     	  deleted++; break;
				case STATE_RECORDING_PENDING: recording_pending++; break;
				default: invalid++; break;
			}
			total++;
		}

		//  No results in that time frame
		if (total == 0) {
			return FileState.STATE_INVALID;
		}

		//  Should not happen if above switch statement is complete
		if (invalid > 0) {
			return FileState.STATE_INVALID;
		}

		//  This is how the result is calculated:
		//
		//  1. STATE_DELETED:   All files have state DELETED
		//  2. STATE_AVAILABLE: At least one file has state RECORDING or AVAILABLE
		//  3. STATE_UPLOADED:  No file has state RECORDING or AVAILABLE, at least one file has state PENDING, UPLOADED or RECORDING_PENDING

		//  Case 1.
		if (total == deleted) {
			return FileState.STATE_DELETED;
		}

		//  Case 2.
		if (recording > 0 || available > 0) {
			return FileState.STATE_AVAILABLE;
		}

		//  Case 3.
		if (pending > 0 || uploaded > 0 || recording_pending > 0) {
			return FileState.STATE_UPLOADED;
		}

		//  Should not happen
		return FileState.STATE_INVALID;
	}
	public void endRecording(SQLiteDatabase db, Context ctx){
		endRecording(db, ctx, null);
	}
	public void endRecording(SQLiteDatabase db, Context ctx, Long maxDurationMillis){
		this.end_time = System.currentTimeMillis();
		long duration = this.end_time - this.start_time;
		if(duration < 0 || (maxDurationMillis != null && duration > maxDurationMillis)){
			// We need a maximum duration since the system time may be changed
			// while SnoopSnitch is recording. Without a limit, this can result
			// in files with a period of several days (according to start_time
			// and end_time), which then confuses the detection whether an Event
			// is already uploaded or not.
			MsdLog.w("DumpFile", "Discarding dumpfile " + filename + " because the duration " + duration + " is negative or larger than the specified maximum of " + maxDurationMillis + " millis");
			ctx.deleteFile(filename);
			db.delete("files", "_id=" + this.id,  null);
			return;
		}
		ContentValues values = new ContentValues();
		values.put("end_time", (new Timestamp(end_time)).toString());
		if(state == STATE_RECORDING){
			if(updateState(db, STATE_RECORDING, STATE_AVAILABLE, values))
				return;
		}
		if(updateState(db, STATE_RECORDING_PENDING, STATE_PENDING, values))
			return;
		throw new IllegalStateException("Can't change state of file " + getFilename() + " id=" + getId());
	}
	public void markForUpload(SQLiteDatabase db) {
		Log.i("DumpFile", "markForUpload: " + this);
		if(state == STATE_AVAILABLE){
			if(updateState(db, STATE_AVAILABLE, STATE_PENDING, null))
				return;
		} else if(state == STATE_RECORDING){
			if(updateState(db, STATE_RECORDING, STATE_RECORDING_PENDING, null))
				return;
			// Fallback in case the database row has been changed from RECORDING
			// to AVAILABLE by MsdService since it has been read from the
			// database.
			if(updateState(db, STATE_AVAILABLE, STATE_PENDING, null))
				return;
		}
		Log.e("DumpFile", "markForUpload failed: " + this);
	}

	/**
	 * Marks all files between time1 and time2 for upload. time2 can be null to get only files containing time1.
	 * @param db An SQLiteDatabase object to get the files from
	 * @param type Optional, get files of a specific type only
	 * @param time1 Start time
	 * @param time2 End time
	 * @param rangeSeconds Extend the range from time1 to time2 by rangeSeconds seconds to get the surrounding dumps as well
	 * @return
	 */
	public static void markForUpload(SQLiteDatabase db, Integer type, long time1, Long time2, Integer rangeSeconds){
		for (DumpFile file:getFiles(db, type, time1, time2, rangeSeconds)) {
			if (file.getState() == STATE_AVAILABLE || file.getState() == STATE_RECORDING) {
				file.markForUpload(db);
			}
		}
	}

	/**
	 * Updates the state field from oldState to newState
	 * @param db
	 * @param oldState
	 * @param newState
	 * @param values Some extra values to change in the database
	 * @return
	 */
	public boolean updateState(SQLiteDatabase db, int oldState, int newState, ContentValues values) {
		if(values == null)
			values = new ContentValues();
		values.put("state", newState);
		int numModified = db.update("files", values, "_id = " + id + " AND state = " + oldState, null);
		return numModified == 1;
	}

	public boolean updateSms(SQLiteDatabase db, boolean b) {
		ContentValues values = new ContentValues();
		values.put("sms", b?1:0);
		int numModified = db.update("files", values, "_id = " + id, null);
		return numModified == 1;
	}

	public boolean updateCrash(SQLiteDatabase db, boolean b) {
		ContentValues values = new ContentValues();
		values.put("crash", b?1:0);
		int numModified = db.update("files", values, "_id = " + id, null);
		return numModified == 1;
	}
	public boolean updateImsi(SQLiteDatabase db, boolean b) {
		ContentValues values = new ContentValues();
		values.put("imsi_catcher", b?1:0);
		int numModified = db.update("files", values, "_id = " + id, null);
		return numModified == 1;
	}
	public boolean delete(SQLiteDatabase db) {
		int numModified = db.delete("files", "_id = " + id, null);
		return numModified == 1;
	}
	
	public String getReportId(){
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss", Locale.getDefault());
		Date date = new Date(getStart_time());
		return dateFormat.format(date);
	}
	
}
