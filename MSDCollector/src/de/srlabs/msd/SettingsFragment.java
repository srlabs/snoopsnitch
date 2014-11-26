package de.srlabs.msd;

import de.srlabs.msd.util.MSDServiceHelperCreator;
import android.R.raw;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Debug;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        Preference myPref = (Preference) findPreference("settings_appId");
        myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
        {
                     public boolean onPreferenceClick(Preference preference) 
                     {
                    	 showgenerateAppIdDialog ();
                    	 
                    	 return true;
                     }
                 });
    }
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) 
	{
		updatePreference(findPreference(key));		
	}
	
	private void updatePreference(Preference preference) 
	{
		if (preference.getKey().equals("settings_appId")) 
		{
			this.getActivity().invalidateOptionsMenu();
		}
	}
	
	private void showgenerateAppIdDialog ()
	{
	      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this.getActivity());
	      alertDialogBuilder.setTitle(getResources().getString(R.string.alert_dialog_refreshappid_title));
	      alertDialogBuilder.setMessage(getResources().getString(R.string.alert_dialog_refreshappid_message));
	      alertDialogBuilder.setPositiveButton(getResources().getString(R.string.alert_dialog_refreshappid_button_refresh), 
	      new DialogInterface.OnClickListener() 
	      {		
	         @Override
	         public void onClick(DialogInterface arg0, int arg1) 
	         {
	        	 ((SettingsActivity)getActivity()).refreshAppId();
	         }
	      });
	      alertDialogBuilder.setNegativeButton(getResources().getString(R.string.alert_dialog_refreshappid_button_cancel), 
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
}
	