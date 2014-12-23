package de.srlabs.snoopsnitch.qdmon;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.params.HttpParams;

import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

/**
 * This class provides a workaround for the missing SNI support of the HttpClient library included in Android.
 * Copied from http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache
 *
 */


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TlsSniSocketFactory implements LayeredSocketFactory {
    private static final String TAG = "davdroid.SNISocketFactory";

    final static HostnameVerifier hostnameVerifier = new StrictHostnameVerifier();


    // Plain TCP/IP (layer below TLS)

    @Override
    public Socket connectSocket(Socket s, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
            return null;
    }

    @Override
    public Socket createSocket() throws IOException {
            return null;
    }

    @Override
    public boolean isSecure(Socket s) throws IllegalArgumentException {
            if (s instanceof SSLSocket)
                    return ((SSLSocket)s).isConnected();
            return false;
    }


    // TLS layer

    @Override
    public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            if (autoClose) {
                    // we don't need the plainSocket
                    plainSocket.close();
            }

            // create and connect SSL socket, but don't do hostname/certificate verification yet
            SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
            SSLSocket ssl = (SSLSocket)sslSocketFactory.createSocket(InetAddress.getByName(host), port);

            // enable TLSv1.1/1.2 if available
            // (see https://github.com/rfc2822/davdroid/issues/229)
            ssl.setEnabledProtocols(ssl.getSupportedProtocols());

            // set up SNI before the handshake
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    Log.i(TAG, "Setting SNI hostname");
                    sslSocketFactory.setHostname(ssl, host);
            } else {
                    Log.d(TAG, "No documented SNI support on Android <4.2, trying with reflection");
                    try {
                         java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
                         setHostnameMethod.invoke(ssl, host);
                    } catch (Exception e) {
                            Log.w(TAG, "SNI not useable", e);
                    }
            }

            // verify hostname and certificate
            SSLSession session = ssl.getSession();
            if (!hostnameVerifier.verify(host, session))
                    throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);

            Log.i(TAG, "Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
                            " using " + session.getCipherSuite());

            return ssl;
    }
}
