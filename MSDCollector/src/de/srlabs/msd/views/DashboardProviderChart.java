package de.srlabs.msd.views;

import java.util.ArrayList;

import de.srlabs.msd.R;
import de.srlabs.msd.util.ProviderScore;
import android.R.bool;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class DashboardProviderChart extends View
{	
	private Canvas canvas;
	private float itemHeight;
	private float itemWidth;
	private float itemStrokeWidth;
	private float chartWidth;
	private float chartOffsetTopBottom;
	private float circleRadius;
	private float circleLineSpace;
	private float circleOffset;
	private float minScore;
	private float maxScore;
	private ArrayList<ProviderScore> providerScoreList;
	
	public DashboardProviderChart(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		itemHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
		itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
		itemStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
		chartWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
		circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
		circleLineSpace = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()) + (circleRadius / 2);
		circleOffset = (circleRadius / 2);
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);
		
		this.canvas = canvas;
	
		drawProviderChart();
		
		providerScoreList = new ArrayList<ProviderScore>();
		
		providerScoreList.add(new ProviderScore(0.81f, false));
		providerScoreList.add(new ProviderScore(0.64f, false));
		providerScoreList.add(new ProviderScore(0.89f, false));
		providerScoreList.add(new ProviderScore(0.15f, false));
		providerScoreList.add(new ProviderScore(0.26f, false));
		providerScoreList.add(new ProviderScore(0.64f, false));
		providerScoreList.add(new ProviderScore(0.32f, true));
		providerScoreList.add(new ProviderScore(0.45f, false));
		providerScoreList.add(new ProviderScore(0.65f, false));
		providerScoreList.add(new ProviderScore(0.15f, true));
		
		drawMinMaxBackground ();
		
		for (ProviderScore ps : providerScoreList) 
		{
			float offset = 0;
			
			for (int i=0; i<providerScoreList.indexOf(ps); i++)
			{
				if (providerScoreList.get(i).getScore() == ps.getScore())
				{
					offset += circleOffset;
				}
			}
			
			drawProviderScore(ps.getScore(), ps.getIs2G(), offset);
		}	
	}
	
	private void drawProviderChart ()
	{
		chartOffsetTopBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
		
		// Set colors
		int colors[] = {
				Color.parseColor(getResources().getString(R.color.common_chartGreen)),
				Color.parseColor(getResources().getString(R.color.common_chartYellow)),
				Color.parseColor(getResources().getString(R.color.common_chartRed))};
		
		// Set positions (if needed)
		float positions[]={
				0f,
				0.2f,
				0.8f};
		
		Shader shader = new LinearGradient(0, chartOffsetTopBottom, 0, getHeight() - chartOffsetTopBottom, colors, null, TileMode.CLAMP);
		Paint paint = new Paint(); 
		paint.setShader(shader); 
		canvas.drawRect(new RectF(getWidth()/2-chartWidth, chartOffsetTopBottom, getWidth()/2+chartWidth, getHeight() - chartOffsetTopBottom), paint);
	}
	
	private void drawProviderScore (float score, boolean is2G, float offset)
	{
		// Calculations
		int top = (int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * score));
		int bottom = (int) (top + itemHeight);
		//int bottom = (int) (getHeight() * score);
		
		Paint paint = new Paint();
		Rect r;
		
		/**
		 * Draw the line
		 */
		if (!is2G)
		{
			// Calculations
			int left = (int) ((getWidth() / 2) - itemWidth);
			int right = (int) (getWidth() / 2);
			
			r = new Rect(left, top, right, bottom);
		}
		else
		{
			// Calculations
			int left = (int) (getWidth() / 2);
			int right = (int) ((getWidth() / 2) + itemWidth);
			
			r = new Rect(left, top, right, bottom);
		}
		
	    // fill
	    paint.setStyle(Paint.Style.FILL);
	    paint.setColor(getResources().getColor(R.color.common_text)); 
	    canvas.drawRect(r, paint);

	    // border
	    paint.setStyle(Paint.Style.STROKE);
	    paint.setStrokeWidth(itemStrokeWidth);
	    paint.setColor(Color.WHITE);
	    canvas.drawRect(r, paint);
	    
	    /**
	     * Draw the circle
	     */
	    paint.setStyle(Paint.Style.FILL);
	    paint.setColor(Color.GRAY);
	    paint.setAntiAlias(true);
	    
	    if (!is2G)
		{
			// Calculations
	    	int positionX = (int) ((getWidth() / 2) - itemWidth - offset);
	    	canvas.drawCircle(positionX - circleLineSpace, top + (itemHeight / 2), circleRadius, paint);
		}
		else
		{
			// Calculations
	    	int positionX = (int) ((getWidth() / 2) + itemWidth + offset);
	    	canvas.drawCircle(positionX + circleLineSpace, top + (itemHeight / 2), circleRadius, paint);
		}    
	}
	
	private void drawMinMaxBackground ()
	{
		setMinMaxScore();
		
		Paint p = new Paint();
	    p.setStyle(Paint.Style.FILL);
	    int c = new Color().argb(150, 255, 255, 255);
	    p.setColor(c);
	    
		// Draw max space
	    canvas.drawRect(new Rect(0,0,getWidth(),(int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * maxScore) + itemHeight / 2)), p);
	    
	    // Draw min space
	    canvas.drawRect(new Rect(0,(int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * minScore) + itemHeight / 2),getWidth(),getHeight()), p); 
	}
	
	private void setMinMaxScore ()
	{
		maxScore = providerScoreList.get(0).getScore();
		minScore = providerScoreList.get(0).getScore();
		
		for (ProviderScore ps : providerScoreList) 
		{
			if (ps.getScore() > maxScore)
			{
				maxScore = ps.getScore();
			}
			
			if (ps.getScore() < minScore)
			{
				minScore = ps.getScore();
			}
		}
	}
}
