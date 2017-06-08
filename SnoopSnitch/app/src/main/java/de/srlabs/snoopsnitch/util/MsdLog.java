package de.srlabs.snoopsnitch.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import de.srlabs.snoopsnitch.BuildConfig;
import de.srlabs.snoopsnitch.EncryptedFileWriterError;
import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.MsdService;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;

public class MsdLog {

    private static final String TAG = "SNSN";
    private static final String mTAG = "MsdLog";

    // TODO: We should use .getApplicationContext() when something points to a context
    private static MsdServiceHelper msdServiceHelper;
    private static MsdService msd;

    public static String getTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZZ", Locale.US);
        return format.format(c.getTime());
    }

    private static String getTimePrefix() {
        return getTime() + " ";
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        printlnToLog(getTimePrefix() + tag + ": INFO: " + msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        printlnToLog(getTimePrefix() + tag + ": ERROR: " + msg);
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
        printlnToLog(getTimePrefix() + tag + ": VERBOSE: " + msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        printlnToLog(getTimePrefix() + tag + ": DEBUG: " + msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        Log.d(tag, msg, tr);
        printlnToLog(getTimePrefix() + tag + ": DEBUG: " + msg + "\n" + Log.getStackTraceString(tr));
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        printlnToLog(getTimePrefix() + tag + ": ERROR: " + msg + "\n" + Log.getStackTraceString(tr));
    }

    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
        printlnToLog(getTimePrefix() + tag + ": WARNING: " + msg + "\n" + Log.getStackTraceString(tr));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        printlnToLog(getTimePrefix() + tag + ": WARNING: " + msg);
    }

    public static void init(MsdServiceHelper msdServiceHelper) {
        MsdLog.msdServiceHelper = msdServiceHelper;
    }

    public static void init(MsdService msd) {
        MsdLog.msd = msd;
    }

    private static void printlnToLog(String line) {
        if (msdServiceHelper != null) {
            msdServiceHelper.writeLog(line + "\n");
        } else if (msd != null) {
            try {
                msd.writeLog(line + "\n");
            } catch (EncryptedFileWriterError e) {
                throw new IllegalStateException("Error writing to log file: " + e.getMessage());
            }
        } else {
            throw new IllegalStateException("Please use MsdLog.init(context) before logging anything");
        }
    }

    /**
     * Check if a string is null or empty
     *
     * @param string
     * @return
     */
    private static boolean isBlank(String string) {
        return string == null || string.trim().length() == 0;
    }


    /**
     * Getting system properties using OS command getprop, instead of using reflection.
     * <p>
     * Reflection would require:
     * import com.android.internal.telephony.TelephonyProperties;
     * import android.os.SystemProperties;
     * //public static final String USER = Settings.System.getString("ro.build.user");
     *
     * @param key
     * @return property
     */
    public static String osgetprop(String key) {
        Process process = null;
        String property = null;
        try {
            String cmd[] = {"getprop", key};
            process = Runtime.getRuntime().exec(cmd);
            BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()));
            property = bis.readLine();
        } catch (IOException ee) {
            Log.e(TAG, mTAG + ": osgetprop(): Error executing getprop:\n" + ee.toString());
        }
        if (process != null)
            process.destroy();

        if (isBlank(property))
            return "<n/a>";

        return property;
    }

    /**
     * Collecting HW specific properties for global use/re-use without having to run
     * shell command every time.
     *
     * @return prop
     */
    public static String getDeviceProps() {
        // TODO: make this as an array, access once and make sure to refill when garbage collector removes MsdLog object
        String prop = "";
        try {
            prop = "Kernel version:        " + System.getProperty("os.version") + "\n"
                    + "gsm.version.baseband:  " + osgetprop("gsm.version.baseband") + "\n"
                    + "gsm.version.ril-impl:  " + osgetprop("gsm.version.ril-impl") + "\n"
                    + "ril.hw_ver:            " + osgetprop("ril.hw_ver") + "\n"
                    + "ril.modem.board:       " + osgetprop("ril.modem.board") + "\n"
                    + "ro.arch:               " + osgetprop("ro.arch") + "\n"
                    + "ro.board.platform:     " + osgetprop("ro.board.platform") + "\n";
        } catch (Exception ee) {
            Log.e(TAG, mTAG + "Exception in getDeviceProps(): Unable to retrieve system properties: " + ee);
            return "";
        }
        return prop;
    }

    /**
     * Gets some information about phone model, Android version etc.
     */
    public static String getLogStartInfo(Context context) {
        StringBuffer result = new StringBuffer();
        result.append("Log opened:          " + Utils.formatTimestamp(System.currentTimeMillis()) + "\n");
        result.append("SnoopSnitch Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")" + "\n");
        result.append("Android version:     " + Build.VERSION.RELEASE + "\n");
        result.append("Kernel version:      " + System.getProperty("os.version") + "\n");
        result.append("Manufacturer:        " + Build.MANUFACTURER + "\n");
        result.append("Board:               " + Build.BOARD + "\n");
        result.append("Brand:               " + Build.BRAND + "\n");
        result.append("Product:             " + Build.PRODUCT + "\n");
        result.append("Model:               " + Build.MODEL + "\n");
        result.append("Baseband:            " + Build.getRadioVersion() + "\n"); // Extra \n ?
        /*TODO: Each of the following lines call the shell...this can take time and is inefficient. Instead, all this should be put in some static parcel somewhere...*/
        result.append("gsm.version.baseband:  " + osgetprop("gsm.version.baseband") + "\n");
        result.append("gsm.version.ril-impl:  " + osgetprop("gsm.version.ril-impl") + "\n");
        result.append("ril.hw_ver:            " + osgetprop("ril.hw_ver") + "\n");
        result.append("ril.modem.board:       " + osgetprop("ril.modem.board") + "\n");
        result.append("ro.arch:               " + osgetprop("ro.arch") + "\n");
        result.append("ro.board.platform:     " + osgetprop("ro.board.platform") + "\n");
        result.append("/dev/diag info:\n	  " + Utils.checkDiag() + "\n");
        return result.toString();
    }
}
