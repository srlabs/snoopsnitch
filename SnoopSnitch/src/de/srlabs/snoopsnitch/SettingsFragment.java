package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	private boolean settingsChanged = false;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		Preference appIDPref = findPreference("settings_appId");
		appIDPref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
		{
			@Override
			public boolean onPreferenceClick(Preference preference) 
			{
				showgenerateAppIdDialog ();
				return true;
			}
		});
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) 
	{
		if(this.getActivity() != null)
			this.getActivity().invalidateOptionsMenu();

		if (key.equals("settings_basebandLogKeepDuration")
		 || key.equals("settings_debugLogKeepDuration")
		 || key.equals("settings_locationLogKeepDuration")
		 || key.equals("settings_analysisLogKeepDuration"))
		{
			MsdConfig.setLastCleanupTime(sharedPreferences, 0);
		}

		settingsChanged = true;
	}
	
	private void showCleanupDialog ()
	{

	}

	private void showgenerateAppIdDialog() {
		showDialog(R.string.alert_dialog_refreshappid_title,
				R.string.alert_dialog_refreshappid_message,
				R.string.alert_dialog_refreshappid_button_refresh,
				R.string.alert_dialog_refreshappid_button_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						((SettingsActivity) getActivity()).refreshAppId();
					}
				});
	}

	private void showDialog(int dialogTitle, int dialogMessage, int dialogPositiveButton, int dialogNegativeButton, OnClickListener dialogOnClick) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this.getActivity());
	      alertDialogBuilder.setTitle(getResources().getString(dialogTitle));
	      alertDialogBuilder.setMessage(getResources().getString(dialogMessage));
	      alertDialogBuilder.setPositiveButton(getResources().getString(dialogPositiveButton), 
	      dialogOnClick);
	      alertDialogBuilder.setNegativeButton(getResources().getString(dialogNegativeButton), 
	      new DialogInterface.OnClickListener() 
	      {		
	         @Override
	         public void onClick(DialogInterface dialog, int which) 
	         {

			 }
	      });
		    
	      AlertDialog alertDialog = alertDialogBuilder.create();
	      alertDialog.show();
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();

		MSDServiceHelperCreator msdServiceHelperCreator = MSDServiceHelperCreator.getInstance(getActivity(), true);
		MsdServiceHelper msdServiceHelper = msdServiceHelperCreator.getMsdServiceHelper();

		if (settingsChanged){
			if (msdServiceHelper.isRecording())
			{
				msdServiceHelper.stopRecording();
				msdServiceHelper.startRecording();
			}
			settingsChanged = false;
		}
		super.onDestroyView();
	}
}
	
