package de.srlabs.snoopsnitch.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This class helps handling the dynamic user granted permissions introduced with API >= 23
 * <p>
 * ======================================================================
 * We are using 4 groups of Dangerous permissions:
 * ("normal" permissions we do not have to ask for)
 * -----------------------------------------------------------------------
 * Group             Dangerous permissions (we need)
 * -----------------------------------------------------------------------
 * LOCATION          ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
 * PHONE             READ_PHONE_STATE, CALL_PHONE
 * SMS               SEND_SMS, RECEIVE_SMS, READ_SMS
 * STORAGE           WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE
 * -----------------------------------------------------------------------
 * <p>
 * It is enough to request one dangerous permission out of each group, cause when the user grants one dangerous permission in a group
 * all the other dangerous permission out of this group will be granted automatically.
 * <p>
 * <p>
 * <p>
 * Please see detailed notes in:  AndroidManifest.xml
 * ======================================================================
 */

public class PermissionChecker {

    public static final int REQUEST_ACTIVE_TEST_PERMISSIONS = 1;
    public static final int REQUEST_MSDSERVICE_PERMISSIONS = 2;
    public static final int REQUEST_PCAP_EXPORT_PERMISSIONS = 3;
    public static final int REQUEST_NETWORK_ACTIVITY_PERMISSIONS = 4;


    /*Methods for checking, if certain permissions are granted currently*/


    /*Do we need to check for dynamic permissions at all?*/
    private static boolean isPermissionCheckingNeeded() {
        return (Build.VERSION.SDK_INT >= 23);
    }

    /**
     * Generice method for checking, if a certain permission is granted currently
     *
     * @param context
     * @param permission Permission to check for
     * @return true if user granted this permission, false if not
     */
    public static boolean isPermissionGranted(Context context, String permission) {
        if (!isPermissionCheckingNeeded())
            return true;
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // Location group----------
    public static boolean isAccessingFineLocationAllowed(Context context) {
        return isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean isAccessingCoarseLocationAllowed(Context context) {
        return isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    // ------------------------

    // Phone group ------------
    public static boolean isAccessingPhoneStateAllowed(Context context) {
        return isPermissionGranted(context, Manifest.permission.READ_PHONE_STATE);
    }

    public static boolean isCallingAllowed(Context context) {
        return isPermissionGranted(context, Manifest.permission.CALL_PHONE);
    }
    // ------------------------

    // SMS group --------------
    public static boolean isSendingSMSAllowed(Context context) {
        return isPermissionGranted(context, Manifest.permission.SEND_SMS);
    }
    // ------------------------

    // Storage group ----------
    public static boolean isWritingToExternalStorageAllowed(Context context) {
        return isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    // ------------------------


    /**
     * Check and dynamically request not yet granted permissions
     *
     * @param activity
     * @param permissions
     * @param requestID
     * @return true if all requested permissions were valid but already granted, false if either invalid permissions were requested,
     * or not all of the permissons are already granted
     */
    public static boolean checkAndRequestPermissions(Activity activity, List<String> permissions, int requestID) {

        if (permissions != null && permissions.size() > 0) {

            List<String> requestPermissions = new LinkedList<>();

            // only accept permission Strings mentioned in the AndroidManifest.xml
            permissions = filterValidPermissionStrings(activity, permissions);

            if (permissions != null && permissions.size() > 0) {
                for (String permission : permissions) {
                    if (!isPermissionGranted(activity, permission)) {
                        requestPermissions.add(permission);
                    }
                }
                if (!requestPermissions.isEmpty()) {
                    ActivityCompat.requestPermissions(activity, requestPermissions.toArray(new String[requestPermissions.size()]),
                            requestID);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Filter out the permission strings mentioned in the AndroidManifest.xml
     *
     * @param context
     * @param permissionStrings
     * @return
     */
    private static List<String> filterValidPermissionStrings(Context context, List<String> permissionStrings) {
        List<String> validPermissionStrings = new LinkedList<>();

        PackageManager packageManager = context.getPackageManager();
        try {
            //get Permissions mentioned in AndroidManifest.xml
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] manifestPermissions = packageInfo.requestedPermissions;
            List<String> listManifestPermissions = Arrays.asList(manifestPermissions);

            for (String permission : permissionStrings) { //check whether permissions are valid to ask for
                if (listManifestPermissions.contains(permission)) {
                    validPermissionStrings.add(permission);
                }
            }

            return validPermissionStrings;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Check if all permissions needed for an Active Test are granted, if not request them
     * -> results will be handled in onRequestPermissionsResult() in activity
     *
     * @param activity
     */
    public static boolean checkAndRequestPermissionsForActiveTest(Activity activity) {
        List<String> neccessaryPermissions = new LinkedList<String>();
        if (MsdConfig.gpsRecordingEnabled(activity))
            neccessaryPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        else
            neccessaryPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        neccessaryPermissions.add(Manifest.permission.CALL_PHONE);
        neccessaryPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
        neccessaryPermissions.add(Manifest.permission.SEND_SMS);
        neccessaryPermissions.add(Manifest.permission.RECEIVE_SMS);

        return checkAndRequestPermissions(activity, neccessaryPermissions, REQUEST_ACTIVE_TEST_PERMISSIONS);
    }

    public static boolean checkAndRequestPermissionForMsdService(Activity activity) {
        List<String> neccessaryPermissions = new LinkedList<>();
        neccessaryPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION); //neccessary for startPhoneStateRecording()
        neccessaryPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION); //neccessary for startPhoneStateRecording()
        neccessaryPermissions.add(Manifest.permission.READ_PHONE_STATE); // neccessary for getNetworkType()

        return checkAndRequestPermissions(activity, neccessaryPermissions, REQUEST_MSDSERVICE_PERMISSIONS);
    }

    public static boolean checkAndRequestPermissionForPCAPExport(Activity activity) {
        List<String> neccessaryPermissions = new LinkedList<>();
        neccessaryPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return checkAndRequestPermissions(activity, neccessaryPermissions, REQUEST_PCAP_EXPORT_PERMISSIONS);
    }

    public static boolean checkAndRequestPermissionsForNetworkActivity(Activity activity) {
        List<String> neccessaryPermissions = new LinkedList<>();
        neccessaryPermissions.add(Manifest.permission.READ_PHONE_STATE);

        return checkAndRequestPermissions(activity, neccessaryPermissions, REQUEST_NETWORK_ACTIVITY_PERMISSIONS);
    }

}
