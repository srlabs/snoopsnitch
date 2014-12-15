package de.srlabs.msd.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import de.srlabs.msd.R;

public class MsdDialog extends DialogFragment
{	
	public static Dialog makeConfirmationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener){
		return makeConfirmationDialog(activity, message, positiveOnClickListener, negativeOnClickListener,null);
	}
	public static Dialog makeConfirmationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener,
			OnCancelListener onCancelListener){
		return makeConfirmationDialog(activity, message, positiveOnClickListener, negativeOnClickListener,null, 
				activity.getResources().getString(R.string.alert_button_ok), activity.getString(R.string.alert_button_cancel));	
	}
	public static Dialog makeConfirmationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener,
			OnCancelListener onCancelListener, String positiveButtonText, String negativeButtonText)
	{
		AlertDialog.Builder builder = getAlertDialogBuilder(activity, 
				activity.getResources().getString(R.string.alert_confirmation_title), message);
		
		builder.setPositiveButton(positiveButtonText, positiveOnClickListener);
		builder.setNegativeButton(negativeButtonText, negativeOnClickListener);
		builder.setOnCancelListener(onCancelListener);

		builder.setIcon(android.R.drawable.ic_dialog_info);
		
		return builder.create();
	}
	public static Dialog makeFatalConditionDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, String detailText){
		return makeFatalConditionDialog(activity, message, positiveOnClickListener, detailText, null);
	}
	public static Dialog makeFatalConditionDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, String detailText, OnCancelListener onCancelListener)
	{
		// TODO: Add detail text
		AlertDialog.Builder builder = getAlertDialogBuilder(activity, 
				activity.getResources().getString(R.string.alert_fatal_condition_title), message);
		
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		
		builder.setPositiveButton(activity.getString(R.string.alert_button_quit), positiveOnClickListener);
		builder.setOnCancelListener(onCancelListener);
		return builder.create();
	}
	
	public static Dialog makeFatalConditionDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener, String detailText)
	{
		// TODO: Add detail text
		AlertDialog.Builder builder = getAlertDialogBuilder(activity,
				activity.getResources().getString(R.string.alert_fatal_condition_title), message);

		builder.setIcon(android.R.drawable.ic_dialog_alert);
		
		builder.setPositiveButton(activity.getString(R.string.alert_button_quit), positiveOnClickListener);
		builder.setNegativeButton(activity.getString(R.string.alert_button_quit), positiveOnClickListener);
		
		return builder.create();
	}
	
	public static Dialog makeNotificationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener)
	{
		AlertDialog.Builder builder = getAlertDialogBuilder(activity, 
				activity.getResources().getString(R.string.alert_notification_title), message);
		
		builder.setPositiveButton(activity.getString(R.string.alert_button_ok), positiveOnClickListener);
		
		return builder.create();
	}
	
	private static AlertDialog.Builder getAlertDialogBuilder (Activity activity, String title, String message)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder (activity);
		
		builder.setTitle(title);
		builder.setMessage(message);
		
		return builder;
	}
}
