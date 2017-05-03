package de.srlabs.snoopsnitch.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;

public class DetailThreatChartMonth extends DetailThreatChart 
{
	public DetailThreatChartMonth(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
		        attrs,
		        R.styleable.DetailThreatChartMonth,
		        0, 0);

		   try 
		   {
			   this._timeSpacePosition = a.getInteger(R.styleable.DetailThreatChartMonth_TimeSpacePositionMonth, 0);
		   } 
		   finally 
		   {
		       a.recycle();
		   }
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);

		if (_threatType == R.id.SilentSMSCharts)
		{
			_items = MSDServiceHelperCreator.getInstance().getThreatsSmsMonth();
		}
		else
		{
			_items = MSDServiceHelperCreator.getInstance().getThreatsImsiMonth();
		}

		drawChartColumn((getMeasuredWidth() / 2) - (_rectWidth / 2), (getMeasuredWidth() / 2) + (_rectWidth / 2), _rectWidth, _items[_timeSpacePosition], canvas);
	}
}
