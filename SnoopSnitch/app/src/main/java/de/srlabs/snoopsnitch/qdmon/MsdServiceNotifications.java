package de.srlabs.snoopsnitch.qdmon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.srlabs.snoopsnitch.BuildConfig;
import de.srlabs.snoopsnitch.CrashUploadActivity;
import de.srlabs.snoopsnitch.DashboardActivity;
import de.srlabs.snoopsnitch.EnableAutoUploadModeActivity;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.StartupActivity;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdConfig;

public class MsdServiceNotifications {
    private Context context;
    private static final String LOG_TAG = "MsdServiceNotifications";
    public static final int ERROR_STORAGE_FULL = 1;
    public static final int ERROR_RECORDING_STOPPED_BATTERY_LEVEL = 2;
    public static final int ERROR_ROOT_PRIVILEGES_DENIED = 2;
    public static final String NOTIFICATION_CHANNEL_ID = "snsn-notification-channel";
    private NotificationManager notificationManager;

    public MsdServiceNotifications(Context context) {
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d(LOG_TAG, "Creating notification channel...");
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, context.getString(R.string.snsn_notification_channel_title), NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(context.getString(R.string.snsn_notification_channel_desc));
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true); //reflect default preferences value here (vibrate+ring)
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public Notification getForegroundNotification() {
        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_content_imsi_ok);

        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setContentTitle(context.getString(R.string.app_name))
                .setTicker(context.getString(R.string.app_name) + " " + context.getString(R.string.no_service_running))
                .setContentText(context.getString(R.string.no_service_running) + " [" + BuildConfig.VERSION_NAME + "]")
                .setSmallIcon(R.drawable.ic_content_imsi_ok)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setContentIntent(pendingIntent);


        return notificationBuilder.build();
    }

    public void showImsiCatcherNotification(int numCatchers) {
        Log.i(LOG_TAG, "Showing IMSI Catcher notification for " + numCatchers + " catchers");

        //vibrate and/or play sound or none
        String notificationSetting = MsdConfig.getIMSICatcherNotificationSetting(context.getApplicationContext());
        triggerSenseableNotification(notificationSetting);


        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder nc = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.no_catcher_title))
                .setTicker(context.getString(R.string.no_catcher_ticker))
                .setContentText(numCatchers + " " + context.getString(R.string.no_catcher_events_detected))
                .setSmallIcon(R.drawable.ic_content_imsi_event)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Add a button to enable Auto-Upload Mode when it is not yet enabled
        if (!MsdConfig.getAutoUploadMode(context)) {
            Intent autoUploadIntent = new Intent(context, EnableAutoUploadModeActivity.class);
            autoUploadIntent.putExtra(EnableAutoUploadModeActivity.NOTIFICATION_ID, Constants.NOTIFICATION_ID_IMSI);
            PendingIntent autoUploadPendingIntent = PendingIntent.getActivity(context, 0, autoUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nc.addAction(R.drawable.ic_content_imsi_event, context.getString(R.string.notification_enable_auto_upload_mode), autoUploadPendingIntent);
        }
        Notification n = nc.build();

        notificationManager.notify(Constants.NOTIFICATION_ID_IMSI, n);
    }

    public void showSmsNotification(int numEvents) {
        Log.i(LOG_TAG, "Showing Event notification for " + numEvents + " events");

        //vibrate and/or play sound or none
        String notificationSetting = MsdConfig.getSMSandSS7NotificationSetting(context.getApplicationContext());
        triggerSenseableNotification(notificationSetting);

        Intent intent = new Intent(context, DashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder nc = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.no_events_title))
                .setTicker(context.getString(R.string.no_events_ticker))
                .setContentText(numEvents + " " + context.getString(R.string.no_events_detected))
                .setSmallIcon(R.drawable.ic_content_sms_event)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Add a button to enable Auto-Upload Mode when it is not yet enabled
        if (!MsdConfig.getAutoUploadMode(context)) {
            Intent autoUploadIntent = new Intent(context, EnableAutoUploadModeActivity.class);
            autoUploadIntent.putExtra(EnableAutoUploadModeActivity.NOTIFICATION_ID, Constants.NOTIFICATION_ID_SMS);
            PendingIntent autoUploadPendingIntent = PendingIntent.getActivity(context, 0, autoUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nc.addAction(R.drawable.ic_content_sms_event, context.getString(R.string.notification_enable_auto_upload_mode), autoUploadPendingIntent);
        }

        Notification n = nc.build();
        notificationManager.notify(Constants.NOTIFICATION_ID_SMS, n);
    }

    public void showInternalErrorNotification(String msg, Long debugLogFileId) {
        if(StartupActivity.isSNSNCompatible(context.getApplicationContext())) {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_content_imsi_event);
            Log.i(LOG_TAG, "showInternalErrorNotification(" + msg + "  debugLogFileId=" + debugLogFileId + ")");
            Intent intent = new Intent(context, CrashUploadActivity.class);
            intent.putExtra(CrashUploadActivity.EXTRA_ERROR_ID, debugLogFileId == null ? 0 : (long) debugLogFileId);
            intent.putExtra(CrashUploadActivity.EXTRA_ERROR_TEXT, msg);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_content_imsi_event)
                            .setLargeIcon(icon)
                            .setContentTitle(context.getString(R.string.app_name) + " " + context.getString(R.string.error_notification_title))
                            .setContentText(context.getString(R.string.error_notification_text))
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);

            Notification n = notificationBuilder.build();
            notificationManager.notify(Constants.NOTIFICATION_ID_INTERNAL_ERROR, n);
        }
    }

    public void showExpectedErrorNotification(int errorId) {
        if(StartupActivity.isSNSNCompatible(context.getApplicationContext())) {
            Log.i(LOG_TAG, "showExpectedErrorNotification(" + errorId + ")");
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_content_imsi_event);
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_content_imsi_event)
                            .setLargeIcon(icon)
                            // .setAutoCancel(true)
                            .setContentTitle(context.getString(R.string.app_name) + " " + context.getString(R.string.error_notification_title));

            if (errorId == ERROR_ROOT_PRIVILEGES_DENIED) {
                notificationBuilder.setContentText(context.getString(R.string.error_root_privileges_denied));
            }

            Notification n = notificationBuilder.build();
            notificationManager.notify(Constants.NOTIFICATION_ID_EXPECTED_ERROR, n);
        }
    }


    /**
     * Decide which notification shall be triggered.
     * For the different options please take a look at @array/notification_options_internal
     *
     * //FIXME: Since Android 0 (SDK version >= 26) users can configure notification channel settings by themselves - no need for this anymore, remove later on
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
     *
     * //FIXME: Since Android 0 (SDK version >= 26) users can configure notification channel settings by themselves - no need for this anymore, remove later on
     */
    public void triggerSoundNotification() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
            if (r != null)
                r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vibrate to notify user
     *
     * //FIXME: Since Android 0 (SDK version >= 26) users can configure notification channel settings by themselves - no need for this anymore, remove later on
     */
    public void triggerVibrateNotification() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 400, 500, 400}; // vibrate 2 times with 0.5s interval
        if (v.hasVibrator()) {
            v.vibrate(pattern, -1);          // "-1" = vibrate exactly as pattern, no repeat
        }
    }
}