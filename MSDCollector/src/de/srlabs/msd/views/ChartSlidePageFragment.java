package de.srlabs.msd.views;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.spec.PSource;

import de.srlabs.msd.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    	Calendar c = Calendar.getInstance();
    	SimpleDateFormat s;
    	LinearLayout layout;
    	
    	if (position == 0)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartHour);
        	s = new SimpleDateFormat("HH:mm");
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c.getTime()));
    			}
    			
        		c.add(Calendar.MINUTE, -5);
    		}
    	}
    	else if (position == 1)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartDay);
        	s = new SimpleDateFormat("HH:mm");
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c.getTime()));
    			}
    			
        		c.add(Calendar.HOUR, -4);
    		}
    	}
    	else if (position == 2)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartWeek);
        	s = new SimpleDateFormat("dd.MM");
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(c.getTime()));
    			}
    			
        		c.add(Calendar.DATE, -1);
    		}
    	}
    	else if (position == 3)
    	{
    		layout = (LinearLayout)view.findViewById(R.id.llTimeLineChartMonth);
        	s = new SimpleDateFormat("dd.MM");
    		Calendar cc = Calendar.getInstance();
        	
        	for (int i=layout.getChildCount()-1; i>=0; i--)
        	{
        		View v = layout.getChildAt(i);
        		cc.add(Calendar.DATE, -6);
        		
    			if (v instanceof LinearLayout)
    			{
    				((TextView)((LinearLayout) v).getChildAt(1)).setText(s.format(cc.getTime()) + " - " + s.format(c.getTime()));
    			}
    			
    			cc.add(Calendar.DATE, -1);
        		c.add(Calendar.DATE, -7);
    		}
    	}
    }
}