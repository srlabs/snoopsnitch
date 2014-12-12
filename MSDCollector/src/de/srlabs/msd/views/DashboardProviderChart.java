package de.srlabs.msd.views;

import java.util.Vector;

import javax.xml.datatype.Duration;

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
import android.widget.Toast;
import de.srlabs.msd.DashboardActivity;
import de.srlabs.msd.R;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.util.MSDServiceHelperCreator;

public class DashboardProviderChart extends View
{	
	private Canvas canvas;
	private DashboardActivity host;
	private float itemHeight;
	private float itemWidth;
	private float itemStrokeWidth;
	private float chartWidth;
	private float chartOffsetTopBottom;
	private float circleRadius;
	private float circleLineSpace;
	private float circleOffset;
	private double minScore = 0.5;
	private double maxScore = 0.5;
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
		
		this.host = (DashboardActivity) context;
	}

	@Override
	protected void onDraw(Canvas canvas) 
	{
		super.onDraw(canvas);
		
		providerData = MSDServiceHelperCreator.getInstance().getMsdServiceHelper().getData().getScores().getServerData();
		
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
        	String colorString = op.getOperatorColor();
        	if (colorString == null) {
        		colorString = "#000000";
        	}
        	int color = Color.parseColor(colorString);

        	if (providerData.lastElement().equals(op))
        	{
        		if (interImper == 0)
            	{
            		if (!op.getInter3G().isEmpty())
            		{
            			drawProviderScore(op.getInter3G().lastElement().getScore(), color,
                				false, 0, true, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            		
            		if (!op.getInter().isEmpty())
            		{
            			drawProviderScore(op.getInter().lastElement().getScore(), color,
            					true, 0, true, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            	}
            	else
            	{
            		if (!op.getImper3G().isEmpty())
            		{
            			drawProviderScore(op.getImper3G().lastElement().getScore(), color,
            					false, 0,  true, op.getOperatorName().equals(risk.getOperatorName()));	
            		}
            		
            		if (!op.getImper().isEmpty())
            		{
            			drawProviderScore(op.getImper().lastElement().getScore(), color,
            					 true, 0,  true, op.getOperatorName().equals(risk.getOperatorName()));
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
                				false, 0, false, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            		
            		if (!op.getInter().isEmpty())
            		{
            			drawProviderScore(op.getInter().lastElement().getScore(), color,
            					true, 0, false, op.getOperatorName().equals(risk.getOperatorName()));
            		}
            	}
            	else
            	{
            		if (!op.getImper3G().isEmpty())
            		{
            			drawProviderScore(op.getImper3G().lastElement().getScore(), color,
            					false, 0,  false, op.getOperatorName().equals(risk.getOperatorName()));	
            		}
            		
            		if (!op.getImper().isEmpty())
            		{
            			drawProviderScore(op.getImper().lastElement().getScore(), color,
            					 true, 0,  false, op.getOperatorName().equals(risk.getOperatorName()));
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
	    	paint.setStyle(Paint.Style.STROKE);
	    	paint.setColor(getResources().getColor(R.color.common_text));
			paint.setStrokeWidth(2);
			
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
	    canvas.drawRect(new Rect(0,0,getWidth(),(int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * maxScore) + itemHeight / 2)), p);
	    
	    // Draw min space
	    canvas.drawRect(new Rect(0,(int) ((getHeight() - chartOffsetTopBottom) - ((getHeight() - (chartOffsetTopBottom * 2)) * minScore) + itemHeight / 2),getWidth(),getHeight()), p); 
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
				
				if (!r.getImper3G().isEmpty())
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
}
