package de.srlabs.snoopsnitch.qdmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import de.srlabs.snoopsnitch.CrashUploadActivity;
import de.srlabs.snoopsnitch.DashboardActivity;
import de.srlabs.snoopsnitch.EnableAutoUploadModeActivity;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdConfig;

public class MsdServiceNotifications {
    Service service;
    public static final int ERROR_STORAGE_FULL = 1;
    public static final int ERROR_RECORDING_STOPPED_BATTERY_LEVEL = 2;
    public static final int ERROR_ROOT_PRIVILEGES_DENIED = 2;

    public MsdServiceNotifications(Service service) {
        this.service = service;
    }

    public Notification getForegroundNotification() {
        Intent intent = new Intent(service, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_content_imsi_ok);
        Notification n = new NotificationCompat.Builder(service)
                .setContentTitle(service.getString(R.string.app_name))
                .setTicker(service.getString(R.string.app_name) + " " + service.getString(R.string.no_service_running))
                .setContentText(service.getString(R.string.no_service_running) + " [" + service.getString(R.string.app_version) + "]")
                .setSmallIcon(R.drawable.ic_content_imsi_ok)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
        // TODO: Allow the user to open the UI from the Notification
        // TODO: Allow the user to stop recording from the Notification
        return n;
    }

    public void showImsiCatcherNotification(int numCatchers) {
        Log.i("MsdServiceNotifications", "Showing IMSI Catcher notification for " + numCatchers + " catchers");

        //vibrate and/or play sound or none
        String notificationSetting = MsdConfig.getIMSICatcherNotificationSetting(service.getApplicationContext());
        triggerSenseableNotification(notificationSetting);


        Intent intent = new Intent(service, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder nc = new NotificationCompat.Builder(service)
                .setContentTitle(service.getString(R.string.no_catcher_title))
                .setTicker(service.getString(R.string.no_catcher_ticker))
                .setContentText(numCatchers + " " + service.getString(R.string.no_catcher_events_detected))
                .setSmallIcon(R.drawable.ic_content_imsi_event)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        // Add a button to enable Auto-Upload Mode when it is not yet enabled
        if (!MsdConfig.getAutoUploadMode(service)) {
            Intent autoUploadIntent = new Intent(service, EnableAutoUploadModeActivity.class);
            autoUploadIntent.putExtra(EnableAutoUploadModeActivity.NOTIFICATION_ID, Constants.NOTIFICATION_ID_IMSI);
            PendingIntent autoUploadPendingIntent = PendingIntent.getActivity(service, 0, autoUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nc.addAction(R.drawable.ic_content_imsi_event, service.getString(R.string.notification_enable_auto_upload_mode), autoUploadPendingIntent);
        }
        Notification n = nc.build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
        notificationManager.notify(Constants.NOTIFICATION_ID_IMSI, n);
    }

    public void showSmsNotification(int numEvents) {
        Log.i("MsdServiceNotifications", "Showing Event notification for " + numEvents + " events");

        //vibrate and/or play sound or none
        String notificationSetting = MsdConfig.getSMSandSS7NotificationSetting(service.getApplicationContext());
        triggerSenseableNotification(notificationSetting);

        Intent intent = new Intent(service, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder nc = new NotificationCompat.Builder(service)
                .setContentTitle(service.getString(R.string.no_events_title))
                .setTicker(service.getString(R.string.no_events_ticker))
                .setContentText(numEvents + " " + service.getString(R.string.no_events_detected))
                .setSmallIcon(R.drawable.ic_content_sms_event)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        // Add a button to enable Auto-Upload Mode when it is not yet enabled
        if (!MsdConfig.getAutoUploadMode(service)) {
            Intent autoUploadIntent = new Intent(service, EnableAutoUploadModeActivity.class);
            autoUploadIntent.putExtra(EnableAutoUploadModeActivity.NOTIFICATION_ID, Constants.NOTIFICATION_ID_SMS);
            PendingIntent autoUploadPendingIntent = PendingIntent.getActivity(service, 0, autoUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nc.addAction(R.drawable.ic_content_sms_event, service.getString(R.string.notification_enable_auto_upload_mode), autoUploadPendingIntent);
        }
        Notification n = nc.build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
        notificationManager.notify(Constants.NOTIFICATION_ID_SMS, n);
    }

    public void showInternalErrorNotification(String msg, Long debugLogFileId) {
        // TODO: Maybe directly start the error reporting activity if the app was on top when the error occured
        Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_content_imsi_event);
        Log.i("MsdServiceNotifications", "showInternalErrorNotification(" + msg + "  debugLogFileId=" + debugLogFileId + ")");
        Intent intent = new Intent(service, CrashUploadActivity.class);
        intent.putExtra(CrashUploadActivity.EXTRA_ERROR_ID, debugLogFileId == null ? 0 : (long) debugLogFileId);
        intent.putExtra(CrashUploadActivity.EXTRA_ERROR_TEXT, msg);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // TODO: Make this notification pretty
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(service)
                        .setSmallIcon(R.drawable.ic_content_imsi_event)
                        .setLargeIcon(icon)
                        .setContentTitle(service.getString(R.string.app_name) + " " + service.getString(R.string.error_notification_title))
                        .setContentText(service.getString(R.string.error_notification_text))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);
        Notification n = notificationBuilder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
        notificationManager.notify(Constants.NOTIFICATION_ID_INTERNAL_ERROR, n);
    }

    public void showExpectedErrorNotification(int errorId) {
        Log.i("MsdServiceNotifications", "showExpectedErrorNotification(" + errorId + ")");
        Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_content_imsi_event);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(service)
                        .setSmallIcon(R.drawable.ic_content_imsi_event)
                        .setLargeIcon(icon)
                        // .setAutoCancel(true)
                        .setContentTitle(service.getString(R.string.app_name) + " " + service.getString(R.string.error_notification_title));
        ;
        if (errorId == ERROR_ROOT_PRIVILEGES_DENIED) {
            notificationBuilder.setContentText(service.getString(R.string.error_root_privileges_denied));
        }
        Notification n = notificationBuilder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
        notificationManager.notify(Constants.NOTIFICATION_ID_EXPECTED_ERROR, n);
    }


    /**
     * Decide which notification shall be triggered.
     * For the different options please take a look at @array/notification_options_internal
     *
     * @param notificationSetting
     */
    public void triggerSenseableNotification(String notificationSetting) {

        switch (notificationSetting) {
            case "vibrate":
                triggerVibrateNotification();
                break;
            case "ring":
                triggerSoundNotification();
                break;
            case "vibrate+ring":
                triggerVibrateNotification();
                triggerSoundNotification();
                break;
            default:
                break;
        }
    }

    /**
     * Play a sound notification
     * Can be used to make the user notice / signal status changes.
     */
    public void triggerSoundNotification() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(service.getApplicationContext(), notification);
            if (r != null)
                r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vibrate to notify user
     */
    public void triggerVibrateNotification() {
        Vibrator v = (Vibrator) service.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 400, 500, 400}; // vibrate 2 times with 0.5s interval
        if (v.hasVibrator()) {
            v.vibrate(pattern, -1);          // "-1" = vibrate exactly as pattern, no repeat
        }
    }
}
