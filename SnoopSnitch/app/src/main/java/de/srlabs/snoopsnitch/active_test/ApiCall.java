package de.srlabs.snoopsnitch.active_test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.Utils;

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
	private String appId;

	public ApiCall(final Action action, final String number, final Context context) {
		this.action = action;
		this.number = number;
		this.context = context;
		this.appId = MsdConfig.getAppId(context);
		this.callbackHandler = new Handler(Looper.myLooper());
	}

	@Override
	public void run() {

		HttpURLConnection connection = null;

		try {
			final URL url = new URL(Constants.API_URL + "&client_MSISDN="
					+ URLEncoder.encode(number)
					+ "&requested_action="
					+ action.name().toLowerCase(Locale.US) + "&appid=" + URLEncoder.encode(this.appId));

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
				InputStream inputStream = connection.getInputStream();
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				int i;
				try {
					i = inputStream.read();
					while (i != -1)
					{
						byteArrayOutputStream.write(i);
						i = inputStream.read();
					}
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				String tmp = byteArrayOutputStream.toString();
				final String apiResponseData;
				if(tmp.length() > 256)
					apiResponseData = tmp.substring(0,256);
				else
					apiResponseData = tmp;
				String[] lines = apiResponseData.split("\\r?\\n"); // Accept both newline and CRLF
				if(lines.length < 2){
					postOnFail(null,"Invalid response data");
					return;
				}
				MsdLog.i(TAG,"API_LINE0: " + lines[0] + "  API_LINE1: " + lines[1]);
				String requestId = null;
				if(lines[0].startsWith("REQUEST_ID:")){
					requestId = lines[0].substring("REQUEST_ID:".length()).trim();
				} else{
					postOnFail(null,"No REQUEST_ID in first API line");
					return;
				}
				String apiStatus = null;
				if(lines[1].startsWith("STATUS:")){
					apiStatus = lines[1].substring("STATUS:".length()).trim();
				} else{
					postOnFail(null,"No STATUS in second API line");
					return;
				}
				if(apiStatus.equals("SUCCESS")){
					MsdLog.i(TAG, "invoking api " + action.name() + " succeeded, took " + duration + " ms");
					postOnSuccess(requestId);
				} else{
					MsdLog.i(TAG, "API returned status " + apiStatus);
					postOnFail(requestId,apiStatus);
				}
				MsdLog.i(TAG,"REQUEST_ID: " + requestId + "  STATUS: " + apiStatus);
			} else {
				MsdLog.w(TAG, "Invalid API response code: " + responseCode + " " + connection.getResponseMessage());
				postOnFail(null,"Invalid HTTP response code: " + responseCode);
			}
		} catch (final IOException x) {
			MsdLog.e(TAG, "error invoking api " + action.name() + ": " + x.getMessage());
			postOnFail(null,"IOException");
		} catch (final GeneralSecurityException x) {
			MsdLog.e(TAG, "error invoking api: " + x.getMessage());
			postOnFail(null,"GeneralSecurityException");
		} finally {
			connection.disconnect();
		}
	}
	private void postOnSuccess(final String requestId){
		callbackHandler.post(new Runnable() {
			@Override
			public void run() {
				if(!aborted){
					onSuccess(requestId);
				}
			}
		});
	}
	private void postOnFail(final String requestId, final String errorStr){
		callbackHandler.post(new Runnable() {
			@Override
			public void run() {
				if(!aborted)
					onFail(requestId, errorStr);
			}
		});
	}
	protected abstract void onSuccess(String requestId);

	protected abstract void onFail(String requestId, String errorStr);
	public void abort(){
		this.aborted = true;
	}
}
