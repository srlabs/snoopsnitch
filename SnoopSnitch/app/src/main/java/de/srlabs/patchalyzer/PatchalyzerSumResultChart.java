package de.srlabs.patchalyzer;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.snoopsnitch.R;

/**This UI element visually summarizes the patchalyzer test results in a bar chart
 * Created by jonas on 20.02.18.
 */

public class PatchalyzerSumResultChart extends View {
    private Canvas canvas;
    private float itemHeight;
    private float itemWidth;
    //private float chartWidth=30f;
    private float chartOffsetTopBottom;
    private boolean showNumbers=false;
    private boolean isSmall=false;
    private boolean drawBorder=false;
    static HashMap<String,ResultPart> parts = new HashMap<String,ResultPart>();

    public PatchalyzerSumResultChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        itemHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        // Read attribute from XML, so we can reuse the same view in SNSN overview and PA details
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PatchalyzerSumResultChart,
                0, 0);

        try {
            showNumbers = a.getBoolean(R.styleable.PatchalyzerSumResultChart_shownumbers, false);
            isSmall = a.getBoolean(R.styleable.PatchalyzerSumResultChart_small,false);
            drawBorder = a.getBoolean(R.styleable.PatchalyzerSumResultChart_drawborder, false);

            Log.d(Constants.LOG_TAG,"PatchalyzerSumResultChart created: "+ (showNumbers ? "showing numbers" : "not showing numbers")+";"+(isSmall ? "small" : "large"));
        } finally {
            a.recycle();
        }

        parts.put("patched",new ResultPart(0,Constants.COLOR_PATCHED));
        parts.put("inconclusive",new ResultPart(0, Constants.COLOR_INCONCLUSIVE));
        parts.put("missing",new ResultPart(0,Constants.COLOR_MISSING));
        parts.put("notAffected",new ResultPart(0,Constants.COLOR_NOTAFFECTED));

    }

    public void increasePatched(int addition){
        ResultPart part = parts.get("patched");
        part.addCount(addition);
    }

    public void increaseInconclusive(int addition){
        ResultPart part = parts.get("inconclusive");
        part.addCount(addition);
    }

    public void increaseMissing(int addition){
        ResultPart part = parts.get("missing");
        part.addCount(addition);
    }

    public void increaseNotAffected(int addition){
        ResultPart part = parts.get("notAffected");
        part.addCount(addition);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.canvas = canvas;
        drawResultChart();
    }

    private void drawResultChart() {
        chartOffsetTopBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        Paint paint = new Paint();

        float marginleftright = 50f;
        float borderWidth = 3f;
        float chartWidth = getWidth()-marginleftright;
        float chartHeight;
        float textSize;

        //apply xml arguments
        if(isSmall) {
            chartHeight = getHeight() * 0.5f;
            textSize = 20f;
        }
        else{
            chartHeight = getHeight() * 0.8f;
            textSize = 60f;
        }

        int sumCVEs = 0;
        //calculate sum of CVE tests
        for(ResultPart part : parts.values()){
            sumCVEs += part.getCount();
        }
        float startX = marginleftright / 2f;
        float partWidth;
        if(sumCVEs > 0) {
            //draw parts in list order
            for (ResultPart part : parts.values()) { //FIXME not sorted here, or?!
                partWidth = startX + chartWidth * (1f * part.getCount() / sumCVEs);
                paint.setColor(part.getColor());
                paint.setTextSize(textSize);
                //Log.d(Constants.LOG_TAG, "" + part + " bar: " + startX + "|" + chartOffsetTopBottom + " -> " + partWidth + "|" + chartOffsetTopBottom + chartHeight);
                canvas.drawRect(new RectF(startX, chartOffsetTopBottom, partWidth, chartOffsetTopBottom + chartHeight), paint);

                if (showNumbers) {
                    if(part.getCount() > 10) {
                        paint.setColor(Color.BLACK);
                        canvas.drawText("" + part.getCount(), (startX + partWidth) / 2f - (textSize / 2f), (chartOffsetTopBottom + chartHeight + textSize) / 2f, paint);
                    }
                }
                startX = partWidth;
            }
        }
        else{
            //default (no test results available)
            paint.setColor(Color.GRAY);
            canvas.drawRect(new RectF(startX, chartOffsetTopBottom, startX+chartWidth, chartOffsetTopBottom + chartHeight), paint);
            paint.setColor(Color.BLACK);
            paint.setTextSize(textSize);
            //FIXME text position not correctly centered vertically!!
            canvas.drawText(this.getResources().getString(R.string.patchalyzer_no_test_result), (chartWidth * 0.3f) , (chartOffsetTopBottom + chartHeight + textSize) / 2f, paint);
        }

        if(drawBorder){
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.DKGRAY);
            paint.setStrokeWidth(borderWidth);
            canvas.drawRect(marginleftright/2,chartOffsetTopBottom,chartWidth+marginleftright/2,chartOffsetTopBottom+chartHeight, paint);
        }

    }

    public void resetCounts() {
        for(ResultPart part : parts.values()){
            part.setCount(0);
        }
    }

    protected class ResultPart{
        private int count;
        private int color;

        public ResultPart(int count, int color){
            this.count = count;
            this.color = color;
        }

        public void setCount(int count){
            this.count = count;
        }

        public int getCount(){
            return count;
        }

        public int getColor(){
            return color;
        }

        public void addCount(int addition){
            this.count += addition;
        }
    }

    // using SharedPrefs
    public void saveValuesToSharedPrefs(ContextWrapper context) {
        JSONObject jsonObject = new JSONObject();

        try {
            for(Map.Entry entry : parts.entrySet()) {
                jsonObject.put((String) entry.getKey(), ((ResultPart) entry.getValue()).getCount());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(Constants.LOG_TAG,"Writing resultsChart state to sharedPrefs");
        SharedPreferences settings = context.getSharedPreferences("PATCHALYZER", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("resultsChart", jsonObject.toString());
        editor.commit();

    }

    // using SharedPrefs
    public void loadValuesFromSharedPrefs(ContextWrapper context) {
        Log.d(Constants.LOG_TAG,"Reading resultsChart state from sharedPrefs");
        SharedPreferences settings = context.getSharedPreferences("PATCHALYZER", 0);

        try {
            JSONObject jsonObject = new JSONObject(settings.getString("resultsChart", "{}"));
            Iterator<String> it = jsonObject.keys();
            if (it.hasNext()) {
                resetCounts();
            }

            while (it.hasNext()) {
                String key = it.next();
                ResultPart part = parts.get(key);
                part.addCount(jsonObject.getInt(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}
