package de.srlabs.patchalyzer;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.util.Log;

import junit.framework.Test;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles interaction with SharedPreferences
 */

public class SharedPrefsHelper {
    public static final String SHAREDPREFS_FILE_CLEAR_ON_UPGRADE = "PATCHALYZER";
    public static final String SHAREDPREFS_FILE_KEEP_ON_UPGRADE = "PATCHALYZER_PERSISTENT";

    // To be stored in SHAREDPREFS_FILE_CLEAR_ON_UPGRADE
    public static final String KEY_STICKY_ERROR_MESSAGE = "KEY_STICKY_ERROR_MESSAGE";
    public static final String KEY_ANALYSIS_RESULT = "KEY_ANALYSIS_RESULT";

    public static final String KEY_BUILD_DATE = "KEY_BUILD_DATE";
    public static final String KEY_BUILD_FINGERPRINT = "KEY_BUILD_FINGERPRINT";
    public static final String KEY_BUILD_DISPLAY_NAME = "KEY_BUILD_DISPLAY_NAME";
    public static final String KEY_BUILD_APPVERSION = "KEY_BUILD_APPVERSION";

    // To be stored in SHAREDPREFS_FILE_KEEP_ON_UPGRADE
    public static final String KEY_BUILD_DATE_LAST_ANALYSIS = "KEY_BUILD_DATE_LAST_ANALYSIS";
    public static final String KEY_BUILD_DATE_NOTIFICATION_DISPLAYED = "KEY_BUILD_DATE_NOTIFICATION_DISPLAYED";
    public static final String KEY_TIMESTAMP_LAST_ANALYSIS = "KEY_TIMESTAMP_LAST_ANALYSIS";
    public static final String KEY_DID_SHOW_NEW_FEATURE = "KEY_DID_SHOW_NEW_FEATURE";

    private static JSONObject cachedResultJSON;
    private static String cachedStickyErrorMessage;


    // Shorthand methods

    public static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(SHAREDPREFS_FILE_CLEAR_ON_UPGRADE, 0);
    }

    public static SharedPreferences getPersistentSharedPrefs(Context context) {
        return context.getSharedPreferences(SHAREDPREFS_FILE_KEEP_ON_UPGRADE, 0);
    }

    public static SharedPreferences.Editor getSharedPrefsEditor(Context context) {
        return getSharedPrefs(context).edit();
    }

    public static SharedPreferences.Editor getPersistentSharedPrefsEditor(Context context) {
        return getPersistentSharedPrefs(context).edit();
    }

    public static boolean putString(String key, String value, Context context) {
        SharedPreferences.Editor editor = getSharedPrefsEditor(context);
        editor.putString(key, value);
        return editor.commit();
    }

    public static boolean putStringPersistent(String key, String value, Context context) {
        SharedPreferences.Editor editor = getPersistentSharedPrefsEditor(context);
        editor.putString(key, value);
        return editor.commit();
    }

    public static boolean putLong(String key, long value, Context context) {
        SharedPreferences.Editor editor = getSharedPrefsEditor(context);
        editor.putLong(key, value);
        return editor.commit();
    }

    public static boolean putLongPersistent(String key, long value, Context context) {
        SharedPreferences.Editor editor = getPersistentSharedPrefsEditor(context);
        editor.putLong(key, value);
        return editor.commit();
    }


    public static void resetSharedPrefs(Context contex){
        Log.i(Constants.LOG_TAG, "SharedPrefsHelper.resetSharedPrefs() called");
        SharedPreferences.Editor editor = getSharedPrefsEditor(contex);
        editor.clear();
        editor.putLong(KEY_BUILD_DATE, TestUtils.getBuildDateUtc());
        editor.putString(KEY_BUILD_FINGERPRINT, TestUtils.getBuildFingerprint());
        editor.putString(KEY_BUILD_DISPLAY_NAME, TestUtils.getBuildDisplayName());
        editor.putInt(KEY_BUILD_APPVERSION, Constants.APP_VERSION);
        editor.commit();
    }

    // Displayed in PatchalyzerMainactivity if no test result is available
    public static void saveStickyErrorMessage(String stickyErrorMessage, ContextWrapper context) {
        cachedStickyErrorMessage = stickyErrorMessage;

        Log.d(Constants.LOG_TAG,"Writing stickyErrorMessage to sharedPrefs");
        putString(KEY_STICKY_ERROR_MESSAGE, stickyErrorMessage, context);

    }

    public static void clearSavedStickyErrorMessage(ContextWrapper context) {
        cachedStickyErrorMessage = null;

        Log.d(Constants.LOG_TAG,"Deleting stickyErrorMessage from sharedPrefs");
        putString(KEY_STICKY_ERROR_MESSAGE, "", context);
    }

    public static String getStickyErrorMessage(ContextWrapper context) {
        if (cachedStickyErrorMessage != null) {
            return cachedStickyErrorMessage;
        }

        Log.d(Constants.LOG_TAG,"Reading stickyErrorMessage from sharedPrefs");
        SharedPreferences settings = getSharedPrefs(context);

        String stickyErrorMessage = settings.getString(KEY_STICKY_ERROR_MESSAGE, "");
        if (stickyErrorMessage.equals("")) {
            return null;
        }

        return stickyErrorMessage;
    }

    public static void clearSavedAnalysisResult(Context context) {
        cachedResultJSON = null;

        Log.d(Constants.LOG_TAG,"Deleting analysisResult from sharedPrefs");
        putString(KEY_ANALYSIS_RESULT, "", context);
    }

    public static JSONObject getAnalysisResult(ContextWrapper context) {
        if (cachedResultJSON != null) {
            return cachedResultJSON;
        }

        Log.d(Constants.LOG_TAG,"Reading analysisResult from sharedPrefs");
        SharedPreferences settings = getSharedPrefs(context);

        String analysisResultString = settings.getString(KEY_ANALYSIS_RESULT, "");
        if (analysisResultString.equals("")) {
            return null;
        }

        try {
            cachedResultJSON = new JSONObject(analysisResultString);
            return cachedResultJSON;
        } catch (JSONException e) {
            Log.d(Constants.LOG_TAG,"Could not parse JSON from SharedPrefs. Returning null");
            return null;
        }
    }

    /**
     * @return A stringified version of analysisResultJSON
     */
    public static String saveAnalysisResult(JSONObject analysisResultJSON, Context context) {
        long timeStamp = System.currentTimeMillis();
        long buildDateUtc = TestUtils.getBuildDateUtc();

        cachedResultJSON = analysisResultJSON;
        String analysisResultString = analysisResultJSON.toString();

        Log.d(Constants.LOG_TAG,"Writing analysisResult to sharedPrefs");
        putString(KEY_ANALYSIS_RESULT, analysisResultString, context);

        SharedPreferences.Editor persistentEditor = getPersistentSharedPrefsEditor(context);
        persistentEditor.putLong(KEY_BUILD_DATE_LAST_ANALYSIS, buildDateUtc);
        persistentEditor.putLong(KEY_TIMESTAMP_LAST_ANALYSIS, timeStamp);
        persistentEditor.commit();

        return analysisResultString;
    }

    // This is needed so that the cached value for the Main App process can be set while the
    // TestExecutorService modifies the saved value in SharedPrefs
    public static String saveAnalysisResultNonPersistent(JSONObject analysisResultJSON) {
        cachedResultJSON = analysisResultJSON;
        return analysisResultJSON.toString();
    }

}
