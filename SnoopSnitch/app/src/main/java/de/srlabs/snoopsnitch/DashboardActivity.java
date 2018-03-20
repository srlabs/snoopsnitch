package de.srlabs.snoopsnitch;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import de.srlabs.snoopsnitch.active_test.ActiveTestCallback;
import de.srlabs.snoopsnitch.active_test.ActiveTestHelper;
import de.srlabs.snoopsnitch.active_test.ActiveTestResults;
import de.srlabs.snoopsnitch.analysis.Risk;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;
import de.srlabs.snoopsnitch.util.DeviceCompatibilityChecker;
import de.srlabs.snoopsnitch.util.MSDServiceHelperCreator;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.PermissionChecker;
import de.srlabs.snoopsnitch.util.Utils;
import de.srlabs.snoopsnitch.views.DashboardProviderChart;
import de.srlabs.snoopsnitch.views.DashboardThreatChart;
import de.srlabs.snoopsnitch.views.adapter.ListViewProviderAdapter;

public class DashboardActivity extends BaseActivity implements ActiveTestCallback {
    // Attributes
    private DashboardThreatChart layout;
    private ViewTreeObserver vto;
    private TextView txtSmsMonthCount;
    private TextView txtSmsWeekCount;
    private TextView txtSmsDayCount;
    private TextView txtSmsHourCount;
    private TextView txtImsiMonthCount;
    private TextView txtImsiWeekCount;
    private TextView txtImsiDayCount;
    private TextView txtImsiHourCount;
    private DashboardThreatChart dtcSmsHour;
    private DashboardThreatChart dtcSmsDay;
    private DashboardThreatChart dtcSmsWeek;
    private DashboardThreatChart dtcSmsMonth;
    private DashboardThreatChart dtcImsiHour;
    private DashboardThreatChart dtcImsiDay;
    private DashboardThreatChart dtcImsiWeek;
    private DashboardThreatChart dtcImsiMonth;
    private DashboardProviderChart pvcProviderInterception;
    private DashboardProviderChart pvcProviderImpersonation;
    private TextView txtLastAnalysisTime;
    private TextView txtDashboardLastAnalysis;
    private TextView txtDashboardInterception3g;
    private TextView txtDashboardInterception2g;
    private TextView txtDashboardImpersonation2g;
    private ListView lstDashboardProviderList;
    private Button btnDashboardNetworkTest;
    private Vector<Risk> providerList;
    Vector<TextView> threatSmsCounts;
    Vector<TextView> threatImsiCounts;
    private ActiveTestHelper activeTestHelper;
    private boolean unknownOperator = false;

    // Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        this.activeTestHelper = new ActiveTestHelper(this, this);

        txtSmsMonthCount = (TextView) findViewById(R.id.txtDashboardSilentSmsMonthCount);
        txtSmsWeekCount = (TextView) findViewById(R.id.txtDashboardSilentSmsWeekCount);
        txtSmsDayCount = (TextView) findViewById(R.id.txtDashboardSilentSmsDayCount);
        txtSmsHourCount = (TextView) findViewById(R.id.txtDashboardSilentSmsHourCount);
        txtImsiMonthCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherMonthCount);
        txtImsiWeekCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherWeekCount);
        txtImsiDayCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherDayCount);
        txtImsiHourCount = (TextView) findViewById(R.id.txtDashboardImsiCatcherHourCount);
        txtLastAnalysisTime = (TextView) findViewById(R.id.txtDashboardLastAnalysisTime);
        txtDashboardLastAnalysis = (TextView) findViewById(R.id.txtDashboardLastAnalysis);

        dtcSmsHour = (DashboardThreatChart) findViewById(R.id.SilentSMSChartHour);
        dtcSmsDay = (DashboardThreatChart) findViewById(R.id.SilentSMSChartDay);
        dtcSmsWeek = (DashboardThreatChart) findViewById(R.id.SilentSMSChartWeek);
        dtcSmsMonth = (DashboardThreatChart) findViewById(R.id.SilentSMSChartMonth);
        dtcImsiHour = (DashboardThreatChart) findViewById(R.id.IMSICatcherChartHour);
        dtcImsiDay = (DashboardThreatChart) findViewById(R.id.IMSICatcherChartDay);
        dtcImsiWeek = (DashboardThreatChart) findViewById(R.id.IMSICatcherChartWeek);
        dtcImsiMonth = (DashboardThreatChart) findViewById(R.id.IMSICatcherChartMonth);

        pvcProviderInterception = (DashboardProviderChart) findViewById(R.id.pvcDashboardInterception);
        pvcProviderImpersonation = (DashboardProviderChart) findViewById(R.id.pvcDashboardImpersonation);

        lstDashboardProviderList = (ListView) findViewById(R.id.lstDashboardProviderList);

        txtDashboardInterception3g = (TextView) findViewById(R.id.txtDashboardInterception3g);
        txtDashboardInterception2g = (TextView) findViewById(R.id.txtDashboardInterception2g);
        //txtDashboardImpersonation3g = (TextView) findViewById(R.id.txtDashboardImpersonation3g);
        txtDashboardImpersonation2g = (TextView) findViewById(R.id.txtDashboardImpersonation2g);

        btnDashboardNetworkTest = (Button) findViewById(R.id.btnDashboardTestNetwork);

        threatSmsCounts = new Vector<TextView>();
        threatSmsCounts.add(txtSmsHourCount);
        threatSmsCounts.add(txtSmsDayCount);
        threatSmsCounts.add(txtSmsWeekCount);
        threatSmsCounts.add(txtSmsMonthCount);

        threatImsiCounts = new Vector<TextView>();
        threatImsiCounts.add(txtImsiHourCount);
        threatImsiCounts.add(txtImsiDayCount);
        threatImsiCounts.add(txtImsiWeekCount);
        threatImsiCounts.add(txtImsiMonthCount);

        checkCompatibilityAndDisableFunctions();


    }

    private void checkCompatibilityAndDisableFunctions(){
        LinearLayout dashboardEventCharts = (LinearLayout) findViewById(R.id.dashboardChartSection);
        View toggleRecordingMenuItem = (View) findViewById(R.id.menu_action_scan);
        View activeTestAdvancedMenuItem = (View) findViewById(R.id.menu_action_active_test_advanced);

        String reason = DeviceCompatibilityChecker.checkDeviceCompatibility(this);
        if(reason != null){
            //SNSN features not fully accessible ; phone not compatible

            setViewAndChildrenEnabled(btnDashboardNetworkTest,false);
            setViewAndChildrenEnabled(dashboardEventCharts,false);
            setViewAndChildrenEnabled(toggleRecordingMenuItem, false); //FIXME toggleRecordingMenuItem == null
            setViewAndChildrenEnabled(activeTestAdvancedMenuItem, false);//FIXME activeTestAdvancedMenuItem == null
        }
        else{ //TODO necessary?
            setViewAndChildrenEnabled(btnDashboardNetworkTest,true);
            setViewAndChildrenEnabled(dashboardEventCharts,true);
            setViewAndChildrenEnabled(toggleRecordingMenuItem, true);
            setViewAndChildrenEnabled(activeTestAdvancedMenuItem, true);
        }
    }

    private static void setViewAndChildrenEnabled(View view, boolean enabled) {
        if(view == null)
            return;
        view.setEnabled(enabled);
        if(enabled)
            view.setAlpha(1.0f);
        else
            view.setAlpha(0.8f);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu _menu) {
        // Inflate the menu items for use in the action bar
        this.menu = _menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_action_scan);
        if (msdServiceHelperCreator.getMsdServiceHelper().isRecording()) {
            menuItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_menu_record_disable, null));
        } else {
            menuItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_menu_notrecord_disable, null));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();


        layout = (DashboardThreatChart) findViewById(R.id.SilentSMSChartMonth);
        vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                msdServiceHelperCreator.setRectWidth(layout.getMeasuredWidth() / 2);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get provider data
        this.providerList = msdServiceHelperCreator.getMsdServiceHelper().getData().getScores().getServerData();

        refreshView();

        fillProviderList();

        // Update RAT
        updateInterseptionImpersonation();
        updateLastAnalysis();

    }

    public void openDetailView(View view) {
        if (view.equals(findViewById(R.id.SilentSMSCharts)) || view.equals(findViewById(R.id.IMSICatcherCharts))) {
            Intent myIntent = new Intent(this, DetailChartActivity.class);
            myIntent.putExtra("ThreatType", view.getId());
            startActivity(myIntent);
        }
    }

    public void openLocalMapView(View view) {
        if (view.equals(findViewById(R.id.pvcDashboardInterception)) ||
                view.equals(findViewById(R.id.pvcDashboardImpersonation))) {
            Intent myIntent = new Intent(this, LocalMapActivity.class);
            startActivity(myIntent);
        }
    }

    @Override
    public void stateChanged(StateChangedReason reason) {

        if (reason.equals(StateChangedReason.CATCHER_DETECTED) || reason.equals(StateChangedReason.SMS_DETECTED)) {
            refreshView();
        } else if (reason.equals(StateChangedReason.ANALYSIS_DONE)) {
            updateLastAnalysis();
            refreshView();
        } else if (reason.equals(StateChangedReason.RAT_CHANGED)) {
            updateInterseptionImpersonation();
        } else if (reason.equals(StateChangedReason.NO_BASEBAND_DATA)) {
            txtLastAnalysisTime.setText(getString(R.string.compat_no_baseband_messages));
            txtLastAnalysisTime.setTextColor(ResourcesCompat.getColor(getResources(), R.color.common_chartRed, null));
            txtDashboardLastAnalysis.setVisibility(View.GONE);
        }

        super.stateChanged(reason);
    }

    @Override
    protected void refreshView() {
        checkOperator();

        // Redraw charts
        resetCharts();

        resetPoviderCharts();

        refreshProviderList();

        refreshPatchalyzerResultSum();

        // Set texts
        resetThreatCounts();
    }

    private void refreshPatchalyzerResultSum() {

    }

    private void checkOperator() {

        Risk risk = MSDServiceHelperCreator.getInstance().getMsdServiceHelper().getData().getScores();
        unknownOperator = risk.operatorUnknown();
    }

    private void resetThreatCounts() {
        txtSmsMonthCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsMonthSum().length));
        txtSmsWeekCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsWeekSum().length));
        txtSmsDayCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsDaySum().length));
        txtSmsHourCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsSmsHourSum().length));
        txtImsiMonthCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiMonthSum().length));
        txtImsiWeekCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiWeekSum().length));
        txtImsiDayCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiDaySum().length));
        txtImsiHourCount.setText(String.valueOf(msdServiceHelperCreator.getThreatsImsiHourSum().length));

        // Set text color of threat counts
        for (TextView tv : threatSmsCounts) {
            if (Integer.valueOf(tv.getText().toString()) > 0) {
                tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.common_chartYellow, null));
            } else {
                tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.common_chartGreen, null));
            }
        }

        for (TextView tv : threatImsiCounts) {
            if (Integer.valueOf(tv.getText().toString()) > 0) {
                tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.common_chartRed, null));
            } else {
                tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.common_chartGreen, null));
            }
        }
    }

    private void resetCharts() {
        dtcSmsHour.invalidate();
        dtcSmsDay.invalidate();
        dtcSmsWeek.invalidate();
        dtcSmsMonth.invalidate();
        dtcImsiHour.invalidate();
        dtcImsiDay.invalidate();
        dtcImsiWeek.invalidate();
        dtcImsiMonth.invalidate();
    }

    private void resetPoviderCharts() {
        pvcProviderImpersonation.invalidate();
        pvcProviderInterception.invalidate();
    }

    private void fillProviderList() {
        ListViewProviderAdapter providerAdapter = new ListViewProviderAdapter(this, providerList);
        lstDashboardProviderList.setAdapter(providerAdapter);
    }

    private void refreshProviderList() {
        lstDashboardProviderList.invalidate();
    }

    private void updateLastAnalysis() {
        // Set time of last measurement
        long lastAnalysisTime = 0;
        if (msdServiceHelperCreator.getMsdServiceHelper().isConnected()) {
            lastAnalysisTime = msdServiceHelperCreator.getMsdServiceHelper().getLastAnalysisTimeMs();
        }
        if (lastAnalysisTime > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(lastAnalysisTime);
            txtLastAnalysisTime.setText(String.valueOf(DateFormat.getDateTimeInstance().format(c.getTime())));
            txtLastAnalysisTime.setTextColor(ResourcesCompat.getColor(getResources(), R.color.common_text, null));
            txtDashboardLastAnalysis.setVisibility(View.VISIBLE);
        } else {
            txtDashboardLastAnalysis.setVisibility(View.GONE);
        }
    }

    private void updateInterseptionImpersonation() {
        switch (msdServiceHelperCreator.getMsdServiceHelper().getData().getCurrentRAT()) {
            case RAT_2G:
                txtDashboardInterception3g.setTypeface(Typeface.DEFAULT);
                txtDashboardInterception2g.setTypeface(Typeface.DEFAULT_BOLD);
                //txtDashboardImpersonation3g.setTypeface(Typeface.DEFAULT);
                txtDashboardImpersonation2g.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case RAT_3G:
                txtDashboardInterception3g.setTypeface(Typeface.DEFAULT_BOLD);
                txtDashboardInterception2g.setTypeface(Typeface.DEFAULT);
                //txtDashboardImpersonation3g.setTypeface(Typeface.DEFAULT_BOLD);
                txtDashboardImpersonation2g.setTypeface(Typeface.DEFAULT);
                break;
            case RAT_LTE:
                txtDashboardInterception3g.setTypeface(Typeface.DEFAULT);
                txtDashboardInterception2g.setTypeface(Typeface.DEFAULT);
                //txtDashboardImpersonation3g.setTypeface(Typeface.DEFAULT);
                txtDashboardImpersonation2g.setTypeface(Typeface.DEFAULT);
                break;
            case RAT_UNKNOWN:
                txtDashboardInterception3g.setTypeface(Typeface.DEFAULT);
                txtDashboardInterception2g.setTypeface(Typeface.DEFAULT);
                //txtDashboardImpersonation3g.setTypeface(Typeface.DEFAULT);
                txtDashboardImpersonation2g.setTypeface(Typeface.DEFAULT);
                break;
            default:
                txtDashboardInterception3g.setTypeface(Typeface.DEFAULT);
                txtDashboardInterception2g.setTypeface(Typeface.DEFAULT);
                //txtDashboardImpersonation3g.setTypeface(Typeface.DEFAULT);
                txtDashboardImpersonation2g.setTypeface(Typeface.DEFAULT);
                break;
        }
    }

    @Override
    public void handleTestResults(ActiveTestResults results) {
        ((TextView) findViewById(R.id.txtDashboardNetworkTest)).setText(results.getCurrentActionString(this.getApplicationContext()));
    }

    @Override
    public void testStateChanged() {
        if (activeTestHelper.isActiveTestRunning()) {
            btnDashboardNetworkTest.setText(getResources().getString(R.string.common_button_networktest_stop));
        } else {
            btnDashboardNetworkTest.setText(getResources().getString(R.string.common_button_networktest_start));
        }
    }

    public void toggleNetworkTest(View view) {
        if (activeTestHelper.isActiveTestRunning()) {
            activeTestHelper.stopActiveTest();
        } else {
            if (PermissionChecker.checkAndRequestPermissionsForActiveTest(this)) {
                activeTestHelper.showConfirmDialogAndStart(true);
            }
        }
    }

    @Override
    public void deviceIncompatibleDetected() {
        String incompatibilityReason = getResources().getString(R.string.compat_no_baseband_messages_in_active_test);
        Utils.showDeviceIncompatibleDialog(this, incompatibilityReason, new Runnable() {
            @Override
            public void run() {
                msdServiceHelperCreator.getMsdServiceHelper().stopRecording();
                quitApplication();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        MsdLog.d("DashboardActivity", "Received Permission request result; code: " + requestCode);
        if (requestCode == PermissionChecker.REQUEST_ACTIVE_TEST_PERMISSIONS) {
            if (grantResults.length > 0) {
                //find all neccessary permissions not granted
                List<String> notGrantedPermissions = new LinkedList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        notGrantedPermissions.add(permissions[i]);
                    }
                }

                if (notGrantedPermissions.isEmpty()) {
                    //Success: All neccessary permissions granted
                    // -> start Active Test!
                    //Log.i(TAG, "Active Test PERMISSIONS: ALL granted!");
                    activeTestHelper.showConfirmDialogAndStart(true);
                } else {

                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }

                    if (showDialog) {
                        showDialogAskingForAllPermissionsActiveTest(getResources().getString(R.string.alert_active_test_permissions_not_granted));
                    } else {
                        // IF permission is denied (and "never ask again" is checked)
                        // Log.e(TAG, mTAG + ": Permission FAILURE: some permissions are not granted. Asking again.");
                        showDialogPersistentDeniedPermissions(getResources().getString(R.string.alert_active_test_permissions_not_granted_persistent));
                    }

                }

            }
        } else if (requestCode == PermissionChecker.REQUEST_NETWORK_ACTIVITY_PERMISSIONS) {
            if (grantResults.length > 0) {
                //find all neccessary permissions not granted
                List<String> notGrantedPermissions = new LinkedList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        notGrantedPermissions.add(permissions[i]);
                    }
                }

                if (notGrantedPermissions.isEmpty()) {
                    //Success: All neccessary permissions granted
                    Intent intent = new Intent(this, NetworkInfoActivity.class);
                    startActivity(intent);
                } else {
                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }

                    if (showDialog) {
                        showDialogAskingForAllPermissionsNetworkInfo(getResources().getString(R.string.alert_network_activity_permissions_not_granted));
                    } else {
                        // IF permission is denied (and "never ask again" is checked)
                        // Log.e(TAG, mTAG + ": Permission FAILURE: some permissions are not granted. Asking again.");
                        showDialogPersistentDeniedPermissionsNetworkInfo(getResources().getString(R.string.alert_network_activity_permissions_not_granted_persistent));
                    }

                }

            }
        } else if (requestCode == PermissionChecker.REQUEST_MSDSERVICE_PERMISSIONS) {
            if (grantResults.length > 0) {
                //find all neccessary permissions not granted
                List<String> notGrantedPermissions = new LinkedList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        notGrantedPermissions.add(permissions[i]);
                    }
                }

                if (notGrantedPermissions.isEmpty()) {
                    //Success: All neccessary permissions granted
                    startRecording();
                } else {

                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }

                    if (showDialog) {
                        showDialogAskingForAllPermissionsMsdService(getResources().getString(R.string.alert_msdservice_permissions_not_granted));
                    } else {
                        // IF permission is denied (and "never ask again" is checked)
                        // Log.e(TAG, mTAG + ": Permission FAILURE: some permissions are not granted. Asking again.");
                        showDialogPersistentDeniedPermissions(getResources().getString(R.string.alert_msdservice_permissions_not_granted_persistent));
                    }

                }

            }
        }

    }

    private void showDialogAskingForAllPermissionsMsdService(String message) {
        MsdDialog.makeConfirmationDialog(this, message,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionChecker.checkAndRequestPermissionForMsdService(DashboardActivity.this);
                    }
                }, null, false).show();
    }


    private void showDialogAskingForAllPermissionsActiveTest(String message) {
        MsdDialog.makeConfirmationDialog(this, message,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionChecker.checkAndRequestPermissionsForActiveTest(DashboardActivity.this);
                    }
                }, null, false).show();
    }

    private void showDialogPersistentDeniedPermissions(String message) {
        /*TODO: Send user to permission settings for SNSN directly? Adapt message accordingly
                     startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
							 Uri.parse("package:de.srlabs.snoopsnitch")));*/
        MsdDialog.makeConfirmationDialog(this, message, null, null, false).show();

    }

    private void showDialogAskingForAllPermissionsNetworkInfo(String message) {
        MsdDialog.makeConfirmationDialog(this, message,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionChecker.checkAndRequestPermissionsForNetworkActivity(DashboardActivity.this);
                    }
                },
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(DashboardActivity.this, NetworkInfoActivity.class);
                        startActivity(intent);
                    }
                }, false).show();
    }

    private void showDialogPersistentDeniedPermissionsNetworkInfo(String message) {
        MsdDialog.makeConfirmationDialog(this, message,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(DashboardActivity.this, NetworkInfoActivity.class);
                        startActivity(intent);
                    }
                },
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(DashboardActivity.this, NetworkInfoActivity.class);
                        startActivity(intent);
                    }
                }, false).show();
    }

}