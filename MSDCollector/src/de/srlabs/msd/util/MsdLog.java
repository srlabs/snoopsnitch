package de.srlabs.msd.util;

import java.util.Calendar;

import android.os.Build;
import android.util.Log;
import de.srlabs.msd.qdmon.MsdService;
import de.srlabs.msd.qdmon.MsdServiceHelper;

public class MsdLog {
	private static MsdServiceHelper msdServiceHelper;
	private static MsdService msd;
	private static String getTimePrefix(){
		Calendar c = Calendar.getInstance();
		String result = "" + c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) < 10 ? "0" : "") + c.get(Calendar.MONTH) + "-" + ( c.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "" ) + c.get(Calendar.DAY_OF_MONTH);
		// TODO: Add millis
		result += " " + (c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" : "") + c.get(Calendar.HOUR_OF_DAY) + ":" + (c.get(Calendar.MINUTE) < 10 ? "0" : "") + c.get(Calendar.MINUTE) + ":" + (c.get(Calendar.SECOND) < 10 ? "0" : "") + c.get(Calendar.SECOND) + " ";
		return result;
	}
	public static void i(String tag, String msg) {
		Log.i(tag,msg);
		printlnToLog(getTimePrefix() + tag + ": INFO: " + msg);
	}
	public static void e(String tag, String msg) {
		Log.e(tag,msg);
		printlnToLog(getTimePrefix() + tag + ": ERROR: " + msg);
	}
	public static void v(String tag, String msg) {
		Log.v(tag,msg);
		printlnToLog(getTimePrefix() + tag + ": VERBOSE: " + msg);
	}
	public static void d(String tag, String msg) {
		Log.d(tag,msg);
		printlnToLog(getTimePrefix() + tag + ": DEBUG: " + msg);
	}
	public static void d(String tag, String msg, Throwable tr) {
		Log.d(tag,msg,tr);
		printlnToLog(getTimePrefix() + tag + ": DEBUG: " + msg + "\n" + Log.getStackTraceString(tr));
	}
	public static void e(String tag, String msg, Throwable tr) {
		Log.e(tag,msg,tr);
		printlnToLog(getTimePrefix() + tag + ": ERROR: " + msg + "\n" + Log.getStackTraceString(tr));
	}
	public static void w(String tag, String msg, Throwable tr) {
		Log.w(tag,msg,tr);
		printlnToLog(getTimePrefix() + tag + ": WARNING: " + msg + "\n" + Log.getStackTraceString(tr));
	}
	public static void w(String tag, String msg) {
		Log.w(tag,msg);
		printlnToLog(getTimePrefix() + tag + ": WARNING: " + msg);
	}
	public static void init(MsdServiceHelper msdServiceHelper) {
		MsdLog.msdServiceHelper = msdServiceHelper;
	}
	public static void init(MsdService msd) {
		MsdLog.msd = msd;
	}
	private static void printlnToLog(String line){
		if(msdServiceHelper != null){
			msdServiceHelper.writeLog(line + "\n");
		} else if(msd != null){
			msd.writeLog(line + "\n");
		} else{
			throw new IllegalStateException("Please use MsdLog.init(context) before logging anything");
		}
	}
	/**
	 * Gets some information about phone model, Android version etc.
	 */
	public static String getLogStartInfo() {
		StringBuffer result = new StringBuffer();
		result.append("Log opened " + Utils.formatTimestamp(System.currentTimeMillis()) + "\n");
		result.append("Running on model " + Build.MODEL + " Android version " + Build.VERSION.RELEASE + "\n");
		return result.toString();
	}
}
