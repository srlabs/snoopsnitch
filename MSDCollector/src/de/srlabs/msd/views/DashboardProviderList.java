package de.srlabs.msd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DashboardProviderList extends View
{
	private Canvas canvas;
	
	public DashboardProviderList(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		this.canvas = canvas;
	}

	private void drawCircle (int color, Boolean isOwnProvider, Boolean isResult)
	{
		Paint p = new Paint();
		p.setColor(color);
		p.setAntiAlias(true);
		p.setStyle(Paint.Style.FILL);
		
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, p);
	}
}
