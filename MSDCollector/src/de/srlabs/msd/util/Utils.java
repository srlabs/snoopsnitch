package de.srlabs.msd.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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
	public static String generateAppId(){
		// TODO
		return "";
	}

}
