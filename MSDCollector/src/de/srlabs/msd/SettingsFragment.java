package de.srlabs.msd;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment 
{
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
	
	@Override
	public void onResume() 
	{
		super.onResume();
		
		//findPreference("settings_appId").setDefaultValue(MSDServiceHelperCreator.getInstance(null).getMsdServiceHelper().)
	}
}
	