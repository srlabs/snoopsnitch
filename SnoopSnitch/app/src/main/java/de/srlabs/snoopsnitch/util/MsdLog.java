package de.srlabs.snoopsnitch.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import de.srlabs.snoopsnitch.EncryptedFileWriterError;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdService;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;

public class MsdLog {
	private static MsdServiceHelper msdServiceHelper;
	private static MsdService msd;

	public static String getTime(){
		Calendar c = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZZ", Locale.US);
		return format.format(c.getTime());
	}
	
	private static String getTimePrefix(){
		return getTime() + " ";
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
			try {
				msd.writeLog(line + "\n");
			} catch (EncryptedFileWriterError e) {
				throw new IllegalStateException("Error writing to log file: " + e.getMessage());
			}
		} else{
			throw new IllegalStateException("Please use MsdLog.init(context) before logging anything");
		}
	}
	/**
	 * Gets some information about phone model, Android version etc.
	 */
	public static String getLogStartInfo(Context context) {
		StringBuffer result = new StringBuffer();
		result.append("Log opened " + Utils.formatTimestamp(System.currentTimeMillis()) + "\n");
		result.append("SnoopSnitch Version: " + context.getString(R.string.app_version) + "\n");
		result.append("Android version: " + Build.VERSION.RELEASE + "\n");
		result.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		result.append("Board: "        + Build.BOARD + "\n");
		result.append("Brand: "        + Build.BRAND + "\n");
		result.append("Product: "      + Build.PRODUCT + "\n");
		result.append("Model: "        + Build.MODEL + "\n");
		result.append("Baseband: "     + Build.getRadioVersion() + "\n");
		return result.toString();
	}
}
