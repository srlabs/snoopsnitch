package de.srlabs.msd.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import android.content.Context;


public class Utils {
	public static HttpsURLConnection openUrlWithPinning(Context context, String strUrl) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException{
		URL url = new URL(strUrl);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		final InputStream keystoreInputStream = context.getAssets().open("keystore.bks");

		final KeyStore keystore = KeyStore.getInstance("BKS");
		keystore.load(keystoreInputStream, "password".toCharArray());
		keystoreInputStream.close();

		final TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		tmf.init(keystore);

		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tmf.getTrustManagers(), null);

		((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
		return connection;
	}

	/**
	 * Generates a new random app ID. Currently the App id consists of 8
	 * hexadecimal digits generated based on the Android SecureRandom class.
	 * 
	 * @return
	 */
	public static String generateAppId(){
		SecureRandom sr = new SecureRandom();
		byte[] random = new byte[4];
		sr.nextBytes(random);
		return String.format("%02x%02x%02x%02x", random[0],random[1],random[2],random[3]);
	}
	public static String formatTimestamp(long millis){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = new Date(millis);
		return dateFormat.format(date);
	}

}
