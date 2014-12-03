package de.srlabs.msd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;

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
			_items = MSDServiceHelperCreator.getInstance().getThreatsSmsHour();
		}
		else
		{
			_items = MSDServiceHelperCreator.getInstance().getThreatsImsiHour();
		}
		
//		drawChartColumn((getMeasuredWidth() / 24) - (_rectWidth / 2), (getMeasuredWidth() / 24) + (_rectWidth / 2), _rectWidth, _items[11], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 3 - (_rectWidth / 2) + getDpFromPx(1), (getMeasuredWidth() / 24) * 3 + (_rectWidth / 2) + getDpFromPx(1), _rectWidth, _items[10], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 5 - (_rectWidth / 2) + getDpFromPx(2), (getMeasuredWidth() / 24) * 5 + (_rectWidth / 2) + getDpFromPx(2), _rectWidth, _items[9], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 7 - (_rectWidth / 2) + getDpFromPx(3), (getMeasuredWidth() / 24) * 7 + (_rectWidth / 2) + getDpFromPx(3), _rectWidth, _items[8], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 9 - (_rectWidth / 2) + getDpFromPx(4), (getMeasuredWidth() / 24) * 9 + (_rectWidth / 2) + getDpFromPx(4), _rectWidth, _items[7], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 11 - (_rectWidth / 2) + getDpFromPx(5), (getMeasuredWidth() / 24) * 11 + (_rectWidth / 2) + getDpFromPx(5), _rectWidth, _items[6], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 13 - (_rectWidth / 2) + getDpFromPx(6), (getMeasuredWidth() / 24) * 13 + (_rectWidth / 2) + getDpFromPx(6), _rectWidth, _items[5], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 15 - (_rectWidth / 2) + getDpFromPx(7), (getMeasuredWidth() / 24) * 15 + (_rectWidth / 2) + getDpFromPx(7), _rectWidth, _items[4], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 17 - (_rectWidth / 2) + getDpFromPx(8), (getMeasuredWidth() / 24) * 17 + (_rectWidth / 2) + getDpFromPx(8), _rectWidth, _items[3], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 19 - (_rectWidth / 2) + getDpFromPx(9), (getMeasuredWidth() / 24) * 19 + (_rectWidth / 2) + getDpFromPx(9), _rectWidth, _items[2], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 21 - (_rectWidth / 2) + getDpFromPx(10), (getMeasuredWidth() / 24) * 21 + (_rectWidth / 2) + getDpFromPx(10), _rectWidth, _items[1], canvas);
//		drawChartColumn((getMeasuredWidth() / 24) * 23 - (_rectWidth / 2) + getDpFromPx(11), (getMeasuredWidth() / 24) * 23 + (_rectWidth / 2) + getDpFromPx(11), _rectWidth, _items[0], canvas);	
		
		int width = getMeasuredWidth();
		
		drawChartColumn((getMeasuredWidth() / 24) - (_rectWidth / 2), (getMeasuredWidth() / 24) + (_rectWidth / 2), _rectWidth, _items[11], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 3 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 3 + (_rectWidth / 2), _rectWidth, _items[10], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 5 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 5 + (_rectWidth / 2), _rectWidth, _items[9], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 7 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 7 + (_rectWidth / 2), _rectWidth, _items[8], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 9 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 9 + (_rectWidth / 2), _rectWidth, _items[7], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 11 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 11 + (_rectWidth / 2), _rectWidth, _items[6], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 13 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 13 + (_rectWidth / 2), _rectWidth, _items[5], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 15 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 15 + (_rectWidth / 2), _rectWidth, _items[4], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 17 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 17 + (_rectWidth / 2), _rectWidth, _items[3], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 19 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 19 + (_rectWidth / 2), _rectWidth, _items[2], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 21 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 21 + (_rectWidth / 2), _rectWidth, _items[1], canvas);
		drawChartColumn((getMeasuredWidth() / 24) * 23 - (_rectWidth / 2), (getMeasuredWidth() / 24) * 23 + (_rectWidth / 2), _rectWidth, _items[0], canvas);
	}
}
