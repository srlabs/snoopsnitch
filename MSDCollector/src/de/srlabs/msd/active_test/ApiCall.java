package de.srlabs.msd.active_test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.MsdLog;
import de.srlabs.msd.util.Utils;

/**
 * @author Andreas Schildbach
 */
public abstract class ApiCall extends Thread {
	private static final String TAG = "msd-active-test-service-api";

	public enum Action {
		CALL, SMS, IGNORE
	}

	private final Action action;
	private final String number;
	private final Handler callbackHandler;
	private Context context;
	private boolean aborted = false;

	public ApiCall(final Action action, final String number, final Context context) {
		this.action = action;
		this.number = number;
		this.context = context;
		this.callbackHandler = new Handler(Looper.myLooper());
	}

	@Override
	public void run() {

		HttpURLConnection connection = null;

		try {

			final URL url = new URL(Constants.API_URL + "&client_MSISDN="
					+ URLEncoder.encode(number)
					+ "&requested_action="
					+ action.name().toLowerCase(Locale.US));

			MsdLog.i(TAG, "invoking api: " + url);
			final long start = System.currentTimeMillis();

			connection = Utils.openUrlWithPinning(context, url.toExternalForm());

			connection.setConnectTimeout((int) Constants.CONNECT_TIMEOUT);
			connection.setReadTimeout((int) Constants.READ_TIMEOUT);
			connection.setInstanceFollowRedirects(false);
			connection.connect();

			final int responseCode = connection.getResponseCode();
			final long duration = System.currentTimeMillis() - start;
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String tmp = connection.getResponseMessage();
				final String responseData;
				if(tmp.length() > 256)
					responseData = tmp.substring(0,256);
				else
					responseData = tmp;
				MsdLog.d(TAG, "API RESPONSE DATA: " + responseData);
				if(responseData.contains("all ok")){
					MsdLog.i(TAG, "invoking api succeeded, took " + duration + " ms");
					postOnSuccess();
				} else{
					MsdLog.i(TAG, "invoking api failed with invalid response data: " + responseData + ", took " +
							duration
							+ " ms");
					postOnFail();
				}
			} else {
				MsdLog.w(TAG, "Invalid API response code: " + responseCode + " " + connection.getResponseMessage());
				postOnFail();
			}
		} catch (final IOException x) {
			MsdLog.e(TAG, "error invoking api: " + x.getMessage());
			postOnFail();
		} catch (final GeneralSecurityException x) {
			MsdLog.e(TAG, "error invoking api: " + x.getMessage());
			postOnFail();
		} finally {
			connection.disconnect();
		}
	}
	private void postOnSuccess(){
		callbackHandler.post(new Runnable() {
			@Override
			public void run() {
				if(!aborted)
					onSuccess();
			}
		});
	}
	private void postOnFail(){
		callbackHandler.post(new Runnable() {
			@Override
			public void run() {
				if(!aborted)
					onFail();
			}
		});
	}
	protected abstract void onSuccess();

	protected abstract void onFail();
	public void abort(){
		this.aborted = true;
	}
}
