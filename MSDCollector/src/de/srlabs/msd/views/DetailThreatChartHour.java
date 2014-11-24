package de.srlabs.msd.views;

import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class DetailThreatChartHour extends DetailThreatChart 
{
	public DetailThreatChartHour(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);
		
		if (_threatType == R.id.SilentSMSCharts)
		{
			_items = MSDServiceHelperCreator.getInstance(null).getThreatsSmsHour();
		}
		else
		{
			_items = MSDServiceHelperCreator.getInstance(null).getThreatsImsiHour();
		}
		
		drawChartColumn((getMeasuredWidth() / 24) - (_rectWidth / 2) + 1, (getMeasuredWidth() / 24) + (_rectWidth / 2), _rectWidth, _items[11], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 3 - (_rectWidth / 2) + 1, (getMeasuredWidth() / 24) * 3 + (_rectWidth / 2) + 1, _rectWidth, _items[10], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 5 - (_rectWidth / 2) + 2, (getMeasuredWidth() / 24) * 5 + (_rectWidth / 2) + 2, _rectWidth, _items[9], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 7 - (_rectWidth / 2) + 3, (getMeasuredWidth() / 24) * 7 + (_rectWidth / 2) + 3, _rectWidth, _items[8], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 9 - (_rectWidth / 2) + 4, (getMeasuredWidth() / 24) * 9 + (_rectWidth / 2) + 4, _rectWidth, _items[7], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 11 - (_rectWidth / 2) + 5, (getMeasuredWidth() / 24) * 11 + (_rectWidth / 2) + 5, _rectWidth, _items[6], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 13 - (_rectWidth / 2) + 6, (getMeasuredWidth() / 24) * 13 + (_rectWidth / 2) + 6, _rectWidth, _items[5], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 15 - (_rectWidth / 2) + 7, (getMeasuredWidth() / 24) * 15 + (_rectWidth / 2) + 7, _rectWidth, _items[4], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 17 - (_rectWidth / 2) + 8, (getMeasuredWidth() / 24) * 17 + (_rectWidth / 2) + 8, _rectWidth, _items[3], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 19 - (_rectWidth / 2) + 9, (getMeasuredWidth() / 24) * 19 + (_rectWidth / 2) + 9, _rectWidth, _items[2], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 21 - (_rectWidth / 2) + 10, (getMeasuredWidth() / 24) * 21 + (_rectWidth / 2) + 10, _rectWidth, _items[1], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 23 - (_rectWidth / 2) + 11, (getMeasuredWidth() / 24) * 23 + (_rectWidth / 2) + 11, _rectWidth, _items[0], canvas);		
	}
}
