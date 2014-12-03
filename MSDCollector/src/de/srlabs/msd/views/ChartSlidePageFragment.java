package de.srlabs.msd.views;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.srlabs.msd.R;
import de.srlabs.msd.util.TimeSpace;

public class ChartSlidePageFragment extends Fragment 
{
	// Attributes
	private int position;
	private int _threatType;
	
	public ChartSlidePageFragment(int position, int threatType) 
	{
		this.position = position;
		this._threatType = threatType;
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) 
    {
    	ViewGroup rootView = null;
    	
    	switch (position) {
		case 0:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_hour, container, false);
			break;
		case 1:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_day, container, false);
			break;
		case 2:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_week, container, false);
			break;
		case 3:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_month, container, false);
			break;
		default:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_hour, container, false);
			break;
		}
    	
    	setTimeLegend (rootView);
       
        return rootView;
    }
    
    private void setTimeLegend (View view)
    {
    	long c;
    	SimpleDateFormat s;
    	LinearLayout layout;
    	
    	if (position == 0)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartHour);
        	s = new SimpleDateFormat("HH:mm");
        	c = TimeSpace.Times.Hour.getEndTime();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c));
    			}
    			
        		c -= 300000;
    		}
    	}
    	else if (position == 1)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartDay);
        	s = new SimpleDateFormat("HH:mm");
        	c = TimeSpace.Times.Day.getEndTime();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c));
    			}
    			
        		c -= 14400000;
    		}
    	}
    	else if (position == 2)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartWeek);
        	s = new SimpleDateFormat("dd.MM");
        	c = TimeSpace.Times.Week.getEndTime();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c));
    			}
    			
        		c -= 86400000;
    		}
    	}
    	else if (position == 3)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartMonth);
        	s = new SimpleDateFormat("dd.MM");
        	c = TimeSpace.Times.Month.getEndTime();
    		long cc = TimeSpace.Times.Month.getEndTime();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		cc -= 518400000;
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(cc) + " - " + s.format(c));
    			}
    			
    			cc -= 86400000;
        		c -= 604800000;
    		}
    	}
    }
}