package de.srlabs.msd.views;

import de.srlabs.msd.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        
        return rootView;
    }
}