package de.srlabs.patchalyzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Checks whether a new build version was installed and prompts the user to perform a new analysis
 */

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefs = SharedPrefsHelper.getPersistentSharedPrefs(context);
        long currentBuildDate = TestUtils.getBuildDateUtc();
        long buildDateUtcAtLastSuccessfulAnalysis = sharedPrefs.getLong(SharedPrefsHelper.KEY_BUILD_DATE_LAST_ANALYSIS, -1);
        long buildDateNotificationDisplayed = sharedPrefs.getLong(SharedPrefsHelper.KEY_BUILD_DATE_NOTIFICATION_DISPLAYED, -1);

        if (currentBuildDate != buildDateNotificationDisplayed && currentBuildDate != buildDateUtcAtLastSuccessfulAnalysis) {
            SharedPrefsHelper.clearSavedAnalysisResult(context);
            SharedPrefsHelper.putLongPersistent(SharedPrefsHelper.KEY_BUILD_DATE_NOTIFICATION_DISPLAYED, currentBuildDate, context);
            if (buildDateUtcAtLastSuccessfulAnalysis == -1) {
                NotificationHelper.showNewPatchalyzerFeatureOnce(context);
            } else {
                NotificationHelper.showBuildVersionChangedNotification(context);
            }
        }
    }
}
