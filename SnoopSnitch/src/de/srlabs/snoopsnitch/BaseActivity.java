package de.srlabs.snoopsnitch;

import java.util.Vector;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;
import de.srlabs.snoopsnitch.upload.DumpFile;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.Utils;

public class BaseActivity extends FragmentActivity
{	
	// Attributes
	protected MSDServiceHelperCreator msdServiceHelperCreator;
	protected TextView messageText;
	protected View messageLayout;
	protected Toast messageToast;
	protected Menu menu;
	protected Boolean isInForeground = false;
	protected Handler handler;
	protected final int refresh_intervall = 1000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = getLayoutInflater();
		messageLayout = inflater.inflate(R.layout.custom_message_popdown,
                (ViewGroup) findViewById(R.id.toast_layout_root));	
		messageText = (TextView) messageLayout.findViewById(R.id.text);
		messageToast = new Toast(getApplicationContext());
		
		// Get MsdService Helper
		msdServiceHelperCreator = MSDServiceHelperCreator.getInstance(this.getApplicationContext(), true);
		MsdLog.init(msdServiceHelperCreator.getMsdServiceHelper());
		MsdLog.i("MSD","MSD_ACTIVITY_CREATED: " + getClass().getCanonicalName());
		
		handler = new Handler();
	}
	
	@Override
	protected void onResume() 
	{		
		msdServiceHelperCreator.setCurrentActivity(this);
		
		isInForeground = true;
		// Set title/subtitle of the action bar...
		ActionBar ab = getActionBar();
		
		ab.setTitle(R.string.actionBar_title);
		ab.setSubtitle(setAppId ());
		
		handler.postDelayed(runnable, refresh_intervall);
			
		setRecordingIcon ();
		
		super.onResume();
	}
	
	@Override
	protected void onPause() 
	{
		isInForeground = false;
		handler.removeCallbacks(runnable);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() 
	{	
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu _menu) 
	{
	    // Inflate the menu items for use in the action bar
		this.menu = _menu;
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    
		if (msdServiceHelperCreator.getMsdServiceHelper().isRecording())
		{
			menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_record_disable));
		}
		else
		{
			menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_notrecord_disable));
		}
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	protected void showMap ()
	{
	    Intent intent = new Intent(this, MapActivity.class);
	    startActivity(intent);
	}
	
	protected void showTestScreen ()
	{
	    Intent intent = new Intent(this, MsdServiceHelperTest.class);
	    startActivity(intent);
	}
	
	protected void showSettings ()
	{
	    Intent intent = new Intent(this, SettingsActivity.class);
	    startActivity(intent);
	}
	
	protected void showAbout ()
	{
	    Intent intent = new Intent(this, AboutActivity.class);
	    startActivity(intent);
	}
	
	protected void toggleRecording ()
	{
		Boolean isRecording = msdServiceHelperCreator.getMsdServiceHelper().isRecording();
		
		if (isRecording)
		{
			msdServiceHelperCreator.getMsdServiceHelper().stopRecording();
		}
		else
		{
			msdServiceHelperCreator.getMsdServiceHelper().startRecording();
		}
	}
	
	public MSDServiceHelperCreator getMsdServiceHelperCreator ()
	{
		return msdServiceHelperCreator;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    switch (item.getItemId()) 
	    {
	    	case R.id.menu_action_scan:
	    		toggleRecording ();
	    		break;
		    case R.id.menu_action_map:
		      showMap();
		      break;
		    case R.id.menu_action_info:
		      showTestScreen();
		      break;
		    case R.id.menu_action_active_test_advanced:
			    Intent intent = new Intent(this, ActiveTestAdvanced.class);
			    startActivity(intent);
		    	break;
		    case R.id.menu_action_upload_pending_files:
		    	getMsdServiceHelperCreator().getMsdServiceHelper().triggerUploading();
		    	break;
		    case R.id.menu_action_upload_suspicious_dumps:
		    	MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(this));
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		    	Vector<DumpFile> files = DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, System.currentTimeMillis() - 3600 * 1000, System.currentTimeMillis(), 0);
		    	long firstStartTime = System.currentTimeMillis();
		    	String reportId = "";
		    	for(DumpFile file:files){
		    		if(file.getStart_time() < firstStartTime){
		    			firstStartTime = file.getStart_time();
		    			reportId = file.getReportId();
		    		}
		    	}
		    	if(reportId == null){
		    		// TODO: Show error message, no file is available
					MsdDatabaseManager.getInstance().closeDatabase();
		    		return true;
		    	}
		    	// TODO: Show dialog for confirmation:
		    	// * Inform user about uploading private data
		    	// * Show report ID and encourage user to send context information to some email address
		    	boolean confirmed = true;
		    	if(confirmed){
			    	for(DumpFile file:files){
			    		file.markForUpload(db);
			    	}
					getMsdServiceHelperCreator().getMsdServiceHelper().triggerUploading();
		    	}
				MsdDatabaseManager.getInstance().closeDatabase();
		    	break;
		    case R.id.menu_action_upload_debug_logs:
		    	// TODO: Show confirmation dialog
		    	confirmed = true;
		    	if(confirmed){
					long debugLogId = getMsdServiceHelperCreator().getMsdServiceHelper().reopenAndUploadDebugLog();
					if(debugLogId == 0){
						// TODO Show error dialog
					} else{
						db = MsdDatabaseManager.getInstance().openDatabase();
						DumpFile df = DumpFile.get(db, debugLogId);
						MsdDatabaseManager.getInstance().closeDatabase();
						reportId = df.getReportId();
						// TODO: Show dialog with reportId, encourage user to report additional info via email
					}
		    	}
		    	break;
		    case R.id.menu_action_settings:
		    	showSettings ();
		    	break;
		    case R.id.menu_action_about:
		    	showAbout ();
		    	break;
		    case android.R.id.home:
		        NavUtils.navigateUpFromSameTask(this);
		        break;
		    default:
		    	MsdLog.e("BaseActivity","Invalid menu entry pressed,  id=" + item.getItemId());
		      break;
	    }

	    return true;
	}
	
	private void showMessage (String message)
	{
		if (isInForeground)
		{
			messageText.setText(message);
			messageToast.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP, 0, getActionBar().getHeight());
			messageToast.setDuration(Toast.LENGTH_LONG);
			messageToast.setView(messageLayout);
			messageToast.show();
		}
	}

	public void internalError(String errorMsg) 
	{
		MsdDialog.makeFatalConditionDialog(this, "A fatal error occured!", new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				quitApplication();
			}
		}, errorMsg, false).show();
	}

	public void stateChanged(StateChangedReason reason) 
	{	
		if (reason.equals(StateChangedReason.RECORDING_STATE_CHANGED))
		{
			if (menu != null)
			{
				if (msdServiceHelperCreator.getMsdServiceHelper().isRecording())
				{
					menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_record_disable));
					showMessage(getResources().getString(R.string.message_recordingStarted));
				}
				else
				{
					menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_notrecord_disable));
					showMessage(getResources().getString(R.string.message_recordingStopped));
				}	
			}
		}
	}
	
	private String setAppId ()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_MULTI_PROCESS);

		if (sharedPreferences.getString("settings_appId", "") == "")
		{
	        SharedPreferences.Editor editor = sharedPreferences.edit();
	        editor.putString("settings_appId", Utils.generateAppId());
	        editor.commit();
		}
		
		
		return getResources().getText(R.string.actionBar_subTitle) + " " + sharedPreferences.getString("settings_appId", "");
	}
	
	protected Runnable runnable = new Runnable() 
	{
		   @Override
		   public void run() 
		   {
		      /* do what you need to do */
		      refreshView();
		      /* and here comes the "trick" */
		      handler.postDelayed(runnable, refresh_intervall);
		   }
		};
	
	protected void refreshView () {}
	
	private void setRecordingIcon ()
	{
		if (menu != null)
		{
			if (msdServiceHelperCreator.getMsdServiceHelper().isRecording())
			{
				menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_record_disable));
			}
			else
			{
				menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_notrecord_disable));
			}	
		}
	}
	
	protected void quitApplication ()
	{
		finish();
		System.exit(0);
	}
}
