package de.srlabs.patchalyzer;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;

import de.srlabs.patchalyzer.helpers.NotificationHelper;

/**
 * This interface helps declaring enviromental dependencies for certain app flavors
 * To define the usage of another AppFlavor you need to override the Constants.getAppFlavor() method and return the implementation of this class, which suits the environment
 */
public interface AppFlavor {

    public String getBinaryPath();

    public String setAppId(Context context);

    public void setShowInconclusiveResults(Context context, boolean showInconclusive);

    public boolean getShowInconclusivePatchAnalysisTestResults(Context context);

    public String getPatchAnalysisNotificationSetting(Context context);

    public Class<?> getMainActivityClass();

    public Class<?> getPatchAnalysisActivityClass();

    public void homeUpButtonMainActivitiyCallback(Activity activity, MenuItem item);

    public String getString(Context activity, String key);

    public int getShowInconclusiveMenuItemID();

    public int getPatchAnalysisMenuId();

    public int getPatchalyzerLogoId();

    public int getPatchalyzerLayoutId();

    public int getCustomButtonLayoutId();

    public int getTestButtonId();

    int getWebViewLayoutId();

    int getErrorTextId();

    int getPercentagTextId();

    int getLegendWebViewId();

    int getMetaInfoViewId();

    int getProgressBarId();

    int getResultChartId();

    int getProgressBoxId();

    int getColor(Context context, String colorId);

    int[] getStyleIdResultChart();

    int getStyleResultChart(String styleableId);
}
