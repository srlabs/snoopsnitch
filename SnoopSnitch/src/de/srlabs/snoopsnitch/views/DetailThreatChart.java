package de.srlabs.snoopsnitch.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.DetailChartActivity;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;

public class DetailThreatChart extends View
{
	int _timePeriod;
	int _timeSpacePosition;
	int _threatType;
	int _amount;	
	int _rectWidth;
	int _color;
	int _color_uploaded;
	boolean[][] _items;
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
			_color_uploaded = getResources().getColor(R.color.common_chartYellow_uploaded);
			//_imgThreatType.setBackgroundResource(R.drawable.ic_content_sms_event);
		}
		else
		{
			_color = getResources().getColor(R.color.common_chartRed);
			_color_uploaded = getResources().getColor(R.color.common_chartRed_uploaded);
			//_imgThreatType.setBackgroundResource(R.drawable.ic_content_sms_event);
		}
		
		this._rectWidth = MSDServiceHelperCreator.getInstance().getRectWidth();
		this.context = context;
	}

	protected void drawChartColumn (float startX, float endX, int elementWidth, boolean[] items_upload_state, Canvas canvas)
	{
		Paint myPaint = new Paint();
		myPaint.setStyle(Paint.Style.FILL);
		myPaint.setColor(_color);
		
		float left = startX;
		float top = this.getMeasuredHeight() - (elementWidth) - 10;
		float right = endX;
		float bottom = this.getMeasuredHeight()-10;
		
		float _columnSpace = 2;
		
		if (items_upload_state.length == 0)
		{
			myPaint.setColor(getResources().getColor(R.color.common_chartGreen));
			canvas.drawRect(left, top+(elementWidth*0.75f), right, bottom, myPaint);
		}
		else
		{
			for (int i=1; i<=items_upload_state.length; i++)
			{
				if(items_upload_state[i-1])
					myPaint.setColor(_color_uploaded);
				else
					myPaint.setColor(_color);
				if (i<6)
				{
					canvas.drawRect(left, top, right, bottom, myPaint);
					top -= (elementWidth + _columnSpace);
					bottom -= (elementWidth + _columnSpace);
				}
				else if (i==6 && items_upload_state.length>6)
				{
					canvas.drawRect(left, top+(_rectWidth * 0.85f), right, bottom, myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.5875f), right, bottom-(_rectWidth * 0.2625f), myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.33f), right, bottom-(_rectWidth * 0.525f), myPaint);
					canvas.drawRect(left, top+(_rectWidth * 0.063f), right, bottom-(_rectWidth * 0.783f), myPaint);
				}
				else if (i==6 && items_upload_state.length<=6)
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
