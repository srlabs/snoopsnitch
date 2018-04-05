package de.srlabs.patchalyzer;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.StartupActivity;

/**
 * Handles creation of all notifications displayed by the Patchalyzer
 */

public class NotificationHelper {
    public static final int NEW_FEATURE_NOTIFICATION_ID = 1145;
    public static final int BUILD_CHANGED_NOTIFICATION_ID = 1146;
    public static final int ONGOING_NOTIFICATION_ID = 1147;
    public static final int FINISHED_NOTIFICATION_ID = 1148;
    public static final int FAILED_NOTIFICATION_ID = 1149;

    public static void cancelNonStickyNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NEW_FEATURE_NOTIFICATION_ID);
        notificationManager.cancel(BUILD_CHANGED_NOTIFICATION_ID);
        notificationManager.cancel(FINISHED_NOTIFICATION_ID);
        notificationManager.cancel(FAILED_NOTIFICATION_ID);
    }

    public static void showNewPatchalyzerFeatureOnce(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("PATCHALYZER", Context.MODE_PRIVATE);
        boolean didShowAlready = sharedPrefs.getBoolean("didShowNewFeatureNotification",false);

        if(!didShowAlready) {
            Intent notificationIntent = new Intent(context, StartupActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, 0, notificationIntent, 0);
            Notification notification =
                    new Notification.Builder(context)
                            .setContentTitle(context.getText(R.string.patchalyzer_notification_new_feature_title))
                            .setContentText(context.getText(R.string.patchalyzer_notification_new_feature_text))
                            .setSmallIcon(R.drawable.ic_patchalyzer)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NEW_FEATURE_NOTIFICATION_ID, notification);

            //persist that we showed the notification already, to not show it again
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean("didShowNewFeatureNotification",true);
            editor.commit();
        }
    }

    public static void showBuildVersionChangedNotification(Context context) {
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

    public static void showAnalysisFinishedNotification(Context context) {
        Intent notificationIntent = new Intent(context, PatchalyzerMainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getText(R.string.patchalyzer_finished_notification_title))
                        .setContentText(context.getText(R.string.patchalyzer_finished_notification_text))
                        .setSmallIcon(R.drawable.ic_patchalyzer)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification);
    }

    public static void showAnalysisFailedNotification(Context context) {
        Log.d(Constants.LOG_TAG, "TestExeCutorService.showAnalysisFailedNotification called");
        Intent notificationIntent = new Intent(context, PatchalyzerMainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getText(R.string.patchalyzer_failed_notification_title))
                        .setContentText(context.getText(R.string.patchalyzer_failed_notification_text))
                        .setSmallIcon(R.drawable.ic_patchalyzer)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(FAILED_NOTIFICATION_ID, notification);
    }

    public static Notification getAnalysisOngoingNotification(Context context) {
        Intent notificationIntent = new Intent(context, PatchalyzerMainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(context)
                        .setContentTitle(context.getText(R.string.patchalyzer_running_notification_title))
                        .setContentText(context.getText(R.string.patchalyzer_running_notification_text))
                        .setSmallIcon(R.drawable.ic_patchalyzer)
                        .setContentIntent(pendingIntent)
                        .build();
        return notification;
    }
}
