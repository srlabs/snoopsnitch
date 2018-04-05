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
        // TODO: Show this only once when the build date changes, not on every reboot
        SharedPreferences sharedPrefs = context.getSharedPreferences("PATCHALYZER", Context.MODE_PRIVATE);
        long currentBuildDate = TestUtils.getBuildDateUtc();
        long buildDateUtcAtLastSuccessfulAnalysis = sharedPrefs.getLong("buildDateUtcAtLastSuccessfulAnalysis", 0);
        if (buildDateUtcAtLastSuccessfulAnalysis != 0 && buildDateUtcAtLastSuccessfulAnalysis != currentBuildDate) {
            TestUtils.clearSavedAnalysisResult(context);
            NotificationHelper.showBuildVersionChangedNotification(context);
        }
    }
}
