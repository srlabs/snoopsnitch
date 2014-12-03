package de.srlabs.msd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;

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
			_items = MSDServiceHelperCreator.getInstance().getThreatsSmsWeek();
		}
		else
		{
			_items = MSDServiceHelperCreator.getInstance().getThreatsImsiWeek();
		}
		
		drawChartColumn((getMeasuredWidth() / 14) - (_rectWidth / 2), (getMeasuredWidth() / 14) + (_rectWidth / 2), _rectWidth, _items[6], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 3 - (_rectWidth / 2) + getPxFromDp(1), (getMeasuredWidth() / 14) * 3 + (_rectWidth / 2) + getPxFromDp(1), _rectWidth, _items[5], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 5 - (_rectWidth / 2) + getPxFromDp(2), (getMeasuredWidth() / 14) * 5 + (_rectWidth / 2) + getPxFromDp(2), _rectWidth, _items[4], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 7 - (_rectWidth / 2) + getPxFromDp(3), (getMeasuredWidth() / 14) * 7 + (_rectWidth / 2) + getPxFromDp(3), _rectWidth, _items[3], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 9 - (_rectWidth / 2) + getPxFromDp(4), (getMeasuredWidth() / 14) * 9 + (_rectWidth / 2) + getPxFromDp(4), _rectWidth, _items[2], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 11 - (_rectWidth / 2) + getPxFromDp(5), (getMeasuredWidth() / 14) * 11 + (_rectWidth / 2) + getPxFromDp(5), _rectWidth, _items[1], canvas);
		drawChartColumn((getMeasuredWidth() / 14) * 13 - (_rectWidth / 2) + getPxFromDp(6), (getMeasuredWidth() / 14) * 13 + (_rectWidth / 2) + getPxFromDp(6), _rectWidth, _items[0], canvas);
	}
}
