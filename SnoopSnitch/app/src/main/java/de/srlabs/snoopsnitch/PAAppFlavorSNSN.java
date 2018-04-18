package de.srlabs.snoopsnitch;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;

import de.srlabs.patchalyzer.AppFlavor;
import de.srlabs.patchalyzer.PatchalyzerMainActivity;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.Utils;

public class PAAppFlavorSNSN implements AppFlavor {

    public static final String BINARIES_PATH = "/data/data/de.srlabs.snoopsnitch/lib/";
    private static final String PACKAGE_NAME = "de.srlabs.snoopsnitch";

    @Override
    public String getBinaryPath() {
        return BINARIES_PATH;
    }

    @Override
    public String setAppId(Context context) {
        if (MsdConfig.getAppId(context).equals("")) {
            MsdConfig.setAppId(context, generateAppId());
        }
        return MsdConfig.getAppId(context);
    }

    public static String generateAppId() {
        return Utils.generateAppId();
    }

    @Override
    public void setShowInconclusiveResults(Context context, boolean showInconclusive) {
        MsdConfig.setShowInconclusiveResults(context, showInconclusive);
    }

    @Override
    public boolean getShowInconclusivePatchAnalysisTestResults(Context context) {
        return MsdConfig.getShowInconclusivePatchAnalysisTestResults(context);
    }

    @Override
    public String getPatchAnalysisNotificationSetting(Context context) {
        return MsdConfig.getPatchAnalysisNotificationSetting(context);
    }

    @Override
    public Class<?> getMainActivityClass() {
        return StartupActivity.class;
    }

    @Override
    public Class<?> getPatchAnalysisActivityClass() {
        return PatchalyzerMainActivity.class;
    }

    @Override
    public void homeUpButtonMainActivitiyCallback(Activity activity, MenuItem item) {
        if(item != null && item.getItemId() == android.R.id.home)
            activity.finish(); //TODO put old code from PatchalyzerMainActivity here
    }

    @Override
    public String getString(Context activity, String key) {
        return (String) activity.getResources().getString(activity.getResources().getIdentifier(key, "string", PACKAGE_NAME));
    }

    @Override
    public int getShowInconclusiveMenuItemID() {
        return R.id.menu_action_pa_inconclusive;
    }

    @Override
    public int getPatchAnalysisMenuId() {
        return R.menu.patch_analysis;
    }

    @Override
    public int getPatchalyzerLogoId() {
        return R.drawable.ic_patchalyzer;
    }

    @Override
    public int getPatchalyzerLayoutId() {
        return R.layout.activity_patchalyzer;
    }

    @Override
    public int getCustomButtonLayoutId() {
        return R.layout.custom_button;
    }

    @Override
    public int getTestButtonId() {
        return R.id.btnDoIt;
    }

    @Override
    public int getWebViewLayoutId() {
        return R.id.scrollViewTable;
    }

    @Override
    public int getErrorTextId() {
        return R.id.errorText;
    }

    @Override
    public int getPercentagTextId() {
        return R.id.textPercentage;
    }

    @Override
    public int getLegendWebViewId() {
        return R.id.legend;
    }

    @Override
    public int getMetaInfoViewId() {
        return R.id.scrollViewText;
    }

    @Override
    public int getProgressBarId() {
        return R.id.progressBar;
    }

    @Override
    public int getResultChartId() {
        return R.id.sumResultChart;
    }

    @Override
    public int getProgressBoxId() {
        return R.id.progress_box;
    }

    @Override
    public int getColor(Context context, String colorId) {
        return context.getResources().getColor(context.getResources().getIdentifier(colorId, "color", PACKAGE_NAME));

    }

    @Override
    public int[] getStyleIdResultChart() {
        return R.styleable.PatchalyzerSumResultChart;
    }

    @Override
    public int getStyleResultChart(String styleableId) {
        switch(styleableId){
            case "PatchalyzerSumResultChart_shownumbers":
                return R.styleable.PatchalyzerSumResultChart_shownumbers;
            case "PatchalyzerSumResultChart_small":
                return R.styleable.PatchalyzerSumResultChart_small;
            case "PatchalyzerSumResultChart_drawborder":
                return R.styleable.PatchalyzerSumResultChart_drawborder;
            default:
                return -1;
        }
    }


}
