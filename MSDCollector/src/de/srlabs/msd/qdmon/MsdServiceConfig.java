package de.srlabs.msd.qdmon;

/**
 * This class contains a set of static methods, which are called by MsdService
 * to get the current configuration values from the App settings dialog.
 * 
 */
public class MsdServiceConfig {
	public static int getBasebandLogKeepDurationHours(){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static int getLocationLogKeepDurationHours(){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static int getSessionInfoKeepDurationHours(){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static int getCellInfoKeepDurationHours(){
		return 24;
		// TODO: Make this setting configurable by the user
	}
	public static boolean gpsRecordingEnabled(){
		return true;
		// TODO: Make this setting configurable by the user
	}
	public static boolean networkLocationRecordingEnabled(){
		return true;
		// TODO: Make this setting configurable by the user
	}
	public static boolean recordUnencryptedDumpfiles(){
		return true;
		// TODO: Make this a configuration setting with default false in the final app
	}
	public static boolean recordEncryptedDumpfiles(){
		return true;
		// TODO: Make this a configuration setting with default true in the final app
	}
}
