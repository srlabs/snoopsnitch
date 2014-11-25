package de.srlabs.msd;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.srlabs.msd.qdmon.MsdServiceCallback;
import de.srlabs.msd.qdmon.StateChangedReason;
import de.srlabs.msd.util.MSDServiceHelperCreator;

public class BaseActivity extends FragmentActivity implements MsdServiceCallback
{	
	// Attributes
	protected MSDServiceHelperCreator msdServiceHelperCreator;
	protected TextView messageText;
	protected View messageLayout;
	protected Toast messageToast;
	protected Menu menu;
	protected Boolean isInForeground = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = getLayoutInflater();
		messageLayout = inflater.inflate(R.layout.custom_message_popdown,
                (ViewGroup) findViewById(R.id.toast_layout_root));	
		messageText = (TextView) messageLayout.findViewById(R.id.text);
		messageToast = new Toast(getApplicationContext());
		
		// Set title/subtitle of the action bar...
		ActionBar ab = getActionBar();
		ab.setTitle(R.string.actionBar_title);
		ab.setSubtitle(R.string.actionBar_subTitle);
		
		// Get MsdService Helper
		msdServiceHelperCreator = MSDServiceHelperCreator.getInstance(this.getApplicationContext(), this);
	}
	
	@Override
	protected void onResume() 
	{	
		isInForeground = true;
		super.onResume();
	}
	
	@Override
	protected void onPause() 
	{
		isInForeground = false;
		super.onPause();
	}
	
	@Override
	protected void onDestroy() 
	{	
		//msdServiceHelperCreator.destroy();
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

	@Override
	public void internalError(String errorMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stateChanged(StateChangedReason reason) 
	{
		Log.e("msd","REASON: " + reason.name());
		if (menu != null)
		{
			if (msdServiceHelperCreator.getMsdServiceHelper().isRecording())
			{
				menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_record_disable));
				//showMessage("Recording started...");
			}
			else
			{
				menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_menu_notrecord_disable));
				//showMessage("Recording stopped...");
			}	
		}
	}
}
