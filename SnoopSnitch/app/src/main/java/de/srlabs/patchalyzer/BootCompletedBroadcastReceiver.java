package de.srlabs.patchalyzer;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;

import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.StartupActivity;

/**
 * Checks whether a new build version was installed and prompts the user to perform a new analysis
 */

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {
    public static final int BUILD_CHANGED_NOTIFICATION_ID = 1146;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: Show this only once when the build date changes, not on every reboot
        SharedPreferences sharedPrefs = context.getSharedPreferences("PATCHALYZER", Context.MODE_PRIVATE);
        long currentBuildDate = TestUtils.getBuildDateUtc();
        long buildDateUtcAtLastSuccessfulAnalysis = sharedPrefs.getLong("buildDateUtcAtLastSuccessfulAnalysis", 0);
        if (buildDateUtcAtLastSuccessfulAnalysis != 0 && buildDateUtcAtLastSuccessfulAnalysis != currentBuildDate) {
            TestUtils.clearSavedAnalysisResult(context);
            showBuildVersionChangedNotification(context);
        }
    }

    private void showBuildVersionChangedNotification(Context context) {
        Intent notificationIntent = new Intent(context, PatchalyzerMainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getText(R.string.patchalyzer_notification_build_changed_title))
                        .setContentText(context.getText(R.string.patchalyzer_notification_build_changed_text))
                        .setSmallIcon(R.drawable.ic_patchalyzer)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(BUILD_CHANGED_NOTIFICATION_ID, notification);

    }
}
