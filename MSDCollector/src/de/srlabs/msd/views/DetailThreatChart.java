package de.srlabs.msd.views;

import de.srlabs.msd.DetailChartActivity;
import de.srlabs.msd.R;
import de.srlabs.msd.util.MSDServiceHelperCreator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

public class DetailThreatChart extends View
{
	int _timePeriod;
	int _threatType;
	int _amount;	
	int _rectWidth;
	int _color;
	int[] _items;
	private DetailChartActivity _activity;
	ImageView _imgThreatType;
	private Context context;
	
	public DetailThreatChart(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		_threatType = ((DetailChartActivity) context).getThreatType();
		_items = null;
		
		if (_threatType == R.id.SilentSMSCharts)
		{
			_color = getResources().getColor(R.color.common_chartYellow);
			//_imgThreatType.setBackgroundResource(R.drawable.ic_content_sms_event);
		}
		else
		{
			_color = getResources().getColor(R.color.common_chartRed);
			//_imgThreatType.setBackgroundResource(R.drawable.ic_content_sms_event);
		}
		
		this._rectWidth = MSDServiceHelperCreator.getInstance(null).getRectWidth();
		this.context = context;
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
	}

	protected void drawChartColumn (int startX, int endX, int elementWidth, int numberOfElements, Canvas canvas)
	{
		Paint myPaint = new Paint();
		myPaint.setStyle(Paint.Style.FILL);
		myPaint.setColor(_color);
		
		int left = startX;
		int top = this.getMeasuredHeight() - (elementWidth) - 10;
		int right = endX;
		int bottom = this.getMeasuredHeight()-10;
		
		int _columnSpace = 2;
		
		if (numberOfElements == 0)
		{
			myPaint.setColor(getResources().getColor(R.color.common_chartGreen));
			canvas.drawRect(left, top+(elementWidth*0.75f), right, bottom, myPaint);
		}
		else
		{
			for (int i=1; i<=numberOfElements; i++)
			{
				if (i<6)
				{
					canvas.drawRect(left, top, right, bottom, myPaint);
					top -= (elementWidth + _columnSpace);
					bottom -= (elementWidth + _columnSpace);
				}
				else if (i==6 && numberOfElements>6)
				{
					canvas.drawRect(left, top+(_rectWidth * 0.85f), right, bottom, myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.5875f), right, bottom-(_rectWidth * 0.2625f), myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.33f), right, bottom-(_rectWidth * 0.525f), myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.063f), right, bottom-(_rectWidth * 0.783f), myPaint);
				}
				else if (i==6 && numberOfElements<=6)
				{
					canvas.drawRect(left, top, right, bottom, myPaint);
					top -= left*2.1f;
					bottom -= left*2.1f;
				}
			}
		}
	}
	
	protected int getPxFromDp (int dp)
	{
		return (int)((dp * context.getResources().getDisplayMetrics().density) + 0.5);
	}
}
