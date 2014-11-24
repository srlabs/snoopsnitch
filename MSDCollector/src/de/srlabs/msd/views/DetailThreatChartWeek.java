package de.srlabs.msd.views;

import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class DetailThreatChartWeek extends DetailThreatChart 
{	
	public DetailThreatChartWeek(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);

		if (_threatType == R.id.SilentSMSCharts)
		{
			_items = MSDServiceHelperCreator.getInstance(null).getThreatsSmsWeek();
		}
		else
		{
			_items = MSDServiceHelperCreator.getInstance(null).getThreatsImsiWeek();
		}
		
		drawChartColumn((getMeasuredWidth() / 14) - (_rectWidth / 2), (getMeasuredWidth() / 14) + (_rectWidth / 2), _rectWidth, _items[6], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 3 - (_rectWidth / 2) + 1, (getMeasuredWidth() / 14) * 3 + (_rectWidth / 2) + 1, _rectWidth, _items[5], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 5 - (_rectWidth / 2) + 2, (getMeasuredWidth() / 14) * 5 + (_rectWidth / 2) + 2, _rectWidth, _items[4], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 7 - (_rectWidth / 2) + 3, (getMeasuredWidth() / 14) * 7 + (_rectWidth / 2) + 3, _rectWidth, _items[3], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 9 - (_rectWidth / 2) + 4, (getMeasuredWidth() / 14) * 9 + (_rectWidth / 2) + 4, _rectWidth, _items[2], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 11 - (_rectWidth / 2) + 5, (getMeasuredWidth() / 14) * 11 + (_rectWidth / 2) + 5, _rectWidth, _items[1], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 13 - (_rectWidth / 2) + 6, (getMeasuredWidth() / 14) * 13 + (_rectWidth / 2) + 6, _rectWidth, _items[0], canvas);
	}
}
