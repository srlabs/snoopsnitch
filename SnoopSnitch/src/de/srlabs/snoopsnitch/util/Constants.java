package de.srlabs.snoopsnitch.util;

import android.text.format.DateUtils;

public class Constants {
	public static final String UPLOAD_URL = "https://gsmmap.srlabs.de:4433/cgi-bin/dat_upload.cgi";
	public static final String MULTIPART_BOUNDARY = "**********";
	public static final String CRLF = "\r\n";
	public static final long CONNECT_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
	public static final long READ_TIMEOUT = 30 * DateUtils.SECOND_IN_MILLIS;
	public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 1;
	public static final int NOTIFICATION_ID_INTERNAL_ERROR = 2;
	public static final int NOTIFICATION_ID_ERROR = 3;
	public static final int NOTIFICATION_ID_SMS = 4;
	public static final int NOTIFICATION_ID_IMSI = 5;
	public static final int NOTIFICATION_ID_EXPECTED_ERROR = 6;
	public static final long ANALYSIS_INTERVAL_MS = 60000;
	public static final int LOC_MAX_DELTA = 600;
	
	// Active test
	public static final String PREFS_KEY_OWN_NUMBER = "own_number";
	public static final String API_URL = "https://brest.srlabs.de:4443/clientCommandReceiver.php?Password=gdsajsdgkgdsalkgfdsgsdrw43435swds";
	public static final String CALL_NUMBER = "+14046206543"; // use '+' notation
	public static final String CALLBACK_NUMBER = "+14046206545";
	
	public static final long CALL_MT_TIMEOUT = 30000;
	public static final long CALL_MT_ACTIVE_TIMEOUT = 30000;
	public static final long CALL_MO_TIMEOUT = 20000;
	public static final long CALL_MO_ACTIVE_TIMEOUT = 10000;
	public static final long SMS_MT_TIMEOUT = 30000;
	public static final long API_TIMEOUT = 30000;
	public static final long SMS_MO_TIMEOUT = 10000;
}
