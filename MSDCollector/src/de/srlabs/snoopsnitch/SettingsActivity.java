package de.srlabs.snoopsnitch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.srlabs.msd.R;
import de.srlabs.snoopsnitch.util.Utils;

public class SettingsActivity extends PreferenceActivity
{	
	@Override
    protected void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        
     // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        
        setTitle(getResources().getText(R.string.settings_actionBar_title));
    }
	
	public void refreshAppId ()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
	    SharedPreferences.Editor editor = sharedPreferences.edit();
	    editor.putString("settings_appId", Utils.generateAppId());
	    editor.commit();
	}
}