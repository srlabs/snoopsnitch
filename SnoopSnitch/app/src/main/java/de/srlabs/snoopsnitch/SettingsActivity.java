package de.srlabs.snoopsnitch;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;

import java.util.LinkedList;
import java.util.List;

import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.PermissionChecker;
import de.srlabs.snoopsnitch.util.Utils;

public class SettingsActivity extends PreferenceActivity {
    private SettingsFragment settingsFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsFragment = new SettingsFragment();

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .commit();

        setTitle(getResources().getText(R.string.settings_actionBar_title));
    }

    public void refreshAppId() {
        MsdConfig.setAppId(this, Utils.generateAppId());
    }

    public void disablePCAPExport() {
        MsdLog.d("SNSN: Settings", "PCAP export is not allowed. Disabling it again.");
        settingsFragment.disablePCAPExport();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PermissionChecker.REQUEST_PCAP_EXPORT_PERMISSIONS) {
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
                    //PCAP export is allowed!

                } else {
                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }

                    if (showDialog) {
                        showDialogAskingForAllPermissions(getResources().getString(R.string.alert_pcap_export_permissions_not_granted));
                    } else {
                        // IF permission is denied (and "never ask again" is checked)
                        // Log.e(TAG, mTAG + ": Permission FAILURE: some permissions are not granted. Asking again.");
                        showDialogPersistentDeniedPermissions(getResources().getString(R.string.alert_pcap_export_permissions_not_granted_persistent));
                        disablePCAPExport();
                    }

                }
            }
        }
    }

    private void showDialogAskingForAllPermissions(String message) {
        MsdDialog.makeConfirmationDialog(this, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PermissionChecker.checkAndRequestPermissionForPCAPExport(SettingsActivity.this);
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                disablePCAPExport();
            }
        }, false).show();
    }

    private void showDialogPersistentDeniedPermissions(String message) {
        /*TODO: Send user to permission settings for SNSN directly? Adapt message accordingly
         startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
				 Uri.parse("package:de.srlabs.snoopsnitch")));*/
        MsdDialog.makeConfirmationDialog(this, message, null, null, false).show();
    }
}
