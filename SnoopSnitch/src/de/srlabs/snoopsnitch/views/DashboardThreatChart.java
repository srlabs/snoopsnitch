package de.srlabs.snoopsnitch.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.DashboardActivity;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;

public class DashboardThreatChart extends View 
{
	// Attributes
	int _timePeriod;
	int _threatType;
	int _amount;	
	int _rectWidth;
	int _color;
	int _color_uploaded;
	private DashboardActivity _activity;
	
	public DashboardThreatChart(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		_activity = (DashboardActivity) getContext();
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
		        attrs,
		        R.styleable.DashboardThreatChart,
		        0, 0);

		   try 
		   {
			   _timePeriod = a.getInteger(R.styleable.DashboardThreatChart_timePeriodDashboard, 0);
		       _threatType = a.getInteger(R.styleable.DashboardThreatChart_typeOfThreatDashboard, 0);
		   } 
		   finally 
		   {
		       a.recycle();
		   }
		   
		   setColor();
	}
	
	@Override
	protected void onDraw(Canvas canvas) 
	{
		this._rectWidth = MSDServiceHelperCreator.getInstance().getRectWidth();
		int _rectSpace = 2;
		
		if (_threatType == 0)
		{
			if (_timePeriod == 0)
			{
				int offsetX = (this.getMeasuredWidth() - (12 * _rectWidth) - (11 * _rectSpace)) / 2;
				boolean _smsHour[][] = _activity.getMsdServiceHelperCreator().getThreatsSmsHour();
				
				drawChartColumn(offsetX, offsetX + (1 * _rectWidth), _rectWidth, _smsHour[11], canvas);
				drawChartColumn(offsetX + (1 * _rectWidth) + (1 * _rectSpace), offsetX + (2 * _rectWidth) + (1 * _rectSpace), _rectWidth, _smsHour[10], canvas);
				drawChartColumn(offsetX + (2 * _rectWidth) + (2 * _rectSpace), offsetX + (3 * _rectWidth) + (2 * _rectSpace), _rectWidth, _smsHour[9], canvas);
				drawChartColumn(offsetX + (3 * _rectWidth) + (3 * _rectSpace), offsetX + (4 * _rectWidth) + (3 * _rectSpace), _rectWidth, _smsHour[8], canvas);
				drawChartColumn(offsetX + (4 * _rectWidth) + (4 * _rectSpace), offsetX + (5 * _rectWidth) + (4 * _rectSpace), _rectWidth, _smsHour[7], canvas);
				drawChartColumn(offsetX + (5 * _rectWidth) + (5 * _rectSpace), offsetX + (6 * _rectWidth) + (5 * _rectSpace), _rectWidth, _smsHour[6], canvas);
				drawChartColumn(offsetX + (6 * _rectWidth) + (6 * _rectSpace), offsetX + (7 * _rectWidth) + (6 * _rectSpace), _rectWidth, _smsHour[5], canvas);
				drawChartColumn(offsetX + (7 * _rectWidth) + (7 * _rectSpace), offsetX + (8 * _rectWidth) + (7 * _rectSpace), _rectWidth, _smsHour[4], canvas);
				drawChartColumn(offsetX + (8 * _rectWidth) + (8 * _rectSpace), offsetX + (9 * _rectWidth) + (8 * _rectSpace), _rectWidth, _smsHour[3], canvas);
				drawChartColumn(offsetX + (9 * _rectWidth) + (9 * _rectSpace), offsetX + (10 * _rectWidth) + (9 * _rectSpace), _rectWidth, _smsHour[2], canvas);
				drawChartColumn(offsetX + (10 * _rectWidth) + (10 * _rectSpace), offsetX + (11 * _rectWidth) + (10 * _rectSpace), _rectWidth, _smsHour[1], canvas);
				drawChartColumn(offsetX + (11 * _rectWidth) + (11 * _rectSpace), offsetX + (12 * _rectWidth) + (11 * _rectSpace), _rectWidth, _smsHour[0], canvas);
			}
			else if (_timePeriod == 1)
			{
				int offsetX = (this.getMeasuredWidth() - (6 * _rectWidth) - (5 * _rectSpace)) / 2;
				boolean _smsDay[][] = _activity.getMsdServiceHelperCreator().getThreatsSmsDay();
				
				drawChartColumn(offsetX, offsetX + (1 * _rectWidth), _rectWidth, _smsDay[5], canvas);
				drawChartColumn(offsetX + (1 * _rectWidth) + (1 * _rectSpace), offsetX + (2 * _rectWidth) + (1 * _rectSpace), _rectWidth, _smsDay[4], canvas);
				drawChartColumn(offsetX + (2 * _rectWidth) + (2 * _rectSpace), offsetX + (3 * _rectWidth) + (2 * _rectSpace), _rectWidth, _smsDay[3], canvas);
				drawChartColumn(offsetX + (3 * _rectWidth) + (3 * _rectSpace), offsetX + (4 * _rectWidth) + (3 * _rectSpace), _rectWidth, _smsDay[2], canvas);
				drawChartColumn(offsetX + (4 * _rectWidth) + (4 * _rectSpace), offsetX + (5 * _rectWidth) + (4 * _rectSpace), _rectWidth, _smsDay[1], canvas);
				drawChartColumn(offsetX + (5 * _rectWidth) + (5 * _rectSpace), offsetX + (6 * _rectWidth) + (5 * _rectSpace), _rectWidth, _smsDay[0], canvas);
			}
			else if (_timePeriod == 2)
			{
				drawChartColumn(this.getMeasuredWidth() / 4, this.getMeasuredWidth()-(getMeasuredWidth() / 4), 
						_rectWidth, _activity.getMsdServiceHelperCreator().getThreatsSmsWeekSum(), canvas);				
			}
			else if (_timePeriod == 3)
			{
				drawChartColumn(this.getMeasuredWidth() / 4, this.getMeasuredWidth()-(getMeasuredWidth() / 4), 
						_rectWidth, _activity.getMsdServiceHelperCreator().getThreatsSmsMonthSum(), canvas);	
			}	
		}
		else if (_threatType == 1)
		{
			if (_timePeriod == 0)
			{
				int offsetX = (this.getMeasuredWidth() - (12 * _rectWidth) - (11 * _rectSpace)) / 2;
				boolean _imsiHour[][] = _activity.getMsdServiceHelperCreator().getThreatsImsiHour();
				
				drawChartColumn(offsetX, offsetX + (1 * _rectWidth), _rectWidth, _imsiHour[11], canvas);
				drawChartColumn(offsetX + (1 * _rectWidth) + (1 * _rectSpace), offsetX + (2 * _rectWidth) + (1 * _rectSpace), _rectWidth, _imsiHour[10], canvas);
				drawChartColumn(offsetX + (2 * _rectWidth) + (2 * _rectSpace), offsetX + (3 * _rectWidth) + (2 * _rectSpace), _rectWidth, _imsiHour[9], canvas);
				drawChartColumn(offsetX + (3 * _rectWidth) + (3 * _rectSpace), offsetX + (4 * _rectWidth) + (3 * _rectSpace), _rectWidth, _imsiHour[8], canvas);
				drawChartColumn(offsetX + (4 * _rectWidth) + (4 * _rectSpace), offsetX + (5 * _rectWidth) + (4 * _rectSpace), _rectWidth, _imsiHour[7], canvas);
				drawChartColumn(offsetX + (5 * _rectWidth) + (5 * _rectSpace), offsetX + (6 * _rectWidth) + (5 * _rectSpace), _rectWidth, _imsiHour[6], canvas);
				drawChartColumn(offsetX + (6 * _rectWidth) + (6 * _rectSpace), offsetX + (7 * _rectWidth) + (6 * _rectSpace), _rectWidth, _imsiHour[5], canvas);
				drawChartColumn(offsetX + (7 * _rectWidth) + (7 * _rectSpace), offsetX + (8 * _rectWidth) + (7 * _rectSpace), _rectWidth, _imsiHour[4], canvas);
				drawChartColumn(offsetX + (8 * _rectWidth) + (8 * _rectSpace), offsetX + (9 * _rectWidth) + (8 * _rectSpace), _rectWidth, _imsiHour[3], canvas);
				drawChartColumn(offsetX + (9 * _rectWidth) + (9 * _rectSpace), offsetX + (10 * _rectWidth) + (9 * _rectSpace), _rectWidth, _imsiHour[2], canvas);
				drawChartColumn(offsetX + (10 * _rectWidth) + (10 * _rectSpace), offsetX + (11 * _rectWidth) + (10 * _rectSpace), _rectWidth, _imsiHour[1], canvas);
				drawChartColumn(offsetX + (11 * _rectWidth) + (11 * _rectSpace), offsetX + (12 * _rectWidth) + (11 * _rectSpace), _rectWidth, _imsiHour[0], canvas);
			}
			else if (_timePeriod == 1)
			{
				int offsetX = (this.getMeasuredWidth() - (6 * _rectWidth) - (5 * _rectSpace)) / 2;
				boolean _imsiDay[][] = _activity.getMsdServiceHelperCreator().getThreatsImsiDay();
				
				drawChartColumn(offsetX, offsetX + (1 * _rectWidth), _rectWidth, _imsiDay[5], canvas);
				drawChartColumn(offsetX + (1 * _rectWidth) + (1 * _rectSpace), offsetX + (2 * _rectWidth) + (1 * _rectSpace), _rectWidth, _imsiDay[4], canvas);
				drawChartColumn(offsetX + (2 * _rectWidth) + (2 * _rectSpace), offsetX + (3 * _rectWidth) + (2 * _rectSpace), _rectWidth, _imsiDay[3], canvas);
				drawChartColumn(offsetX + (3 * _rectWidth) + (3 * _rectSpace), offsetX + (4 * _rectWidth) + (3 * _rectSpace), _rectWidth, _imsiDay[2], canvas);
				drawChartColumn(offsetX + (4 * _rectWidth) + (4 * _rectSpace), offsetX + (5 * _rectWidth) + (4 * _rectSpace), _rectWidth, _imsiDay[1], canvas);
				drawChartColumn(offsetX + (5 * _rectWidth) + (5 * _rectSpace), offsetX + (6 * _rectWidth) + (5 * _rectSpace), _rectWidth, _imsiDay[0], canvas);
			}
			else if (_timePeriod == 2)
			{
				drawChartColumn(this.getMeasuredWidth() / 4, this.getMeasuredWidth()-(getMeasuredWidth() / 4), _rectWidth, 
						_activity.getMsdServiceHelperCreator().getThreatsImsiWeekSum(), canvas);				
			}
			else if (_timePeriod == 3)
			{
				drawChartColumn(this.getMeasuredWidth() / 4, this.getMeasuredWidth()-(getMeasuredWidth() / 4), _rectWidth, 
						_activity.getMsdServiceHelperCreator().getThreatsImsiMonthSum(), canvas);	
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	private void drawChartColumn (int startX, int endX, int elementWidth, boolean[] _smsDay, Canvas canvas)
	{
		Paint myPaint = new Paint();
		myPaint.setStyle(Paint.Style.FILL);
		myPaint.setColor(_color);
		
		int left = startX;
		int top = this.getMeasuredHeight() - (elementWidth) - 10;
		int right = endX;
		int bottom = this.getMeasuredHeight()-10;
		
		int _columnSpace = 2;
		
		if (_smsDay.length == 0)
		{
			myPaint.setColor(Color.rgb(133, 180, 65));
			canvas.drawRect(left, top+(elementWidth*0.75f), right, bottom, myPaint);
		}
		else
		{
			for (int i=1; i<=_smsDay.length; i++)
			{
				if(_smsDay[i-1])
					myPaint.setColor(_color_uploaded);
				else
					myPaint.setColor(_color);
				if (i<5)
				{
					canvas.drawRect(left, top, right, bottom, myPaint);
					top -= (elementWidth + _columnSpace);
					bottom -= (elementWidth + _columnSpace);
				}
				else if (i==5 && _smsDay.length>5)
				{
					canvas.drawRect(left, top+(_rectWidth * 0.85f), right, bottom, myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.5875f), right, bottom-(_rectWidth * 0.2625f), myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.33f), right, bottom-(_rectWidth * 0.525f), myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.063f), right, bottom-(_rectWidth * 0.783f), myPaint);
				}
				else if (i==5 && _smsDay.length<=5)
				{
					canvas.drawRect(left, top, right, bottom, myPaint);
					top -= left*2.1f;
					bottom -= left*2.1f;
				}
			}
		}
	}
	
	private void setColor ()
	{
		switch (_threatType) {
		case 0:
			this._color = getResources().getColor(R.color.common_chartYellow);
			this._color_uploaded = getResources().getColor(R.color.common_chartYellow_uploaded);
			break;
		case 1:
			this._color = getResources().getColor(R.color.common_chartRed);
			this._color_uploaded = getResources().getColor(R.color.common_chartRed_uploaded);
			break;
		default:
			break;
		}
	}
}
