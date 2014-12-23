package de.srlabs.snoopsnitch.util;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * This class contains a set of static methods for accessing the App configuration.
 * 
 */
public class MsdConfig {
	public static int getBasebandLogKeepDurationHours(Context context){
		return 24*Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("settings_basebandLogKeepDuration", "1"));
	}
	public static int getDebugLogKeepDurationHours(Context context){
		return 24*Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("settings_debugLogKeepDuration", "1"));
	}
	public static int getLocationLogKeepDurationHours(Context context){
		return 24*Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("settings_locationLogKeepDuration", "1"));
	}
	public static int getAnalysisInfoKeepDurationHours(Context context){
		return 24*Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("settings_analysisInfoKeepDuration", "30"));
	}
	public static boolean gpsRecordingEnabled(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_gpsRecording", false);
	}
	public static boolean networkLocationRecordingEnabled(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_networkLocationRecording", true);
	}
	public static boolean recordUnencryptedLogfiles(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_recordUnencryptedLogfiles", false);
	}
	public static boolean recordUnencryptedDumpfiles(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_recordUnencryptedDumpfiles", false);
	}
	public static String getAppId(Context context){
		return context.getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS).getString("settings_appId", "");
	}
}
