package de.srlabs.msd;

import android.text.format.DateUtils;

public class Constants {
	public static final String UPLOAD_URL = "https://gsmmap.srlabs.de:4433/cgi-bin/dat_upload.cgi";
	public static final String API_URL = "https://brest.srlabs.de:4443/clientCommandReceiver.php?Password=gdsajsdgkgdsalkgfdsgsdrw43435swds";
	public static final String MULTIPART_BOUNDARY = "**********";
	public static final String CRLF = "\r\n";
	public static final long CONNECT_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
	public static final long READ_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
}
