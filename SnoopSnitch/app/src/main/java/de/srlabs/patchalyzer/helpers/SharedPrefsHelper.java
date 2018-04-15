package de.srlabs.patchalyzer.helpers;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.analysis.PatchalyzerService;
import de.srlabs.patchalyzer.analysis.TestUtils;
import de.srlabs.patchalyzer.util.CertifiedBuildChecker;

/**
 * Handles interaction with SharedPreferences
 */

public class SharedPrefsHelper {
    public static final String SHAREDPREFS_FILE_CLEAR_ON_UPGRADE = "PATCHALYZER";
    public static final String SHAREDPREFS_FILE_KEEP_ON_UPGRADE = "PATCHALYZER_PERSISTENT";

    // To be stored in SHAREDPREFS_FILE_CLEAR_ON_UPGRADE
    public static final String KEY_STICKY_ERROR_MESSAGE = "KEY_STICKY_ERROR_MESSAGE";
    public static final String KEY_ANALYSIS_RESULT = "KEY_ANALYSIS_RESULT";
    public static final String KEY_LAST_BUILD_CERTIFIED = "KEY_LAST_BUILD_CERTIFIED";

    public static final String KEY_BUILD_DATE = "KEY_BUILD_DATE";
    public static final String KEY_BUILD_FINGERPRINT = "KEY_BUILD_FINGERPRINT";
    public static final String KEY_BUILD_DISPLAY_NAME = "KEY_BUILD_DISPLAY_NAME";
    public static final String KEY_BUILD_APPVERSION = "KEY_BUILD_APPVERSION";

    //SafetyNet responses
    private static final String KEY_BUILD_CERTIFIED_CTSPROFILE_MATCH = "KEY_CERTIFIED_BUILD_CTSMATCHPROFILE_RESPONSE";
    private static final String KEY_BUILD_CERTIFIED_BASICINTEGRITY_MATCH = "KEY_CERTIFIED_BUILD_BASICINTEGRITY_RESPONSE";

    // To be stored in SHAREDPREFS_FILE_KEEP_ON_UPGRADE
    public static final String KEY_BUILD_DATE_LAST_ANALYSIS = "KEY_BUILD_DATE_LAST_ANALYSIS";
    public static final String KEY_BUILD_DATE_NOTIFICATION_DISPLAYED = "KEY_BUILD_DATE_NOTIFICATION_DISPLAYED";
    public static final String KEY_TIMESTAMP_LAST_ANALYSIS = "KEY_TIMESTAMP_LAST_ANALYSIS";
    public static final String KEY_DID_SHOW_NEW_FEATURE = "KEY_DID_SHOW_NEW_FEATURE";

    private static JSONObject cachedResultJSON;
    private static String cachedStickyErrorMessage;
    private static Boolean isBuildFromLastAnalysisCertified;


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

    public static void saveStickyErrorMessageNonPersistent(String stickyErrorMessage) {
        cachedStickyErrorMessage = stickyErrorMessage;
    }

    public static void clearSavedStickyErrorMessage(ContextWrapper context) {
        cachedStickyErrorMessage = null;

        Log.d(Constants.LOG_TAG,"Deleting stickyErrorMessage from sharedPrefs");
        putString(KEY_STICKY_ERROR_MESSAGE, "", context);
    }

    public static void clearSavedStickyErrorMessageNonPersistent() {
        cachedStickyErrorMessage = null;
        Log.d(Constants.LOG_TAG,"Deleting cached stickyErrorMessage");
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
        isBuildFromLastAnalysisCertified = null;

        Log.d(Constants.LOG_TAG,"Deleting analysisResult from sharedPrefs");
        putString(KEY_ANALYSIS_RESULT, "", context);
    }

    public static void clearSavedAnalysisResultNonPersistent() {
        cachedResultJSON = null;
        isBuildFromLastAnalysisCertified = null;
        Log.d(Constants.LOG_TAG,"Deleting cached analysisResult");
    }


    public static boolean isBuildFromLastAnalysisCertified(Context context) {
        if (isBuildFromLastAnalysisCertified == null) {
            isBuildFromLastAnalysisCertified = getSharedPrefs(context).getBoolean(KEY_LAST_BUILD_CERTIFIED, false);
        }
        return isBuildFromLastAnalysisCertified;
    }

    public static JSONObject getAnalysisResult(ContextWrapper context) {
        if (cachedResultJSON != null) {
            return cachedResultJSON;
        }

        Log.d(Constants.LOG_TAG, "Reading analysisResult from sharedPrefs");
        SharedPreferences settings = getSharedPrefs(context);

        String analysisResultString = settings.getString(KEY_ANALYSIS_RESULT, "");
        if (analysisResultString.equals("")) {
            return null;
        }

        try {
            cachedResultJSON = new JSONObject(analysisResultString);
            return cachedResultJSON;
        } catch (JSONException e) {
            Log.d(Constants.LOG_TAG, "Could not parse JSON from SharedPrefs. Returning null");
            return null;
        }
    }

    /**
     * @return A stringified version of analysisResultJSON
     */
    public static String saveAnalysisResult(JSONObject analysisResultJSON, boolean isBuildCertified, Context context) {
        long timeStamp = System.currentTimeMillis();
        long buildDateUtc = TestUtils.getBuildDateUtc();

        cachedResultJSON = analysisResultJSON;
        String analysisResultString = analysisResultJSON.toString();

        Log.d(Constants.LOG_TAG,"Writing analysisResult to sharedPrefs");
        SharedPreferences.Editor clearOnUpgradeEditor = getSharedPrefsEditor(context);
        clearOnUpgradeEditor.putString(KEY_ANALYSIS_RESULT, analysisResultString);
        clearOnUpgradeEditor.putBoolean(KEY_LAST_BUILD_CERTIFIED, isBuildCertified);
        clearOnUpgradeEditor.commit();

        SharedPreferences.Editor persistentEditor = getPersistentSharedPrefsEditor(context);
        persistentEditor.putLong(KEY_BUILD_DATE_LAST_ANALYSIS, buildDateUtc);
        persistentEditor.putLong(KEY_TIMESTAMP_LAST_ANALYSIS, timeStamp);
        persistentEditor.commit();

        return analysisResultString;
    }

    // This is needed so that the cached value for the Main App process can be set while the
    // PatchalyzerService modifies the saved value in SharedPrefs
    public static String saveAnalysisResultNonPersistent(JSONObject analysisResultJSON, boolean isBuildCertified) {
        cachedResultJSON = analysisResultJSON;
        isBuildFromLastAnalysisCertified = isBuildCertified;
        return analysisResultJSON.toString();
    }

    public static Boolean getCtsProfileMatchResponse(Context context) {
        Log.d(Constants.LOG_TAG,"Getting ctsProfileMatchResponse from sharedPrefs");
        String response = getSharedPrefs(context).getString(KEY_BUILD_CERTIFIED_CTSPROFILE_MATCH,null);
        if(response == null)
            return null;
        return new Boolean(response);
    }

    public static boolean hasCtsProfileMatchResponse(Context context) {
        Log.d(Constants.LOG_TAG,"Checking if ctsProfileMatchResponse is in sharedPrefs");
        SharedPreferences settings = getSharedPrefs(context);
        return settings.contains(KEY_BUILD_CERTIFIED_CTSPROFILE_MATCH);
    }

    public static void setCtsProfileMatchResponse(Context context, Boolean ctsProfileMatch) {
        Log.d(Constants.LOG_TAG,"Writing ctsProfileMatchResponse to sharedPrefs");
        SharedPreferences.Editor editor = getSharedPrefsEditor(context);
        editor.putString(KEY_BUILD_CERTIFIED_CTSPROFILE_MATCH, ""+ctsProfileMatch);
        editor.commit();
    }

    public static boolean hasBasicIntegrityResponse(Context context) {
        Log.d(Constants.LOG_TAG,"Checking if ctsProfileMatchResponse is in sharedPrefs");
        SharedPreferences settings = getSharedPrefs(context);
        return settings.contains(KEY_BUILD_CERTIFIED_BASICINTEGRITY_MATCH);
    }

    public static Boolean getBasicIntegrityResponse(Context context) {
        Log.d(Constants.LOG_TAG,"Getting ctsProfileMatchResponse from sharedPrefs");
        String response = getSharedPrefs(context).getString(KEY_BUILD_CERTIFIED_BASICINTEGRITY_MATCH,null);
        if(response == null)
            return null;
        return new Boolean(response);
    }

    public static void setBasicIntegrityResponse(Context context, Boolean basicIntegrity) {
        Log.d(Constants.LOG_TAG,"Writing ctsProfileMatchResponse to sharedPrefs");
        SharedPreferences.Editor editor = getSharedPrefsEditor(context);
        editor.putString(KEY_BUILD_CERTIFIED_BASICINTEGRITY_MATCH, ""+basicIntegrity);
        editor.commit();
    }
}
