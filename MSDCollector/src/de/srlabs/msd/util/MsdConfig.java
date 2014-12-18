package de.srlabs.msd.util;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * This class contains a set of static methods for accessing the App configuration.
 * 
 */
public class MsdConfig {
	public static int getBasebandLogKeepDurationHours(Context context){
		
		return PreferenceManager.getDefaultSharedPreferences(context).getInt("settings_basebandLogKeepDuration", 30);
	}
	public static int getDebugLogKeepDurationHours(Context context){
		return 3; // TODO: Maybe add a configuration entry
	}
	public static int getLocationLogKeepDurationHours(Context context){
		
		return PreferenceManager.getDefaultSharedPreferences(context).getInt("settings_locationLogKeepDuration", 30);
	}
	public static int getSessionInfoKeepDurationHours(Context context){
		
		return PreferenceManager.getDefaultSharedPreferences(context).getInt("settings_sessionInfoKeepDuration", 30);
	}
	public static int getCellInfoKeepDurationHours(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getInt("settings_cellInfoKeepDuration", 30);
	}
	public static boolean gpsRecordingEnabled(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_gpsRecording", false);
	}
	public static boolean networkLocationRecordingEnabled(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_networkLocationRecording", true);
	}
	public static boolean recordUnencryptedDumpfiles(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_recordUnencryptedDumpfiles", false);
	}
	public static boolean recordEncryptedDumpfiles(Context context){

		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("settings_recordEncryptedDumpfiles", true);
	}
	public static String getAppId(Context context){
		return context.getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS).getString("settings_appId", "");
	}
}
