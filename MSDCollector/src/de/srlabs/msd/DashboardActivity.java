package de.srlabs.msd;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.srlabs.msd.qdmon.StateChangedReason;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msd.util.Utils;
import de.srlabs.msd.views.DashboardThreatChart;

public class DashboardActivity extends BaseActivity 
{	
	// Attributes
	private int rectWidth;
	private DashboardThreatChart layout;
	private ViewTreeObserver vto;
	private TextView txtSmsMonthCount;
	private TextView txtSmsWeekCount;
	private TextView txtSmsDayCount;
	private TextView txtSmsHourCount;
	private TextView txtImsiMonthCount;
	private TextView txtImsiWeekCount;
	private TextView txtImsiDayCount;
	private TextView txtImsiHourCount;
	private TextView txtLastScan;
	private ImageView imgSilentSms;
	private ImageView imgImsiCatcher;

	// Methods
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		
		// Compatibility check
		if (DeviceCompatibilityChecker.checkDeviceCompatibility() != null)
		{
			new AlertDialog.Builder(this)
		    .setTitle(R.string.alert_deviceCompatibility_title)
		    .setMessage(R.string.alert_deviceCompatibility_message)
		    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() 
		    {
		        @Override
				public void onClick(DialogInterface dialog, int which) 
		        {   	
		        	android.os.Process.killProcess(android.os.Process.myPid());
		        }
		     })
		    .setIcon(android.R.drawable.ic_dialog_alert)
		     .show();
		}
		
				
		txtSmsMonthCount = (TextView) findViewById(R.id.txtDashboardSilentSmsMonthCount);
		txtSmsWeekCount = (TextView) findViewById(R.id.txtDashboardSilentSmsWeekCount);
		txtSmsDayCount = (TextView) findViewById(R.id.txtDashboardSilentSmsDayCount);
		txtSmsHourCount = (TextView) findViewById(R.id.txtDashboardSilentSmsHourCount);
		txtImsiMonthCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherMonthCount);
		txtImsiWeekCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherWeekCount);
		txtImsiDayCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherDayCount);
		txtImsiHourCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherHourCount);
		txtLastScan = (TextView) findViewById(R.id.txtDashboardLastScan);
		imgSilentSms = (ImageView) findViewById(R.id.imgDashboardSilentSms);
		imgImsiCatcher = (ImageView) findViewById(R.id.imgDashboardImsiCatcher);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		// Set text
		txtSmsMonthCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsMonthSum()));
		txtSmsWeekCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsWeekSum()));
		txtSmsDayCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsDaySum()));
		txtSmsHourCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsHourSum()));
		txtImsiMonthCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiMonthSum()));
		txtImsiWeekCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiWeekSum()));
		txtImsiDayCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiDaySum()));
		txtImsiHourCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiHourSum()));
	}
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		
		layout = (DashboardThreatChart)findViewById(R.id.SilentSMSChartWeek);
		vto = layout.getViewTreeObserver(); 
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() 
		{ 
		    @Override 
		    public void onGlobalLayout() 
		    { 
		        setRectWidth(layout.getMeasuredWidth());
		    } 
		});
	}
	
	private void setRectWidth (int columnWidth)
	{
		int _rectWidth = columnWidth / 2;
		
		LinearLayout silentSMSCharts = (LinearLayout) findViewById(R.id.SilentSMSCharts);
		int countSMSCharts = silentSMSCharts.getChildCount();
		
		for (int i=0; i<=countSMSCharts; i++) 
		{
		    View v = silentSMSCharts.getChildAt(i);
		    
		    if (v instanceof LinearLayout) 
		    {
		    	if (((LinearLayout) v).getChildAt(1) instanceof DashboardThreatChart)
		    	{
		    		((DashboardThreatChart)((LinearLayout) v).getChildAt(1)).setRectWidth(_rectWidth);		 
		    	}
		    }
		}
		
		LinearLayout imsiCatcherCharts = (LinearLayout) findViewById(R.id.IMSICatcherCharts);
		int countImsiCharts = imsiCatcherCharts.getChildCount();
		
		for (int i=0; i<=countImsiCharts; i++) 
		{
		    View v = imsiCatcherCharts.getChildAt(i);
		    
		    if (v instanceof LinearLayout) 
		    {
		    	if (((LinearLayout) v).getChildAt(1) instanceof DashboardThreatChart)
		    	{
		    		((DashboardThreatChart)((LinearLayout) v).getChildAt(1)).setRectWidth(_rectWidth);	    		
		    	}
		    }
		}
	}
	
	public void openDetailView (View view)
	{		
		if (view.equals(findViewById(R.id.SilentSMSCharts)) || view.equals(findViewById(R.id.IMSICatcherCharts)))
		{
			Intent myIntent = new Intent(this, DetailChartActivity.class);
			myIntent.putExtra("ThreatType", view.getId());
			startActivity(myIntent);
		}
	}
}
