package de.srlabs.snoopsnitch.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;


/**
 * This class contains a set of static methods for accessing the App configuration.
 */
public class MsdConfig {

    // The version suffix should be counted up when there is a solution for the
    // "No baseband messages" problem (so that phones detected to be
    // incompatible with a previous version can used again)
    private static final String DEVICE_INCOMPATIBLE_DETECTED_FLAG = "device_incompatible_detected_2";

    private static SharedPreferences sharedPrefs(Context context) {
        return context.getSharedPreferences("de.srlabs.snoopsnitch_preferences", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    public static int getBasebandLogKeepDurationHours(Context context) {
        return 24 * Integer.parseInt(sharedPrefs(context).getString("settings_basebandLogKeepDuration", "1"));
    }

    public static int getDebugLogKeepDurationHours(Context context) {
        return 24 * Integer.parseInt(sharedPrefs(context).getString("settings_debugLogKeepDuration", "1"));
    }

    public static int getMetadataKeepDurationHours(Context context) {
        return 24 * Integer.parseInt(sharedPrefs(context).getString("settings_basebandMetadataKeepDuration", "1"));
    }

    public static int getLocationLogKeepDurationHours(Context context) {
        return 24 * Integer.parseInt(sharedPrefs(context).getString("settings_locationLogKeepDuration", "1"));
    }

    public static int getAnalysisInfoKeepDurationHours(Context context) {
        return 24 * Integer.parseInt(sharedPrefs(context).getString("settings_analysisInfoKeepDuration", "30"));
    }

    public static boolean gpsRecordingEnabled(Context context) {
        return sharedPrefs(context).getBoolean("settings_gpsRecording", false);
    }

    public static boolean networkLocationRecordingEnabled(Context context) {
        return sharedPrefs(context).getBoolean("settings_networkLocationRecording", true);
    }

    public static boolean recordUnencryptedLogfiles(Context context) {
        return sharedPrefs(context).getBoolean("settings_recordUnencryptedLogfiles", false);
    }

    public static boolean recordUnencryptedDumpfiles(Context context) {
        return sharedPrefs(context).getBoolean("settings_recordUnencryptedDumpfiles", false);
    }


    public static boolean dumpUnencryptedEvents(Context context) {
        return sharedPrefs(context).getBoolean("settings_dumpUnencryptedEvents", false);
    }

    public static String getAppId(Context context) {
        return sharedPrefs(context).getString("settings_appId", "");
    }

    public static void setAppId(Context context, String appID) {
        Editor editor = sharedPrefs(context).edit();
        editor.putString("settings_appId", appID);
        editor.commit();
    }

    public static boolean getActiveTestForceOffline(Context context) {
        return sharedPrefs(context).getBoolean("settings_active_test_force_offline", false);
    }

    public static boolean getDeviceIncompatible(Context context) {
        return sharedPrefs(context).getBoolean(DEVICE_INCOMPATIBLE_DETECTED_FLAG, false);
    }

    public static void setDeviceIncompatible(Context context, boolean incompatible) {
        Editor editor = sharedPrefs(context).edit();
        editor.putBoolean(DEVICE_INCOMPATIBLE_DETECTED_FLAG, incompatible);
        editor.commit();
    }

    public static String getOwnNumber(Context context) {
        return sharedPrefs(context).getString("own_number", "");
    }

    public static void setOwnNumber(Context context, String ownNumber) {
        sharedPrefs(context).edit().putString("own_number", ownNumber).commit();
    }

    public static boolean getActiveTestSMSMODisabled(Context context) {
        return sharedPrefs(context).getBoolean("settings_active_test_sms_mo_disabled", false);
    }

    public static String getActiveTestSMSMONumber(Context context) {
        return sharedPrefs(context).getString("settings_active_test_sms_mo_number", "*4*");
    }

    public static long getDataJSLastCheckTime(Context context) {
        return sharedPrefs(context).getLong("data_js_last_check_time", 0);
    }

    public static void setDataJSLastCheckTime(Context context, Long timeInMillis) {
        Editor editor = sharedPrefs(context).edit();
        editor.putLong("data_js_last_check_time", timeInMillis);
        editor.commit();
    }

    public static String getDataJSLastModifiedHeader(Context context) {
        return sharedPrefs(context).getString("data_js_last_modified_header", null);
    }

    public static void setDataJSLastModifiedHeader(Context context, String header) {
        Editor editor = sharedPrefs(context).edit();
        editor.putString("data_js_last_modified_header", header);
        editor.commit();
    }

    public static int getActiveTestNumIterations(Context context) {
        return Integer.parseInt(sharedPrefs(context).getString("settings_active_test_num_iterations", "3"));
    }

    public static boolean getParserLogging(Context context) {
        return sharedPrefs(context).getBoolean("settings_parser_logging", false);
    }

    public static boolean getDeviceCompatibleDetected(Context context) {
        return sharedPrefs(context).getBoolean("device_compatible_detected", false);
    }

    public static void setDeviceCompatibleDetected(Context context, boolean compatible) {
        Editor editor = sharedPrefs(context).edit();
        editor.putBoolean("device_compatible_detected", compatible);
        editor.commit();
    }

    public static boolean getDumpAnalysisStackTraces(Context context) {
        return sharedPrefs(context).getBoolean("settings_debugging_dump_analysis_stacktraces", false);
    }

    public static boolean getActiveTestDisableUpload(Context context) {
        return sharedPrefs(context).getBoolean("settings_active_test_disable_upload", false);
    }

    public static boolean getCrash(Context context) {
        return sharedPrefs(context).getBoolean("settings_crash", false);
    }

    public static void setCrash(Context context, boolean crash) {
        Editor edit = sharedPrefs(context).edit();
        edit.putBoolean("settings_crash", crash);
        edit.commit();
    }

    public static long getLastCleanupTime(Context context) {
        return sharedPrefs(context).getLong("last_cleanup_time", 0);
    }

    public static void setLastCleanupTime(SharedPreferences pref, long time) {
        Editor edit = pref.edit();
        edit.putLong("last_cleanup_time", time);
        edit.commit();
    }

    public static void setLastCleanupTime(Context context, long time) {
        setLastCleanupTime(sharedPrefs(context), time);
    }

    public static boolean getFirstRun(Context context) {
        return sharedPrefs(context).getBoolean("app_first_run", true);
    }

    public static void setFirstRun(Context context, boolean firstRun) {
        Editor edit = sharedPrefs(context).edit();
        edit.putBoolean("app_first_run", firstRun);
        edit.commit();
    }

    public static boolean getStartOnBoot(Context context) {

        return sharedPrefs(context).getBoolean("settings_start_on_boot", true);
    }

    public static void setStartOnBoot(Context context, boolean startOnBoot) {
        Editor edit = sharedPrefs(context).edit();
        edit.putBoolean("settings_start_on_boot", startOnBoot);
        edit.commit();
    }

    public static boolean getAutoUploadMode(Context context) {
        return sharedPrefs(context).getBoolean("settings_auto_upload_mode", false);
    }

    public static boolean getUploadDailyPing(Context context) {
        return sharedPrefs(context).getBoolean("settings_upload_daily_ping", false);
    }

    public static boolean getPcapRecordingEnabled(Context context) {
        return sharedPrefs(context).getBoolean("settings_enable_pcap_recording", false);
    }

    public static String getPcapFilenamePrefix(Context context) {
        return sharedPrefs(context).getString("settings_pcap_filename_prefix", Environment.getExternalStorageDirectory().getPath() + "/snoopsnitch");
    }

    public static String getIMSICatcherNotificationSetting(Context context) {
        return sharedPrefs(context).getString("settings_imsi_catcher_event", "vibrate+ring");
    }

    public static String getSMSandSS7NotificationSetting(Context context) {
        return sharedPrefs(context).getString("settings_sms_event", "vibrate+ring");
    }

    public static String getLastFirmwareInformation(Context context) {
        return sharedPrefs(context).getString("firmware_info", null);
    }

    public static void setLastFirmwareInformation(Context context, String firmwareInfo) {
        Editor edit = sharedPrefs(context).edit();
        edit.putString("firmware_info", firmwareInfo);
        edit.commit();
    }
}
