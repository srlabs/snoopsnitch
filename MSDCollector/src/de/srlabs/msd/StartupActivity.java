package de.srlabs.msd;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import de.srlabs.msd.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.msd.qdmon.MsdServiceHelper;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msd.util.MsdDialog;
import de.srlabs.msd.util.MsdLog;

/**
 * This class is launched when starting the App. It will display a dialog if the
 * device is not compatible or if it is the first run of the App (so that the
 * user has to confirm to continue). If the device is compatible and the user
 * has already confirmed the first run dialog, it will directly switch over to
 * DashboardActivity.
 * 
 */
public class StartupActivity extends Activity{
    private MsdSQLiteOpenHelper helper;
	private boolean alreadyClicked = false;
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	String incompatibilityReason = DeviceCompatibilityChecker.checkDeviceCompatibility(this.getApplicationContext());
        if(incompatibilityReason == null){
    		final SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
        	if(sharedPreferences.getBoolean("app_first_run", true)){
        		showFirstRunDialog();
        	} else{
        		startDashboard();
        	}
        } else{
        	showDeviceIncompatibleDialog(incompatibilityReason);
        }
    }

    private void showDeviceIncompatibleDialog(String incompatibilityReason){
    	
    	String dialogMessage =
    			getResources().getString(R.string.alert_deviceCompatibility_header) + "\n(" +
    			incompatibilityReason + ")\n\n" +
    			getResources().getString(R.string.alert_deviceCompatibility_message);

    	MsdDialog.makeFatalConditionDialog(this, dialogMessage, new OnClickListener() 
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
		}, false
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
						if(alreadyClicked)
							return;
						alreadyClicked = true;
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
						if(alreadyClicked)
							return;
						quitApplication();
					}
				},
				new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						if(alreadyClicked)
							return;
						quitApplication();
					}
				}, false
				).show();
	}
	protected void quitApplication ()
	{
		finish();
		System.exit(0);
	}
	private void startDashboard() {
		// Initialize the database before starting DashboardActivity
		// TODO: Maybe show some status indication (e.g. a Toast message) while creating the database
		helper = new MsdSQLiteOpenHelper(this);
		SQLiteDatabase db = helper.getReadableDatabase();
		db.rawQuery("SELECT * FROM config", null).close();
		db.close();
		helper.close();
        Intent i = new Intent(this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
        finish();
	}

}
