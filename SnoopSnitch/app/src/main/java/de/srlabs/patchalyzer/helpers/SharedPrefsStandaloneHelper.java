package de.srlabs.patchalyzer.helpers;

import android.content.Context;
import android.content.SharedPreferences;


public class SharedPrefsStandaloneHelper extends SharedPrefsHelper{
    //-- Patch analysis related
    public static String getPatchAnalysisNotificationSetting(Context context) {
        return getPersistentSharedPrefs(context).getString("settings_patch_analysis_event", "vibrate+ring");
    }

    public static boolean getShowInconclusivePatchAnalysisTestResults(Context context) {
        return getPersistentSharedPrefs(context).getBoolean("settings_patch_analysis_show_inconclusive", false);
    }

    public static void setShowInconclusiveResults(Context context, boolean showInconclusive) {
        SharedPreferences.Editor edit = getPersistentSharedPrefs(context).edit();
        edit.putBoolean("settings_patch_analysis_show_inconclusive", showInconclusive);
        edit.commit();
    }

    public static String getAppId(Context context) {
        return getPersistentSharedPrefs(context).getString("settings_appId", "");
    }

    public static void setAppId(Context context, String appID) {
        SharedPreferences.Editor editor = getPersistentSharedPrefsEditor(context);
        editor.putString("settings_appId", appID);
        editor.commit();
    }
}
