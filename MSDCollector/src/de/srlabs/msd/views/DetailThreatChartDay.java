package de.srlabs.msd.views;

import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;
import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class DetailThreatChartDay extends DetailThreatChart 
{
	public DetailThreatChartDay(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);

		if (_threatType == R.id.SilentSMSCharts)
		{
			_items = MSDServiceHelperCreator.getInstance(null).getThreatsSmsDay();
		}
		else
		{
			_items = MSDServiceHelperCreator.getInstance(null).getThreatsImsiDay();
		}
		
		drawChartColumn((getMeasuredWidth() / 12) - (_rectWidth / 2), (getMeasuredWidth() / 12) + (_rectWidth / 2), _rectWidth, _items[5], canvas);
		drawChartColumn((getMeasuredWidth() / 12) * 3 - (_rectWidth / 2) + 1, (getMeasuredWidth() / 12) * 3 + (_rectWidth / 2) + 1, _rectWidth, _items[4], canvas);
		drawChartColumn((getMeasuredWidth() / 12) * 5 - (_rectWidth / 2) + 2, (getMeasuredWidth() / 12) * 5 + (_rectWidth / 2) + 2, _rectWidth, _items[3], canvas);
		drawChartColumn((getMeasuredWidth() / 12) * 7 - (_rectWidth / 2) + 3, (getMeasuredWidth() / 12) * 7 + (_rectWidth / 2) + 3, _rectWidth, _items[2], canvas);
		drawChartColumn((getMeasuredWidth() / 12) * 9 - (_rectWidth / 2) + 4, (getMeasuredWidth() / 12) * 9 + (_rectWidth / 2) + 4, _rectWidth, _items[1], canvas);
		drawChartColumn((getMeasuredWidth() / 12) * 11 - (_rectWidth / 2) + 5, (getMeasuredWidth() / 12) * 11 + (_rectWidth / 2) + 5, _rectWidth, _items[0], canvas);
	}
}
