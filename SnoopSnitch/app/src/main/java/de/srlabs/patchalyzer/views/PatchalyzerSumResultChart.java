package de.srlabs.patchalyzer.views;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.PatchalyzerMainActivity;
import de.srlabs.patchalyzer.analysis.TestUtils;
import de.srlabs.patchalyzer.helpers.SharedPrefsHelper;

/**This UI element visually summarizes the patchalyzer test results in a horizontal,rectangular bar chart
 * Created by jonas on 20.02.18.
 */

public class PatchalyzerSumResultChart extends View {
    private Canvas canvas;
    private float chartOffsetTopBottom;
    private boolean showNumbers = false;
    private boolean isSmall = false;
    private boolean drawBorder = true;
    static HashMap<Result, ResultPart> parts = new HashMap<Result, ResultPart>();
    private static boolean isAnalysisRunning = false;
    private static JSONObject resultToDrawFrom = null;
    private final boolean PAINT_DEBUG = false; //set to true to e.g. see center of drawn parts

    public enum Result{
        PATCHED,
        MISSING,
        NOTCLAIMED,
        INCONCLUSIVE,
        NOTAFFECTED
    }

    private static final Result[] drawOrder = {Result.PATCHED, Result.MISSING, Result.NOTCLAIMED, Result.INCONCLUSIVE, Result.NOTAFFECTED};

    public PatchalyzerSumResultChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        float itemHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        float itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        // Read attribute from XML, so we can reuse the same view in SNSN overview and PA details
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                getResultChartStyleables(),
                0, 0);

        try {
            showNumbers = a.getBoolean(getStyleableId("PatchalyzerSumResultChart_shownumbers"), false);
            isSmall = a.getBoolean(getStyleableId("PatchalyzerSumResultChart_small"), false);
            drawBorder = a.getBoolean(getStyleableId("PatchalyzerSumResultChart_drawborder"), true);

            //Log.d(Constants.LOG_TAG, "PatchalyzerSumResultChart created: " + (showNumbers ? "showing numbers" : "not showing numbers") + ";" + (isSmall ? "small" : "large"));
        } finally {
            a.recycle();
        }

        parts.put(Result.PATCHED, new ResultPart(0, Constants.COLOR_PATCHED));
        parts.put(Result.INCONCLUSIVE, new ResultPart(0, Constants.COLOR_INCONCLUSIVE));
        parts.put(Result.MISSING, new ResultPart(0, Constants.COLOR_MISSING));
        parts.put(Result.NOTAFFECTED, new ResultPart(0, Constants.COLOR_NOTAFFECTED));
        parts.put(Result.NOTCLAIMED, new ResultPart(0, Constants.COLOR_NOTCLAIMED));
    }

    private int[] getResultChartStyleables(){
        return Constants.getAppFlavor().getStyleIdResultChart();
    }

    private int getStyleableId(String styleableId) {
        return Constants.getAppFlavor().getStyleResultChart(styleableId);
    }

    public static void setAnalysisRunning(boolean running){
        isAnalysisRunning = running;
    }

    public void increasePatched(int addition) {
        ResultPart part = parts.get(Result.PATCHED);
        if(part != null)
            part.addCount(addition);
    }

    public void increaseInconclusive(int addition) {
        ResultPart part = parts.get(Result.INCONCLUSIVE);
        if(part != null)
            part.addCount(addition);
    }

    public void increaseMissing(int addition) {
        ResultPart part = parts.get(Result.MISSING);
        if(part != null)
            part.addCount(addition);
    }

    public void increaseNotAffected(int addition) {
        ResultPart part = parts.get(Result.NOTAFFECTED);
        if(part != null)
            part.addCount(addition);
    }

    public void increaseNotClaimed(int addition) {
        ResultPart part = parts.get(Result.NOTCLAIMED);
        if(part != null)
            part.addCount(addition);
    }

    public static void setResultToDrawFromOnNextUpdate(JSONObject newResultToDrawFrom) {
        resultToDrawFrom = newResultToDrawFrom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (resultToDrawFrom != null) {
            loadValuesFromJSONResult(resultToDrawFrom);
            resultToDrawFrom = null;
        }
        this.canvas = canvas;

        chartOffsetTopBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        float marginleftright = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        float chartWidth = getWidth() - marginleftright;
        float chartHeight;

        //apply xml arguments
        if (isSmall) {
            chartHeight = getHeight() * 0.6f;
        } else {
            chartHeight = getHeight() * 0.8f;
        }

        float borderWidth = chartHeight * 0.025f;

        // do not show inconclusive test results, if disabled in settings (-> default is disabled)
        if(!getShowInconclusivePatchAnalysisTestResults(getContext())){
            parts.remove(Result.INCONCLUSIVE);
        }

        int sumCVEs = 0;
        //calculate sum of CVE tests
        for (ResultPart part : parts.values()) {
            sumCVEs += part.getCount();
        }

        if(sumCVEs > 0 && !TestUtils.isTooOldAndroidAPIVersion() && !isAnalysisRunning) {
            drawResults(sumCVEs, chartHeight, marginleftright);
        }
        else{
            drawNoResults(chartHeight, chartWidth, marginleftright);
        }

        //finally draw borders around chart
        if (drawBorder) {
            drawBorders(borderWidth, chartWidth, chartHeight, marginleftright);
        }
    }

    /**
     * Display status text instead of test results
     * @param chartHeight
     * @param chartWidth
     * @param marginleftright
     */
    private void drawNoResults(float chartHeight, float chartWidth, float marginleftright){
        //default (no test results available)
        Paint paint = new Paint();
        float startX = marginleftright / 2f;
        float textSize = chartHeight * 0.35f;

        //draw chart with gray background
        paint.setColor(getColor(this.getContext(), "common_button_state_default"));
        canvas.drawRect(new RectF(startX, chartOffsetTopBottom, startX + chartWidth, chartOffsetTopBottom + chartHeight), paint);

        String text = null;
        if(TestUtils.isTooOldAndroidAPIVersion()){
            text = getString(this,"patchalyzer_too_old_android_api_level_result_chart");
        }
        else if(isAnalysisRunning){
            text = getString(this,"patchalyzer_analysis_in_progress");
        }
        else {
            text = getString(this, "patchalyzer_no_test_result");
        }

        //calculate centered text position
        float left = marginleftright;
        float right = chartWidth;
        float top = chartOffsetTopBottom;
        float bottom = chartHeight + chartOffsetTopBottom;

        if(PAINT_DEBUG) {
            //indicate center of chart with blue line
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(2f);
            canvas.drawLine((right - left) / 2f, top, (right - left) / 2f, bottom, paint);
        }

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        drawCenteredText(text, paint, left, right, top, bottom);

    }

    /**
     * Display the latest test result:
     * calculate the individual percentage of the result parts and draw the parts accordingly
     * optional: show the numbers of each part in the center (if they fit in there)
     * @param sumCVEs
     * @param chartHeight
     * @param marginleftright
     */
    private void drawResults(int sumCVEs, float chartHeight, float marginleftright) {
        Paint paint = new Paint();
        float chartWidth = getWidth() - marginleftright;
        float borderWidth = chartHeight * 0.025f;
        float startX = marginleftright / 2f;
        float partWidth;
        float textSize = chartHeight * 0.5f;

        if (sumCVEs > 0) {
            //draw parts in list order
            for (Result key : drawOrder) {
                ResultPart part = parts.get(key);
                if(part == null) //inconclusive might not be in there
                    continue;

                partWidth = startX + chartWidth * (1f * part.getCount() / sumCVEs);
                paint.setColor(part.getColor());
                paint.setTextSize(textSize);
                //Log.d(Constants.LOG_TAG, "" + part + " bar: " + startX + "|" + chartOffsetTopBottom + " -> " + partWidth + "|" + chartOffsetTopBottom + chartHeight);
                canvas.drawRect(new RectF(startX, chartOffsetTopBottom, partWidth, chartOffsetTopBottom + chartHeight), paint);

                if(PAINT_DEBUG) {
                    //indicate center of parts with blue line
                    paint.setColor(Color.BLUE);
                    paint.setStrokeWidth(2f);
                    canvas.drawLine((startX + partWidth) / 2f, chartOffsetTopBottom, (startX + partWidth) / 2f, chartOffsetTopBottom + chartHeight, paint);
                }

                if (showNumbers) {
                    if (isNumberFittingDrawnPart(part.getCount(),chartWidth * (1f * part.getCount() / sumCVEs), paint)) {
                        paint.setColor(Color.BLACK);
                        paint.setAntiAlias(true);
                        float top = chartOffsetTopBottom;
                        float bottom = chartHeight + chartOffsetTopBottom;
                        drawCenteredText(""+part.getCount(), paint, startX-(marginleftright/20), partWidth, top, bottom);
                    }
                }
                startX = partWidth;
            }
        }
    }

    /**
     * Draw the borders of the chart in darkgray
     * @param borderWidth
     * @param chartWidth
     * @param chartHeight
     * @param marginleftright
     */
    private void drawBorders(float borderWidth, float chartWidth, float chartHeight, float marginleftright) {
        //Log.d(Constants.LOG_TAG,"chartHeight: "+chartHeight+" borderWidth:"+borderWidth+" border: "+(marginleftright / 2)+"|"+chartOffsetTopBottom+" -> "+(chartWidth + marginleftright / 2)+"|"+(chartOffsetTopBottom + chartHeight - borderWidth));

        // Fix rounding issues
        borderWidth = (float) Math.ceil(borderWidth);
        float left = marginleftright / 2;
        float top = chartOffsetTopBottom;
        float right = chartWidth + marginleftright / 2;
        float bottom = (float) Math.ceil(Math.floor(chartOffsetTopBottom + chartHeight) - borderWidth / 2);


        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getColor(this.getContext(), "common_sectionSeparator"));
        paint.setStrokeWidth(borderWidth);

        canvas.drawRect(left, top, right, bottom, paint);
    }

    private void drawCenteredText(String text, Paint paint, float left, float right, float top, float bottom){
        Rect textBoundsRect = new Rect();
        paint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.NORMAL));
        paint.getTextBounds(text, 0, text.length(), textBoundsRect);
        Paint.FontMetrics fm = paint.getFontMetrics();

        float x = left + (right - left - textBoundsRect.width()) / 2;
        float y = top + (bottom - top) / 2 - (fm.descent + fm.ascent) / 2;

        canvas.drawText(text, x, y, paint);
    }

    /**
     * Decide whether the number really fits in the part we want to draw it onto by calculating the bounds and adding a margin
     * @param count
     * @param partWidth
     * @param paint
     * @return
     */
    private boolean isNumberFittingDrawnPart(int count, float partWidth, Paint paint) {
        float NUMBER_MARGIN = 20f;
        Rect textBoundsRect = new Rect();
        String text = ""+count;
        paint.getTextBounds(text, 0, text.length(), textBoundsRect);
        //Log.d(Constants.LOG_TAG,"textwidth: "+textBoundsRect.width()+" partWidth:"+partWidth);
        return textBoundsRect.width() + NUMBER_MARGIN < partWidth;
    }

    public void resetCounts() {
        for (ResultPart part : parts.values()) {
            part.setCount(0);
        }
        // add inconclusive again, so that it can be set
        if(!parts.containsKey(Result.INCONCLUSIVE)){
            parts.put(Result.INCONCLUSIVE, new ResultPart(0, Constants.COLOR_INCONCLUSIVE));
        }
    }

    /**
     * Load the results from the JSONObject and display these
     * @param analysisResult
     */
    public void loadValuesFromJSONResult(JSONObject analysisResult) {
        setAnalysisRunning(false);
        if (analysisResult == null) {
            resetCounts();
            return;
        }
        try {
            int numPatched = 0;
            int numInconclusive = 0;
            int numMissing = 0;
            int numNotAffected = 0;
            int numNotClaimed = 0;

            Iterator<String> categoryIterator = analysisResult.keys();
            while (categoryIterator.hasNext()) {
                String category = categoryIterator.next();
                JSONArray vulnerabilities = analysisResult.getJSONArray(category);
                // category "other", or a patch date that should be covered
                for (int i = 0; i < vulnerabilities.length(); i++) {
                    JSONObject vulnerability = vulnerabilities.getJSONObject(i);

                    int color = PatchalyzerMainActivity.getVulnerabilityIndicatorColor(vulnerability, category);
                    switch(color) {
                        case Constants.COLOR_PATCHED:
                            numPatched++;
                            break;
                        case Constants.COLOR_INCONCLUSIVE:
                            numInconclusive++;
                            break;
                        case Constants.COLOR_MISSING:
                            numMissing++;
                            break;
                        case Constants.COLOR_NOTAFFECTED:
                            numNotAffected++;
                            break;
                        case Constants.COLOR_NOTCLAIMED:
                            numNotClaimed++;
                            break;
                    }
                }
            }

            resetCounts();
            increasePatched(numPatched);
            increaseInconclusive(numInconclusive);
            increaseMissing(numMissing);
            increaseNotAffected(numNotAffected);
            increaseNotClaimed(numNotClaimed);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void loadValuesFromCachedResult(ContextWrapper context){
        loadValuesFromJSONResult(SharedPrefsHelper.getAnalysisResult(context));
    }

    public HashMap<Result, ResultPart> getParts(){
        return parts;
    }

    /**
     * local container class here
     */
    public class ResultPart {
        private int count;
        private int color;

        public ResultPart(int count, int color) {
            this.count = count;
            this.color = color;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public int getColor() {
            return color;
        }

        public void addCount(int addition) {
            this.count += addition;
        }
    }

    private boolean getShowInconclusivePatchAnalysisTestResults(Context context) {
        return Constants.getAppFlavor().getShowInconclusivePatchAnalysisTestResults(context);
    }

    private int getColor(Context context, String colorId) {
        return Constants.getAppFlavor().getColor(context, colorId);
    }

    private String getString(View view, String stringId) {
        return Constants.getAppFlavor().getString(view.getContext(),stringId);
    }
}
