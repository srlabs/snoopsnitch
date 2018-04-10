package de.srlabs.snoopsnitch;

import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.content.res.ResourcesCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.srlabs.patchalyzer.PatchalyzerMainActivity;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.PermissionChecker;
import de.srlabs.snoopsnitch.util.Utils;

public class BaseActivity extends FragmentActivity {
    private static final String TAG = "SNSN: BaseActivity";
    // Attributes
    protected MSDServiceHelperCreator msdServiceHelperCreator;
    protected TextView messageText;
    protected View messageLayout;
    protected Toast messageToast;
    protected Menu menu;
    protected Boolean isInForeground = false;
    protected Handler handler;
    private Intent patchalyzerIntent;
    protected final int refresh_intervall = 1000;
    // Static variable so that it is common to all Activities of the App
    private static boolean exitFlag = false;
    protected String snsnIncompatibilityReason=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        messageLayout = inflater.inflate(R.layout.custom_message_popdown,
                (ViewGroup) findViewById(R.id.toast_layout_root));
        messageText = (TextView) messageLayout.findViewById(R.id.text);
        messageToast = new Toast(getApplicationContext());

        // Get MsdService Helper
        boolean autoStartRecording = StartupActivity.isSNSNCompatible() && (PermissionChecker.isAccessingFineLocationAllowed(this) || PermissionChecker.isAccessingCoarseLocationAllowed(this));
        msdServiceHelperCreator = MSDServiceHelperCreator.getInstance(this.getApplicationContext(), autoStartRecording);
        MsdLog.init(msdServiceHelperCreator.getMsdServiceHelper());
        MsdLog.i("MSD", "MSD_ACTIVITY_CREATED: " + getClass().getCanonicalName());

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        if (exitFlag) {
            finish();
            System.exit(0);
            return;
        }
        msdServiceHelperCreator.setCurrentActivity(this);

        isInForeground = true;
        // Set title/subtitle of the action bar...
        ActionBar ab = getActionBar();

        ab.setTitle(R.string.actionBar_title);
        ab.setSubtitle(getResources().getText(R.string.actionBar_subTitle) + " " +setAppId(this));

        handler.postDelayed(runnable, refresh_intervall);

        setRecordingIcon();

        super.onResume();
    }

    @Override
    protected void onPause() {
        isInForeground = false;
        handler.removeCallbacks(runnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void showMap() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    protected void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    protected void showAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    protected void showNetworkInfo() {
        if (!PermissionChecker.isAccessingPhoneStateAllowed(this)) {
            MsdLog.w(TAG, "Showing only partial NetworkInfo allowed.");
            PermissionChecker.checkAndRequestPermissionsForNetworkActivity(this);
        } else {
            Intent intent = new Intent(this, NetworkInfoActivity.class);
            startActivity(intent);
        }
    }

    protected void toggleRecording() {
        Boolean isRecording = msdServiceHelperCreator.getMsdServiceHelper().isRecording();

        if (isRecording) {
            stopRecording();
        } else {
            if (PermissionChecker.checkAndRequestPermissionForMsdService(this))
                startRecording();
        }
    }

    protected void startRecording() {
        msdServiceHelperCreator.getMsdServiceHelper().startRecording();
    }

    protected void stopRecording() {
        msdServiceHelperCreator.getMsdServiceHelper().stopRecording();
    }

    public MSDServiceHelperCreator getMsdServiceHelperCreator() {
        return msdServiceHelperCreator;
    }

    public void showPatchalyzer(){
        patchalyzerIntent = new Intent(this, PatchalyzerMainActivity.class);
        startActivity(patchalyzerIntent);
    }

    public void disableSNSNSpecificFunctionality(String snsnIncompatibilityReason){
        this.snsnIncompatibilityReason = snsnIncompatibilityReason;
    }

    public void enableSNSNSpecificFunctionality(){
        this.snsnIncompatibilityReason = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.label_patch_analysis_long:
                showPatchalyzer();
                break;
            case R.id.menu_action_scan:
                if(snsnIncompatibilityReason == null) {
                    toggleRecording();
                }
                else{
                    showSNSNFeaturesNotWorkingDialog(snsnIncompatibilityReason);
                }
                break;
            case R.id.menu_action_map:
                showMap();
                break;
            case R.id.menu_action_active_test_advanced:
                if(snsnIncompatibilityReason == null) {
                    Intent intent = new Intent(this, ActiveTestAdvanced.class);
                    startActivity(intent);
                }else{
                    showSNSNFeaturesNotWorkingDialog(snsnIncompatibilityReason);
                }
                break;
            case R.id.menu_action_upload_pending_files:
                getMsdServiceHelperCreator().getMsdServiceHelper().triggerUploading();
                break;
            case R.id.menu_action_upload_debug_logs:
                Intent intent2 = new Intent(this, UploadDebugActivity.class);
                startActivity(intent2);
                break;
            case R.id.menu_action_settings:
                showSettings();
                break;
            case R.id.menu_action_about:
                showAbout();
                break;
            case R.id.menu_action_exit:
                quitApplication();
                break;
            case R.id.menu_action_network_info:
                showNetworkInfo();
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                break;
            default:
                MsdLog.e("BaseActivity", "Invalid menu entry pressed,  id=" + item.getItemId());
                break;
        }

        return true;
    }

    private void showMessage(String message) {
        if (isInForeground) {
            messageText.setText(message);
            messageToast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, getActionBar().getHeight());
            messageToast.setDuration(Toast.LENGTH_LONG);
            messageToast.setView(messageLayout);
            messageToast.show();
        }
    }

    public void internalError(String errorMsg) {
        MsdDialog.makeFatalConditionDialog(this, "A fatal error occured!", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quitApplication();
            }
        }, errorMsg, false).show();
    }

    public void stateChanged(StateChangedReason reason) {
        if (reason.equals(StateChangedReason.RECORDING_STATE_CHANGED)) {
            if (menu != null) {
                MenuItem menuItem = menu.findItem(R.id.menu_action_scan);
                if (msdServiceHelperCreator.getMsdServiceHelper().isRecording()) {
                    menuItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_menu_record_disable, null));
                    showMessage(getResources().getString(R.string.message_recordingStarted));
                } else {
                    menuItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_menu_notrecord_disable, null));
                    showMessage(getResources().getString(R.string.message_recordingStopped));
                }
            }
        }
    }

    public static String setAppId(Context context) {
        if (MsdConfig.getAppId(context).equals("")) {
            MsdConfig.setAppId(context, Utils.generateAppId());
        }
        return MsdConfig.getAppId(context);
    }

    protected Runnable runnable = new Runnable() {
        @Override
        public void run() {
            /* do what you need to do */
            refreshView();
            /* and here comes the "trick" */
            handler.postDelayed(runnable, refresh_intervall);
        }
    };

    protected void refreshView() {
    }

    private void setRecordingIcon() {
        if (menu != null) {
            MenuItem menuItem = menu.findItem(R.id.menu_action_scan);
            if (msdServiceHelperCreator.getMsdServiceHelper().isRecording()) {
                menuItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_menu_record_disable, null));
            } else {
                menuItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_menu_notrecord_disable, null));
            }
        }
    }

    protected void quitApplication() {
        MsdLog.i("MSD", "BaseActivity.quitApplication() called");
        msdServiceHelperCreator.getMsdServiceHelper().stopRecording();
        msdServiceHelperCreator.getMsdServiceHelper().stopService();
        // If we call System.exit() here from an activity launched by
        // DashboardActivity, the Android system will restart the App to resume
        // DashboardActivity (which is still on the activity stack). So
        // System.exit() has to be called from onResume() in DashboardActivity
        // instead. This is implemented via exitFlag, which is a static variable
        // of BaseActivity.
        exitFlag = true;
        finish();
        if (this.getClass() == DashboardActivity.class) {
            System.exit(0);
        }
    }

    public void showSNSNFeaturesNotWorkingDialog(String snsnIncompatibilityReason){
        if(snsnIncompatibilityReason.equals(getResources().getString(R.string.compat_no_baseband_messages_in_active_test))){
            showDialogWarningNoBasebandMessages();
        }
        else {
            showDeviceIncompatibleDialog(snsnIncompatibilityReason);
        }
    }

    public void showDialogWarningNoBasebandMessages(){
        MsdDialog.makeConfirmationDialog(this, getResources().getString(R.string.compat_no_baseband_messages_warning),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing here
                    }
                },
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        quitApplication();
                    }
                },
                null,
                getResources().getString(R.string.warning_button_proceed_anyway),
                getResources().getString(R.string.warning_button_quit),
                false
        ).show();
    }

    public void showDeviceIncompatibleDialog(String incompatibilityReason) {
        Utils.showDeviceIncompatibleDialog(this, incompatibilityReason+"\n"+this.getResources().getString(R.string.compat_snsn_features_not_working), new Runnable() {
            @Override
            public void run() {
                //do nothing here
            }
        });
    }

}
