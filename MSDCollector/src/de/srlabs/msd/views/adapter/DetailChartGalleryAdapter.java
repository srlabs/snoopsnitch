package de.srlabs.msd.views.adapter;

import de.srlabs.msd.R;
import de.srlabs.msd.views.ChartSlidePageFragment;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class DetailChartGalleryAdapter extends FragmentStatePagerAdapter
{
	private static final int NUM_PAGES = 4;
	private Context _context;
	private int _threatType;
	
	public DetailChartGalleryAdapter(FragmentManager fm, Context context, int threatType) 
	{
        super(fm);
        _context = context;
        _threatType = threatType;
    }

	@Override
    public Fragment getItem(int position) 
	{
		return new ChartSlidePageFragment(position, _threatType);
    }
	
	@Override
	public int getItemPosition(Object object) {
	    // POSITION_NONE makes it possible to reload the PagerAdapter
	    return POSITION_NONE;
	}

    @Override
    public int getCount() 
    {
        return NUM_PAGES;
    }
    
    @Override
    public CharSequence getPageTitle(int position) 
    {
    	switch (position) {
		case 0:
			return _context.getResources().getString(R.string.detailChart_title_hour);
		case 1:
			return _context.getResources().getString(R.string.detailChart_title_day);
		case 2:
			return _context.getResources().getString(R.string.detailChart_title_week);
		case 3:
			return _context.getResources().getString(R.string.detailChart_title_month);
		default:
			return "";
    	}
    }
    
    public int getThreatType ()
    {
    	return _threatType;
    }
}
