package de.srlabs.snoopsnitch.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import de.srlabs.snoopsnitch.R;

public class MsdDialog extends DialogFragment
{	
	public static Dialog makeConfirmationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener, Boolean backButtonActive){
		return makeConfirmationDialog(activity, message, positiveOnClickListener, negativeOnClickListener,null, backButtonActive);
	}
	public static Dialog makeConfirmationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener,
			OnCancelListener onCancelListener, Boolean backButtonActive){
		return makeConfirmationDialog(activity, message, positiveOnClickListener, negativeOnClickListener,null, 
				activity.getResources().getString(R.string.alert_button_ok), activity.getString(R.string.alert_button_cancel), backButtonActive);	
	}
	public static Dialog makeConfirmationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener,
			OnCancelListener onCancelListener, String positiveButtonText, String negativeButtonText, Boolean backButtonActive)
	{
		AlertDialog.Builder builder = getAlertDialogBuilder(activity, 
				activity.getResources().getString(R.string.alert_confirmation_title), message);
		
		builder.setPositiveButton(positiveButtonText, positiveOnClickListener);
		builder.setNegativeButton(negativeButtonText, negativeOnClickListener);
		builder.setOnCancelListener(onCancelListener);

		builder.setIcon(android.R.drawable.ic_dialog_info);
		
		builder.setCancelable(backButtonActive);
		
		return builder.create();
	}
	public static Dialog makeFatalConditionDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, String detailText, Boolean backButtonActive){
		return makeFatalConditionDialog(activity, message, positiveOnClickListener, detailText, null, backButtonActive);
	}
	public static Dialog makeFatalConditionDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, String detailText, OnCancelListener onCancelListener, Boolean backButtonActive)
	{
		// TODO: Add detail text
		AlertDialog.Builder builder = getAlertDialogBuilder(activity, 
				activity.getResources().getString(R.string.alert_fatal_condition_title), message);
		
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		
		builder.setPositiveButton(activity.getString(R.string.alert_button_quit), positiveOnClickListener);
		builder.setOnCancelListener(onCancelListener);
		
		builder.setCancelable(backButtonActive);
		
		return builder.create();
	}
	
	public static Dialog makeFatalConditionDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, OnClickListener negativeOnClickListener, String detailText, Boolean backButtonActive)
	{
		// TODO: Add detail text
		AlertDialog.Builder builder = getAlertDialogBuilder(activity,
				activity.getResources().getString(R.string.alert_fatal_condition_title), message);

		builder.setIcon(android.R.drawable.ic_dialog_alert);
		
		builder.setPositiveButton(activity.getString(R.string.alert_button_quit), positiveOnClickListener);
		builder.setNegativeButton(activity.getString(R.string.alert_button_quit), positiveOnClickListener);
		
		builder.setCancelable(backButtonActive);
		
		return builder.create();
	}
	
	public static Dialog makeNotificationDialog (Activity activity, String message, 
			OnClickListener positiveOnClickListener, Boolean backButtonActive)
	{
		AlertDialog.Builder builder = getAlertDialogBuilder(activity, 
				activity.getResources().getString(R.string.alert_notification_title), message);
		
		builder.setPositiveButton(activity.getString(R.string.alert_button_ok), positiveOnClickListener);
		
		builder.setCancelable(backButtonActive);
		
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
