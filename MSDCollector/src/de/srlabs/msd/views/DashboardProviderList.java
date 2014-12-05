package de.srlabs.msd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import de.srlabs.msd.DashboardActivity;
import de.srlabs.msd.R;

public class DashboardProviderList extends View
{
	private Canvas canvas;
	private DashboardActivity host;
	private int color;
	private Boolean isOwnProvider = false;
	private Boolean isResult = false;
	
	public DashboardProviderList(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		this.host = (DashboardActivity) context;
	}
	
	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);
		
		this.canvas = canvas;
		
		drawCircle(color, isOwnProvider, isResult);
	}

	private void drawCircle (int color, Boolean isOwnProvider, Boolean isResult)
	{
		Paint p = new Paint();
		p.setColor(color);
		p.setAntiAlias(true);
		
		if (isOwnProvider)
		{			
			p.setStyle(Paint.Style.FILL);
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 4, p);
			
			p.setStyle(Paint.Style.STROKE);
			p.setStrokeWidth(2);
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, ((getWidth() - p.getStrokeWidth()) / 2), p);
		}
		else if (isResult)
		{
			p.setStyle(Paint.Style.STROKE);
			p.setStrokeWidth(2);
			p.setColor(getResources().getColor(R.color.common_text));
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, ((getWidth() - p.getStrokeWidth()) / 2), p);
		}
		else
		{
			p.setStyle(Paint.Style.FILL);	
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, p);
		}
	}
	
	public void setColor (int color)
	{
		this.color = color;
	}
	
	public void setResult (Boolean isResult)
	{
		this.isResult = isResult;
	}
	
	public void setOwnProvider (Boolean isOwnProvider)
	{
		this.isOwnProvider = isOwnProvider;
	}
}
