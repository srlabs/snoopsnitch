package de.srlabs.msd;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msd.util.MsdDialog;

/**
 * This class is launched when starting the App. It will display a dialog if the
 * device is not compatible or if it is the first run of the App (so that the
 * user has to confirm to continue). If the device is compatible and the user
 * has already confirmed the first run dialog, it will directly switch over to
 * DashboardActivity.
 * 
 */
public class StartupActivity extends Activity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        boolean deviceCompatible = DeviceCompatibilityChecker.checkDeviceCompatibility() == null;
        if(deviceCompatible){
    		final SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
        	if(sharedPreferences.getBoolean("app_first_run", true)){
        		showFirstRunDialog();
        	} else{
        		startDashboard();
        	}
        } else{
        	showDeviceIncompatibleDialog();
        }
    }

    private void showDeviceIncompatibleDialog(){
    	MsdDialog.makeFatalConditionDialog(this, getResources().getString(R.string.alert_deviceCompatibility_message), 
				new OnClickListener() 
		{	
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				quitApplication();
			}
		}, null,
		new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				quitApplication();
			}
		}
		).show();
    }
	private void showFirstRunDialog() {
		final SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
		MsdDialog.makeConfirmationDialog(this, getResources().getString(R.string.alert_first_app_start_message),
				new OnClickListener() 
				{
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
					    // record the fact that the app has been started at least once
					    sharedPreferences.edit().putBoolean("app_first_run", false).commit(); 	
					    startDashboard();
					}
				},
				new OnClickListener() 
				{	
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						quitApplication();
					}
				},
				new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						quitApplication();
					}
				}
				).show();
	}
	protected void quitApplication ()
	{
		finish();
		System.exit(0);
	}
	private void startDashboard() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
        finish();
	}

}
