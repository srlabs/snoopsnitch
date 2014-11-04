package de.srlabs.msd.util;

import android.content.Context;

/**
 * This class contains a set of static methods for accessing the App configuration.
 * 
 */
public class MsdConfig {
	public static int getBasebandLogKeepDurationHours(Context context){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static int getLocationLogKeepDurationHours(Context context){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static int getSessionInfoKeepDurationHours(Context context){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static int getCellInfoKeepDurationHours(Context context){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static boolean gpsRecordingEnabled(Context context){
		return true;
		// TODO: Make this setting configurable by the user
	}
	public static boolean networkLocationRecordingEnabled(Context context){
		return true;
		// TODO: Make this setting configurable by the user
	}
	public static boolean recordUnencryptedDumpfiles(Context context){
		return true;
		// TODO: Make this a configuration setting with default false in the final app
	}
	public static boolean recordEncryptedDumpfiles(Context context){
		return true;
		// TODO: Make this a configuration setting with default true in the final app
	}
	public static String getAppId(Context context){
		// TODO: Get saved App ID from Android preferences
		// If there is no saved App ID, please generate one using Utils.generateAppId() and save it to the preferences.
		return "TODO_APP_ID_HERE";
	}
}
