package de.srlabs.snoopsnitch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import de.srlabs.patchalyzer.AppFlavor;
import de.srlabs.patchalyzer.PatchalyzerMainActivity;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.Utils;

public class PAAppFlavorSNSN extends AppFlavor {

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
        if(item != null && item.getItemId() == android.R.id.home) {
            Intent upIntent;
            if (StartupActivity.isAppInitialized()) {
                upIntent = NavUtils.getParentActivityIntent(activity);
            } else {
                // StartupActivity needs to run before we can start DashboardActivity
                upIntent = new Intent(activity, StartupActivity.class);
            }
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(upIntent);
            activity.finish();
        }
    }

    @Override
    public String getPatchalyzerActivityLabel() {
        return "Android patch level analysis";
    }

}
