package de.srlabs.patchalyzer;

import android.graphics.Color;

public class Constants {
    public static final String LOG_TAG="PatchAnalyzer";
    public static final int APP_VERSION = 10;
    public static final String APP_VERSION_TEXT = "v4";
    public static final String DEFAULT_APK_UPGRADE_URL = "https://snoopsnitch-api.srlabs.de/patchalyzer.apk";

    //testmode (decide whether we want to test files on OS or external files extracted to /sdcard/system)
    // can be useful when testing 64Bit builds on a 32Bit device
    public static final boolean IS_TEST_MODE=false;
    public static final String TEST_MODE_BASIC_TEST_FILE_PREFIX = "/sdcard";

    public static enum ActivityState {START, PATCHLEVEL_DATES, VULNERABILITY_LIST};

    //colors
    public static final int COLOR_INCONCLUSIVE=0xFF7575EC;
    public static final int COLOR_MISSING= Color.RED;
    public static final int COLOR_PATCHED=Color.GREEN;
    public static final int COLOR_NOTAFFECTED=Color.GRAY;
}
