package de.srlabs.msd;

import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.util.TimeSpace;
import de.srlabs.msd.views.adapter.DetailChartGalleryAdapter;
import de.srlabs.msd.views.adapter.ListViewImsiCatcherAdapter;
import de.srlabs.msd.views.adapter.ListViewSmsAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class DetailChartActivity extends BaseActivity
{
	// Attributes
	private Spinner spinner;
	private ListView listView;
	private ImageView _imgThreatType;
	private TextView _txtThreatType;
	DetailChartGalleryAdapter mPagerAdapter;
	ViewPager mPager;
	int _threatType;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chart_detail);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		_imgThreatType = (ImageView) findViewById(R.id.imgDetailChartThreatType);
		_txtThreatType = (TextView) findViewById(R.id.txtDetailChartThreatType);
		
		_threatType = getIntent().getIntExtra("ThreatType", R.id.IMSICatcherCharts);
		
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.vpDetailCharts);
        mPagerAdapter = new DetailChartGalleryAdapter(getSupportFragmentManager(), this, 
        		getIntent().getIntExtra("ThreatType", R.id.IMSICatcherCharts));
        mPager.setAdapter(mPagerAdapter);
        
        setThreatTypeImageText();
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() 
        {
			@Override
			public void onPageSelected(int position) 
			{
				fillList(_threatType, position);
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
		});
        
		spinner = (Spinner) findViewById(R.id.spnDetailChart);
		listView = (ListView) findViewById(R.id.lstDetailChart);
		
		configureSpinner(getIntent().getIntExtra("ThreatType", R.id.IMSICatcherCharts));
		fillList(getIntent().getIntExtra("ThreatType", R.id.IMSICatcherCharts), mPager.getCurrentItem());
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() 
		{

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) 
			{
				if (_threatType != R.id.IMSICatcherCharts)
				{
					switch (position) {
					case 0:
						((ListViewSmsAdapter) listView.getAdapter()).getFilter().filter("ALL");
						break;
					case 1:
						((ListViewSmsAdapter) listView.getAdapter()).getFilter().filter(SMS.Type.BINARY_SMS.toString());
						break;
					case 2:
						((ListViewSmsAdapter) listView.getAdapter()).getFilter().filter(SMS.Type.SILENT_SMS.toString());
						break;
					default:
						break;
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) 
			{
				
			}
			
		});
	}
	
	private void configureSpinner (int id)
	{
		if (id == R.id.IMSICatcherCharts)
		{
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.imsi_types));
		    spinner.setAdapter(spinnerAdapter);
		    
			spinner.setVisibility(View.INVISIBLE);
			spinner.setEnabled(false);
		}
		else if (id == R.id.SilentSMSCharts)
		{			
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.sms_types));
			    spinner.setAdapter(spinnerAdapter);
		}
	}
	
	private void fillList (int id, int position)
	{
		long _startTime;
		long _endTime;
		
		switch (position) {
		case 0:
			_startTime = TimeSpace.Times.Hour.getStartTime();
			_endTime = TimeSpace.Times.Hour.getEndTime();
			break;
		case 1:
			_startTime = TimeSpace.Times.Day.getStartTime();
			_endTime = TimeSpace.Times.Day.getEndTime();
			break;
		case 2:
			_startTime = TimeSpace.Times.Week.getStartTime();
			_endTime = TimeSpace.Times.Week.getEndTime();
			break;
		case 3:
			_startTime = TimeSpace.Times.Month.getStartTime();
			_endTime = TimeSpace.Times.Month.getEndTime();
			break;
		default:
			_startTime = TimeSpace.Times.Month.getStartTime();
			_endTime = TimeSpace.Times.Month.getEndTime();
			break;
		}
		
		if (id == R.id.SilentSMSCharts)
		{			
			ListViewSmsAdapter listViewAdapter = new ListViewSmsAdapter(this, 
					getMsdServiceHelperCreator().getMsdServiceHelper().getData().getSMS(_startTime, _endTime));
			listView.setAdapter(listViewAdapter);
		}
		else
		{
			ListViewImsiCatcherAdapter listViewAdapter = new ListViewImsiCatcherAdapter (this, 
					getMsdServiceHelperCreator().getMsdServiceHelper().getData().getImsiCatchers (_startTime, _endTime));
			listView.setAdapter(listViewAdapter);
		}
	}
	
	public void contributeData (View view)
	{
		Toast.makeText(this, view.toString(), 2).show();
	}
	
	public int getThreatType ()
	{
		return _threatType;
	}
	
	private void setThreatTypeImageText ()
	{
		if (_threatType == R.id.IMSICatcherCharts)
		{
			_imgThreatType.setBackground(getResources().getDrawable(R.drawable.ic_content_imsi_event));
			_txtThreatType.setText("IMSI Catcher");
		}
		else
		{
			_imgThreatType.setBackground(getResources().getDrawable(R.drawable.ic_content_sms_event));
			_txtThreatType.setText("X Silent SMS | Y Binary SMS");
		}
	}
}
