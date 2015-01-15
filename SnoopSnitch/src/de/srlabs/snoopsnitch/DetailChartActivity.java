package de.srlabs.snoopsnitch;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.analysis.Event;
import de.srlabs.snoopsnitch.analysis.Event.Type;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;
import de.srlabs.snoopsnitch.util.TimeSpace;
import de.srlabs.snoopsnitch.views.adapter.DetailChartGalleryAdapter;
import de.srlabs.snoopsnitch.views.adapter.ListViewEventAdapter;
import de.srlabs.snoopsnitch.views.adapter.ListViewImsiCatcherAdapter;

public class DetailChartActivity extends BaseActivity
{
	// Attributes
	private Spinner spinner;
	private ListView listView;
	private ImageView _imgThreatType;
	private TextView _txtThreatTypeImsiCatcherCount;
	private TextView _txtThreatTypeSilentSmsCount;
	private LinearLayout _llThreatTypeImsiCatcher;
	private LinearLayout _llThreatTypeSms;
	private LinearLayout _llSpinnerDetailChart;
	private DetailChartGalleryAdapter mPagerAdapter;
	private ViewPager mPager;
	private int _threatType;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chart_detail);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		_imgThreatType = (ImageView) findViewById(R.id.imgDetailChartThreatType);
		_txtThreatTypeImsiCatcherCount = (TextView) findViewById(R.id.txtDetailChartThreatTypeImsiCatcherCount);
		_txtThreatTypeSilentSmsCount = (TextView) findViewById(R.id.txtDetailChartThreatTypeSilentSmsCount);
		_llThreatTypeImsiCatcher = (LinearLayout) findViewById(R.id.llThreatTypeImsiCatcher);
		_llThreatTypeSms = (LinearLayout) findViewById(R.id.llThreatTypeSms);
		_llSpinnerDetailChart = (LinearLayout) findViewById(R.id.llSpinnerDetailChart);
		
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
				spinner.setSelection(0);
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
						((ListViewEventAdapter) listView.getAdapter()).getFilter().filter("ALL");
						break;
					case 1:
						((ListViewEventAdapter) listView.getAdapter()).getFilter().filter(Event.Type.BINARY_SMS.toString());
						break;
					case 2:
						((ListViewEventAdapter) listView.getAdapter()).getFilter().filter(Event.Type.SILENT_SMS.toString());
						break;
					case 3:
						((ListViewEventAdapter) listView.getAdapter()).getFilter().filter(Event.Type.NULL_PAGING.toString());
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
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		// Start pager adapter with hour fragment
		this.mPager.setCurrentItem(3);
		
		resetListView();
	}
	
	private void configureSpinner (int id)
	{
		if (id == R.id.SilentSMSCharts)
		{			
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.event_types));
			    spinner.setAdapter(spinnerAdapter);
		}
	}
	
	private void fillList (int id, int position)
	{
		long _startTime;
		long _endTime;
		
		switch (position) {
		case 3:
			_startTime = TimeSpace.Times.Hour.getStartTime();
			_endTime = TimeSpace.Times.Hour.getEndTime();
			break;
		case 2:
			_startTime = TimeSpace.Times.Day.getStartTime();
			_endTime = TimeSpace.Times.Day.getEndTime();
			break;
		case 1:
			_startTime = TimeSpace.Times.Week.getStartTime();
			_endTime = TimeSpace.Times.Week.getEndTime();
			break;
		case 0:
			_startTime = TimeSpace.Times.Month.getStartTime();
			_endTime = TimeSpace.Times.Month.getEndTime();
			break;
		default:
			_startTime = 0;
			_endTime = 0;
			break;
		}
		
		if (id == R.id.SilentSMSCharts)
		{			
			ListViewEventAdapter listViewAdapter = new ListViewEventAdapter(this, 
					getMsdServiceHelperCreator().getMsdServiceHelper().getData().getEvent(_startTime, _endTime));
			listView.setAdapter(listViewAdapter);
			
			if (_txtThreatTypeSilentSmsCount != null)
			{
				_txtThreatTypeSilentSmsCount.setText(String.valueOf(getMsdServiceHelperCreator().
						getEventOfType(Type.INVALID_EVENT, _startTime, _endTime).size()));
			}
		}
		else
		{
			ListViewImsiCatcherAdapter listViewAdapter = new ListViewImsiCatcherAdapter (this, 
					getMsdServiceHelperCreator().getMsdServiceHelper().getData().getImsiCatchers (_startTime, _endTime));
			listView.setAdapter(listViewAdapter);
			
			if (_txtThreatTypeImsiCatcherCount != null)
			{
				_txtThreatTypeImsiCatcherCount.setText(String.valueOf(getMsdServiceHelperCreator().
						getMsdServiceHelper().getData().getImsiCatchers(_startTime, _endTime).size()));
			}
		}
		

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
			_llThreatTypeImsiCatcher.setVisibility(View.VISIBLE);
			_llThreatTypeSms.setVisibility(View.GONE);
		}
		else
		{
			_imgThreatType.setBackground(getResources().getDrawable(R.drawable.ic_content_sms_event));
			_llThreatTypeSms.setVisibility(View.VISIBLE);
			_llThreatTypeImsiCatcher.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void stateChanged(StateChangedReason reason) 
	{
		super.stateChanged(reason);
		
		if (reason.equals(StateChangedReason.CATCHER_DETECTED) || reason.equals(StateChangedReason.SMS_DETECTED))
		{
			resetListView();
			
		}
	}
	
	@Override
	public void refreshView() 
	{
		mPager.getAdapter().notifyDataSetChanged();
	}
	
	private void resetListView ()
	{		
		fillList(_threatType, mPager.getCurrentItem());
		mPager.getAdapter().notifyDataSetChanged();
	}
}
