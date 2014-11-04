package de.srlabs.msd.analysis;

import java.util.Vector;

import android.content.Context;

public class ImsiCatcher {
	private long startTime;
	private long endTime;
	private long id;
	private int mcc;
	private int mnc;
	private int lac;
	private int cid;
	private double confidence;

	public static Vector<ImsiCatcher> getImsiCatchers(Context context, long startMillis, long endMillis){
		Vector<ImsiCatcher> result = new Vector<ImsiCatcher>();
		// TODO: Read database
		return result;
	}
	/**
	 * Get an IMSI Catcher based on an ID
	 * @param id
	 * @return
	 */
	public static ImsiCatcher get(long id){
		// TODO: Read database
		return null;
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
		StringBuffer result = new StringBuffer("SilentSms: ID=" + id);
		// TODO: Add more fields
		return result.toString();
	}
}
