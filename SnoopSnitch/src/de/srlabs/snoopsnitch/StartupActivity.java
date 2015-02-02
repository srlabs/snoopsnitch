package de.srlabs.snoopsnitch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.DeviceCompatibilityChecker;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.Utils;

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
	private ProgressDialog progressDialog;
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	String incompatibilityReason = DeviceCompatibilityChecker.checkDeviceCompatibility(this.getApplicationContext());
        if(incompatibilityReason == null){
        	if(MsdConfig.getFirstRun(this)){
        		showFirstRunDialog();
        	} else{
        		createDatabaseAndStartDashboard();
        	}
        } else{
        	showDeviceIncompatibleDialog(incompatibilityReason);
        }
    }

    private void showDeviceIncompatibleDialog(String incompatibilityReason){
    	Utils.showDeviceIncompatibleDialog(this, incompatibilityReason, new Runnable() {
			@Override
			public void run() {
				quitApplication();
			}
		});
    }
    
	private void showFirstRunDialog() {
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
					    MsdConfig.setFirstRun(StartupActivity.this, false);
						createDatabaseAndStartDashboard();
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
	private void createDatabaseAndStartDashboard() {
		progressDialog = ProgressDialog.show(this, "Initializing database", "Please wait...", true);
		progressDialog.show();
		final Handler handler = new Handler();
		Thread t = new Thread(){
			@Override
			public void run() {
				helper = new MsdSQLiteOpenHelper(StartupActivity.this);
				SQLiteDatabase db = helper.getReadableDatabase();
				db.rawQuery("SELECT * FROM config", null).close();
				db.close();
				helper.close();
				handler.post(new Runnable() {
					@Override
					public void run() {
						progressDialog.dismiss();
						startDashboard();
					}
				});
			}
		};
		t.start();
	}
	private void startDashboard(){
        Intent i = new Intent(StartupActivity.this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        StartupActivity.this.startActivity(i);
        finish();
	}
}
