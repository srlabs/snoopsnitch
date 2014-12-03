package de.srlabs.msd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;

public class DetailThreatChartMonth extends DetailThreatChart 
{
	public DetailThreatChartMonth(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
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
		
		drawChartColumn((getMeasuredWidth() / 8) - (_rectWidth / 2), (getMeasuredWidth() / 8) + (_rectWidth / 2), _rectWidth, _items[3], canvas);
		drawChartColumn((getMeasuredWidth() / 8) * 3 - (_rectWidth / 2) + 1, (getMeasuredWidth() / 8) * 3 + (_rectWidth / 2) + 1, _rectWidth, _items[2], canvas);
		drawChartColumn((getMeasuredWidth() / 8) * 5 - (_rectWidth / 2) + 2, (getMeasuredWidth() / 8) * 5 + (_rectWidth / 2) + 2, _rectWidth, _items[1], canvas);
		drawChartColumn((getMeasuredWidth() / 8) * 7 - (_rectWidth / 2) + 3, (getMeasuredWidth() / 8) * 7 + (_rectWidth / 2) + 3, _rectWidth, _items[0], canvas);
	}
}
