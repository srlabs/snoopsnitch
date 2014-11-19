package de.srlabs.msd.util;

import android.text.format.DateUtils;

public class Constants {
	public static final String UPLOAD_URL = "https://gsmmap.srlabs.de:4433/cgi-bin/dat_upload.cgi";
	public static final String API_URL = "https://brest.srlabs.de:4443/clientCommandReceiver.php?Password=gdsajsdgkgdsalkgfdsgsdrw43435swds";
	public static final String MULTIPART_BOUNDARY = "**********";
	public static final String CRLF = "\r\n";
	public static final long CONNECT_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
	public static final long READ_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
	public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 1;
	public static final int NOTIFICATION_ID_INTERNAL_ERROR = 2;
	public static final int NOTIFICATION_ID_ERROR = 3;
	public static final int NOTIFICATION_ID_SMS = 4;
	public static final int NOTIFICATION_ID_IMSI = 5;
	public static final long ANALYSIS_INTERVAL_MS = 10000;
	
}
