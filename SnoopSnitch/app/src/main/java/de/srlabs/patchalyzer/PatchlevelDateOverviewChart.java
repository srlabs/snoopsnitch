package de.srlabs.patchalyzer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;


public class PatchlevelDateOverviewChart extends View {
    private int[] colors;
    private Paint myPaint;
    private int nElems;
    float paddingLeft, paddingTop, elementWidth, gapWidth, elementHeight;

    public PatchlevelDateOverviewChart(Context context, int[] colors){
        super(context);
        this.colors = colors;
        myPaint = new Paint();
        myPaint.setStyle(Paint.Style.FILL);
        nElems = colors.length;
        setPadding(10,10,10,10);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if(h > 50)
            h = 50;
        if(h < 50 && MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.AT_MOST)
            h = 50;
        //Log.i(Constants.LOG_TAG, "PatchlevelDateOverviewChart.onMeasure: w=" + w + "  h=" + h);
        setMeasuredDimension(w, h);
        onSizeChanged(w,h,0,0);
    }
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(Constants.LOG_TAG, "PatchlevelDateOverviewChart.onSizeChanged(" + w + ", " + h + ", " + oldw + ", " + oldh + ")");
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        //Log.i(Constants.LOG_TAG, "getPaddingLeft() = " + getPaddingLeft() + ", getPaddingRight()=" + getPaddingRight() + ", getPaddingTop()=" + getPaddingTop() + ", getPaddingBottom()=" + getPaddingBottom());
       float ww = (float)w - xpad;
        float hh = (float)h - ypad;
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        float unscaledElementWidth = 1.0f;
        float unscaledGapWidth = 0.4f;
        float scalingFactor = ww / (nElems * unscaledElementWidth + (nElems-1.0f) * unscaledGapWidth);
        // Log.i(Constants.LOG_TAG, "scalingFactor = " + scalingFactor);
        elementWidth = scalingFactor * unscaledElementWidth;
        gapWidth = scalingFactor * unscaledGapWidth;
        elementHeight = hh;
        //Log.i(Constants.LOG_TAG, "elementWidth=" + elementWidth + "   gapWidth=" + gapWidth + "   elementHeight=" + elementHeight);
    }
    public void onDraw(Canvas canvas) {
        Log.i(Constants.LOG_TAG, "PatchlevelDateOverviewChart.onDraw()");
        float x = paddingLeft;
        for (int i = 0; i < nElems; i++) {
            // Draw element
            myPaint.setColor(colors[i]);
            //Log.i(Constants.LOG_TAG, "PatchlevelDateOverviewChart.onDraw() => drawRect(" + x + ", " + paddingTop + ", " + (x + elementWidth) + ", " + (paddingTop + elementHeight) + ", "+ colors[i] + ")");
            canvas.drawRect(x, paddingTop, x + elementWidth, paddingTop + elementHeight, myPaint);
            x = x + elementWidth + gapWidth;
        }
    }
}
