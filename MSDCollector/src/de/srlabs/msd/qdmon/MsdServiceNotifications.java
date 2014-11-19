package de.srlabs.msd.qdmon;

import android.app.Notification;
import android.app.Service;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;
import de.srlabs.msd.util.Constants;
import de.srlabs.msdcollector.R;

public class MsdServiceNotifications {
	Service service;
	public static final int ERROR_STORAGE_FULL = 1;
	public static final int ERROR_RECORDING_STOPPED_BATTERY_LEVEL = 2;

	public MsdServiceNotifications(Service service) {
		this.service = service;
	}
	public Notification getForegroundNotification(){
		Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_launcher);
		Notification n = new NotificationCompat.Builder(service)
		.setContentTitle("MSD.setContentTitle")
		.setTicker("MSD.setTicker")
		.setContentText("MSD.setContentText")
		.setSmallIcon(R.drawable.ic_launcher)
		.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
		.setOngoing(true)
		.build();
		// TODO: Allow the user to open the UI from the Notification
		// TODO: Allow the user to stop recording from the Notification
		return n;
	}
	public void showImsiCatcherNotification(int numCatchers, long id){
		Log.i("MsdServiceNotifications","Showing IMSI Catcher notification for " + numCatchers + " catchers, id=" + id);
		Toast.makeText(service, "Showing IMSI Catcher notification for " + numCatchers + " catchers, id=" + id, Toast.LENGTH_SHORT).show();
		// TODO: Implement notification and remove Toast
	}
	public void showSmsNotification(int numSilentSms, int numBinarySms, long id){
		Log.i("MsdServiceNotifications","Showing SMS notification for " + numSilentSms + " silent SMS and " + numBinarySms + " binary SMS , id=" + id);
		Toast.makeText(service, "Showing SMS notification for " + numSilentSms + " silent SMS and " + numBinarySms + " binary SMS , id=" + id, Toast.LENGTH_SHORT).show();
		// TODO: Implement notification and remove Toast
	}
	public void showErrorNotification(int errorId){
		Log.i("MsdServiceNotifications","Showing Error notification for errorId=" + errorId);
		Toast.makeText(service, "Showing Error notification for errorId=" + errorId, Toast.LENGTH_SHORT).show();
		// TODO: Implement notification and remove Toast
	}
	public void showInternalErrorNotification(String msg, Integer debugLogFileId){
		Log.i("MsdServiceNotifications","showInternalErrorNotification(" + msg + "  debugLogFileId=" + debugLogFileId + ")");
		// TODO: Make this notification pretty
		// TODO: Add a button for marking the error log file as pending for upload
		NotificationCompat.Builder notificationBuilder =
				new NotificationCompat.Builder(service)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("showInternalErrorNotification")
		.setContentText(msg);
		Notification n = notificationBuilder.build();
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
		notificationManager.notify(Constants.NOTIFICATION_ID_INTERNAL_ERROR, n);
	}
}
