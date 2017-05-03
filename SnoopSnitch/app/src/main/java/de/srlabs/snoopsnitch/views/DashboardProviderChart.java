package de.srlabs.snoopsnitch.views;

import java.util.Collections;
import java.util.Vector;

import android.content.Context;
import android.content.res.TypedArray;
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
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.analysis.Risk;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;

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
	private double minScore = 1;
	private double maxScore = 0;
	private int interImper;
	private Vector<Risk> providerData;
	
	
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
		
		// Read attribute from XML
		TypedArray a = context.getTheme().obtainStyledAttributes(
		        attrs,
		        R.styleable.DashboardProviderChart,
		        0, 0);

		   try 
		   {
			   interImper = a.getInteger(R.styleable.DashboardProviderChart_type, 0);
		   } 
		   finally 
		   {
		       a.recycle();
		   }
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);
		
		providerData = MSDServiceHelperCreator.getInstance().getMsdServiceHelper().getData().getScores().getServerData();
		
		sortProviderData ();
		
		this.canvas = canvas;
		
		
		drawProviderChart();
		
		drawMinMaxBackground ();
		
		drawProvider();
	}
	
	private void drawProviderChart ()
	{
		chartOffsetTopBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
		
		// Set colors
		int colors[] = {
				getResources().getColor(R.color.common_chartGreen),
				getResources().getColor(R.color.common_chartYellow),
				getResources().getColor(R.color.common_chartRed)};
		
		Shader shader = new LinearGradient(0, chartOffsetTopBottom, 0, getHeight() - chartOffsetTopBottom, colors, null, TileMode.CLAMP);
		Paint paint = new Paint(); 
		paint.setShader(shader); 
		canvas.drawRect(new RectF(getWidth()/2-chartWidth, chartOffsetTopBottom, getWidth()/2+chartWidth, getHeight() - chartOffsetTopBottom), paint);
	}
	
	private void drawProvider ()
	{
		
		Risk risk = MSDServiceHelperCreator.getInstance().getMsdServiceHelper().getData().getScores();
		providerData.addElement(risk);
		
		if (risk.getOperatorName() == null) 
		{
			return;
		}

        for (Risk op : providerData) 
        {
        	int color = Color.parseColor(op.getOperatorColor());

        	if (providerData.lastElement().equals(op))
        	{
        		if (interImper == 0)
            	{
            		if (!op.getInter3G().isEmpty())
            		{
            			drawProviderScore(op.getInter3G().lastElement().getScore(), color,
                				false, getOffset(op, true, true), true, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            		
            		if (!op.getInter().isEmpty())
            		{
            			drawProviderScore(op.getInter().lastElement().getScore(), color,
            					true, getOffset(op, true, false), true, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            	}
            	else
            	{
            		if (!op.getImper3G().isEmpty())
            		{
            			drawProviderScore(op.getImper3G().lastElement().getScore(), color,
            					false, getOffset(op, false, true),  true, op.getOperatorName().equals(risk.getOperatorName()));	
            		}
            		
            		if (!op.getImper().isEmpty())
            		{
            			drawProviderScore(op.getImper().lastElement().getScore(), color,
            					 true, getOffset(op, false, false),  true, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            	}
        	}
        	else
        	{
        		if (interImper == 0)
            	{
            		if (!op.getInter3G().isEmpty())
            		{
            			drawProviderScore(op.getInter3G().lastElement().getScore(), color,
                				false, getOffset(op, true, true), false, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            		
            		if (!op.getInter().isEmpty())
            		{
            			drawProviderScore(op.getInter().lastElement().getScore(), color,
            					true, getOffset(op, true, false), false, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            	}
            	else
            	{
            		if (!op.getImper3G().isEmpty())
            		{
            			drawProviderScore(op.getImper3G().lastElement().getScore(), color,
            					false, getOffset(op, false, true),  false, op.getOperatorName().equals(risk.getOperatorName()));	
            		}
            		
            		if (!op.getImper().isEmpty())
            		{
            			drawProviderScore(op.getImper().lastElement().getScore(), color,
            					 true, getOffset(op, false, false),  false, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            	}	
        	}
        }
	}
	
	private void drawProviderScore (double score, int color, Boolean is2G, float offset, Boolean isResult, Boolean isOwnProvider)
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
			int right = getWidth() / 2;
			
			r = new Rect(left, top, right, bottom);
		}
		else
		{
			// Calculations
			int left = getWidth() / 2;
			int right = (int) ((getWidth() / 2) + itemWidth);
			
			r = new Rect(left, top, right, bottom);
		}
		
	    // fill
	    paint.setStyle(Paint.Style.FILL);
    	paint.setAntiAlias(true);
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
	    if (isResult)
	    {			
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(getResources().getColor(R.color.provider_circle_result_fill));
			drawProviderCircle(top, bottom, circleRadius, paint, is2G, offset, isResult, isOwnProvider);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2);
			paint.setColor(getResources().getColor(R.color.common_text));
			drawProviderCircle(top, bottom, circleRadius - paint.getStrokeWidth() / 2, paint, is2G, offset, isResult, isOwnProvider);
	    }
	    else if (isOwnProvider)
	    {
			paint.setStyle(Paint.Style.FILL);
	    	paint.setColor(color);
	    	
			drawProviderCircle(top, bottom, circleRadius / 2, paint, is2G, offset, isResult, isOwnProvider);
			
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2);
			
			drawProviderCircle(top, bottom, circleRadius - paint.getStrokeWidth() / 2, paint, is2G, offset, isResult, isOwnProvider);

	    }
	    else
	    {
	    	paint.setStyle(Paint.Style.FILL);
	    	paint.setColor(color);
	    	
			drawProviderCircle(top, bottom, circleRadius, paint, is2G, offset, isResult, isOwnProvider);
	    }  
	}
	
	private void drawProviderCircle (int top, int bottom, float radius, Paint paint, Boolean is2G, float offset, Boolean isResult, Boolean isOwnProvider)
	{
	    if (!is2G)
		{
			// Calculations
	    	int positionX = (int) ((getWidth() / 2) - itemWidth - offset);
	    	canvas.drawCircle(positionX - circleLineSpace, top + (itemHeight / 2), radius, paint);
		}
		else
		{
			// Calculations
	    	int positionX = (int) ((getWidth() / 2) + itemWidth + offset);
	    	canvas.drawCircle(positionX + circleLineSpace, top + (itemHeight / 2), radius, paint);
		}    
	}
	
	private void drawMinMaxBackground ()
	{
		setMinMaxScore();
		
		Paint p = new Paint();
	    p.setStyle(Paint.Style.FILL);
	    new Color();
		int c = Color.argb(150, 255, 255, 255);
	    p.setColor(c);
	    
		// Draw max space
	    canvas.drawRect(new Rect((int) (getWidth()/2-chartWidth),(int) chartOffsetTopBottom,(int) (getWidth()/2+chartWidth),
	    		(int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * maxScore) + itemHeight / 2)), p);
	    
	    // Draw min space
	    canvas.drawRect(new Rect((int) (getWidth()/2-chartWidth),
	    		(int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * minScore) + itemHeight / 2),
	    		(int) (getWidth()/2+chartWidth),(int) (getHeight() - chartOffsetTopBottom)), p); 
	}
	
	private void setMinMaxScore ()
	{		
		if (interImper == 0)
		{
			for (Risk r : providerData) 
			{
				if (!r.getInter().isEmpty())
				{
					if (r.getInter().lastElement().getScore() > maxScore)
					{
						maxScore = r.getInter().lastElement().getScore();
					}
					else if (r.getInter().lastElement().getScore() < minScore)
					{
						minScore = r.getInter().lastElement().getScore();
					}
				}
				
				if (!r.getInter3G().isEmpty())
				{
					if (r.getInter3G().lastElement().getScore() > maxScore)
					{
						maxScore = r.getInter3G().lastElement().getScore();
					}
					else if (r.getInter3G().lastElement().getScore() < minScore)
					{
						minScore = r.getInter3G().lastElement().getScore();
					}
				}
			}
		}
		else
		{
			for (Risk r : providerData) 
			{
				if (!r.getImper().isEmpty())
				{
					if (r.getImper().lastElement().getScore() > maxScore)
					{
						maxScore = r.getImper().lastElement().getScore();
					}
					else if (r.getImper().lastElement().getScore() < minScore)
					{
						minScore = r.getImper().lastElement().getScore();
					}
				}
				
				if (!r.getImper3G().isEmpty())
				{
					if (r.getImper3G().lastElement().getScore() > maxScore)
					{
						maxScore = r.getImper3G().lastElement().getScore();
					}
					else if (r.getImper3G().lastElement().getScore() < minScore)
					{
						minScore = r.getImper3G().lastElement().getScore();
					}
				}
			}
		}
	}
	
	private void sortProviderData ()
	{
		Risk risk = MSDServiceHelperCreator.getInstance().getMsdServiceHelper().getData().getScores();
		
		for (Risk r : providerData) 
		{
			if (providerData.elementAt(providerData.indexOf(r)).getOperatorName().equals(risk.getOperatorName()))
			{
				Collections.swap(providerData, providerData.size()-1, providerData.indexOf(r));
			}
		}
	}
	
	private float getOffset (Risk risk, Boolean interception, Boolean is3g)
	{
    	float offset = 0;
    	
    	for (int i=providerData.indexOf(risk)+1; i<providerData.size(); i++)
    	{
    		if (interception)
    		{
    			if (is3g)
    			{
    	    		if (providerData.get(i).getInter3G().equals(risk.getInter3G()))
    	    		{
    	    			offset += circleOffset;
    	    		}
    			}
    			else
    			{
    	    		if (providerData.get(i).getInter().equals(risk.getInter()))
    	    		{
    	    			offset += circleOffset;
    	    		}
    			}
    		}
    		else
    		{
    			if  (is3g)
    			{
    	    		if (providerData.get(i).getImper3G().equals(risk.getImper3G()))
    	    		{
    	    			offset += circleOffset;
    	    		}
    			}
    			else
    			{
    	    		if (providerData.get(i).getImper().equals(risk.getImper()))
    	    		{
    	    			offset += circleOffset;
    	    		}
    			}
    		}
    	}
    	
    	return offset;
	}
}
