package de.srlabs.snoopsnitch.views;

import java.text.SimpleDateFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.srlabs.msd.R;
import de.srlabs.snoopsnitch.util.TimeSpace;

public class ChartSlidePageFragment extends Fragment 
{
	// Attributes
	private int position;
	public ChartSlidePageFragment(int position, int threatType) 
	{
		this.position = position;
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) 
    {
    	ViewGroup rootView = null;
    	
    	switch (position) {
		case 0:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_month, container, false);
			break;
		case 1:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_week, container, false);
			break;
		case 2:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_day, container, false);
			break;
		case 3:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_hour, container, false);
			break;
		default:
			rootView = (ViewGroup) inflater.inflate(
	                R.layout.fragment_chart_slide_page_month, container, false);
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
    	
    	if (position == 3)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartHour);
        	s = new SimpleDateFormat("HH:mm");
        	c = TimeSpace.Times.Hour.getEndTime();

        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
        		// Last day is divided into 12 parts, i.e. 5 minutes
        		// Substract 5 minutes (in millis)
        		c -= 5 * 60 * 1000L;
    			
        		if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c));
    			}
    		}
    	}
    	else if (position == 2)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartDay);
        	s = new SimpleDateFormat("HH:mm");
        	c = TimeSpace.Times.Day.getEndTime();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
        		// Last day is divided into 6 parts, i.e. 4 hours
        		// Subtract 4 hours (in millis)
        		c -= 4 * 60 * 60 * 1000L;

    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c));
    			}
    		}
    	}
    	else if (position == 1)
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

    			// Last 7 days divided into 7 parts, i.e. one day
    			// Subtract 1 day in millis
    			c -= 1 * 24 * 60 * 60 * 1000L;

    		}
    	}
    	else if (position == 0)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartMonth);
        	s = new SimpleDateFormat("dd.MM");
        	c = TimeSpace.Times.Month.getEndTime();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c - 6 * 24 * 60 * 60 * 1000L) + " - " + s.format(c));
    			}

    			// Last 4 weeks divided into parts of 7 days
    			c -= 7 * 24 * 60 * 60 * 1000L;
    		}
    	}
    }
}