package de.srlabs.msd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import de.srlabs.msd.DashboardActivity;

public class DashboardProviderList extends View
{
	private Canvas canvas;
	private DashboardActivity host;
	private int color;
	
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
		
		drawCircle(color, false, false);
	}

	private void drawCircle (int color, Boolean isOwnProvider, Boolean isResult)
	{
		Paint p = new Paint();
		p.setColor(color);
		p.setAntiAlias(true);
		p.setStyle(Paint.Style.FILL);
		
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, p);
	}
	
	public void setColor (int color)
	{
		this.color = color;
	}
}
