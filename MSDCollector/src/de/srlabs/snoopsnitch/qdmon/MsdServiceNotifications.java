package de.srlabs.snoopsnitch.qdmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.CrashUploadActivity;
import de.srlabs.snoopsnitch.DashboardActivity;
import de.srlabs.snoopsnitch.util.Constants;

public class MsdServiceNotifications {
	Service service;
	public static final int ERROR_STORAGE_FULL = 1;
	public static final int ERROR_RECORDING_STOPPED_BATTERY_LEVEL = 2;

	public MsdServiceNotifications(Service service) {
		this.service = service;
	}
	public Notification getForegroundNotification(){
		Intent intent = new Intent(service, DashboardActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_content_imsi_ok);
		Notification n = new NotificationCompat.Builder(service)
		.setContentTitle(service.getString(R.string.app_name))
		.setTicker(service.getString(R.string.app_name) + " service running")
		.setContentText("Service running [" + service.getString(R.string.app_version) + "]")
		.setSmallIcon(R.drawable.ic_content_imsi_ok)
		.setLargeIcon(icon)
		.setOngoing(true)
		.setContentIntent(pendingIntent)
		.build();
		// TODO: Allow the user to open the UI from the Notification
		// TODO: Allow the user to stop recording from the Notification
		return n;
	}
	public void showImsiCatcherNotification(int numCatchers, long id){
		Log.i("MsdServiceNotifications","Showing IMSI Catcher notification for " + numCatchers + " catchers, id=" + id);
		
		Intent intent = new Intent(service, DashboardActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification n = new NotificationCompat.Builder(service)
		.setContentTitle("IMSI catcher detected")
		.setTicker("IMSI catcher detected")
		.setContentText("Showing IMSI Catcher notification for " + numCatchers + " catchers, id=" + id)
		.setSmallIcon(R.drawable.ic_content_imsi_event)
		.setOngoing(false)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.build();
		
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
		notificationManager.notify(Constants.NOTIFICATION_ID_IMSI, n);
	}
	public void showSmsNotification(int numSilentSms, int numBinarySms, long id){
		Log.i("MsdServiceNotifications","Showing SMS notification for " + numSilentSms + " silent SMS and " + numBinarySms + " binary SMS , id=" + id);

		Intent intent = new Intent(service, DashboardActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification n = new NotificationCompat.Builder(service)
		.setContentTitle("Silent SMS detected")
		.setTicker("Silent SMS detected")
		.setContentText("Showing SMS notification for " + numSilentSms + " silent SMS and " + numBinarySms + " binary SMS , id=" + id)
		.setSmallIcon(R.drawable.ic_content_sms_event)
		.setOngoing(false)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.build();
		
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
		notificationManager.notify(Constants.NOTIFICATION_ID_SMS, n);
	}
	public void showErrorNotification(int errorId){
		Log.i("MsdServiceNotifications","Showing Error notification for errorId=" + errorId);

		Intent intent = new Intent(service, DashboardActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification n = new NotificationCompat.Builder(service)
		.setContentTitle("Error occured")
		.setTicker("Error occured")
		.setContentText("An error occured: " + errorId)
		.setSmallIcon(R.drawable.ic_content_imsi_event)
		.setOngoing(false)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.build();
		
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
		notificationManager.notify(Constants.NOTIFICATION_ID_INTERNAL_ERROR, n);
	}
	public void showInternalErrorNotification(String msg, Long debugLogFileId){
		// TODO: Maybe directly start the error reporting activity if the app was on top when the error occured
		Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_content_imsi_event);
		Log.i("MsdServiceNotifications","showInternalErrorNotification(" + msg + "  debugLogFileId=" + debugLogFileId + ")");
		Intent intent = new Intent(service, CrashUploadActivity.class);
		intent.putExtra(CrashUploadActivity.EXTRA_ERROR_ID, debugLogFileId == null ? 0:(long)debugLogFileId);
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
}
