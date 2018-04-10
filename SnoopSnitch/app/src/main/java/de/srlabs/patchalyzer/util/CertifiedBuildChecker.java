package de.srlabs.patchalyzer.util;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.safetynet.SafetyNetClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.snoopsnitch.BuildConfig;

/**Calls the Google SafetyNet API to check whether this build is certified or not.
 * Only works with available and up-to-date Google Play services.
 * For more details refer to:
 * https://developer.android.com/training/safetynet/attestation.html#check-gps-version
 *
 * Created by jonas on 10.04.18.
 */

public class CertifiedBuildChecker {
    public static final String API_KEY= BuildConfig.SAFETY_NET_API_KEY;
    private final Random secureRandom;
    private static String result;
    private static boolean ctsProfileMatch=false;
    private static boolean basicIntegrity=false;
    private static CertifiedBuildChecker instance;

    public static CertifiedBuildChecker getInstance(){
        if(instance == null){
            instance = new CertifiedBuildChecker();
        }
        return instance;
    }

    private CertifiedBuildChecker(){
        secureRandom = new SecureRandom();
    }

    public void startChecking(final Activity context){
        Thread checkThread = new Thread(){
            @Override
            public void run(){
                if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                        == ConnectionResult.SUCCESS) {
                    // we can do the test
                    String nonceData = "Noncenoncenonce: " + System.currentTimeMillis();
                    byte[] nonce = getRequestNonce(nonceData);

                    SafetyNetClient client = SafetyNet.getClient(context);
                    Task<SafetyNetApi.AttestationResponse> task = client.attest(nonce, API_KEY);
                    Log.d(Constants.LOG_TAG,"Sending SafetyNet request for this device...");
                    task.addOnSuccessListener(context, mSuccessListener)
                            .addOnFailureListener(context, mFailureListener);
                }
                else{
                    // not possible to test, as play services not available or up-to-date
                    // do nothing then
                    Log.d(Constants.LOG_TAG,"Not able to test for certified build, as Google Play services not available.");
                }
            }
        };
        checkThread.start();
    }


    private byte[] getRequestNonce(String data) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        try {
            byteStream.write(bytes);
            byteStream.write(data.getBytes());
        } catch (IOException e) {
            return null;
        }

        return byteStream.toByteArray();
    }


    /**
        * Called after successfully communicating with the SafetyNet API.
        * The #onSuccess callback receives an
         * {@link com.google.android.gms.safetynet.SafetyNetApi.AttestationResponse} that contains a
         * JwsResult with the attestation result.
     */
    private OnSuccessListener<SafetyNetApi.AttestationResponse> mSuccessListener =
            new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                @Override
                public void onSuccess(SafetyNetApi.AttestationResponse attestationResponse) {
                    /*
                     Successfully communicated with SafetyNet API.
                     */
                    result = attestationResponse.getJwsResult();
                    try {
                        Log.d(Constants.LOG_TAG, "SafetyNet result:\n" + result + "\n");
                        JSONObject resultJSON = new JSONObject(result);
                        if(resultJSON != null){
                            if(resultJSON.has("ctsProfileMatch")){
                                ctsProfileMatch = resultJSON.getBoolean("ctsProfileMatch");
                                Log.d(Constants.LOG_TAG,"ctsProfileMatch -> "+ctsProfileMatch);
                            }
                            if(resultJSON.has("basicIntegrity")){
                                basicIntegrity = resultJSON.getBoolean("basicIntegrity");
                                Log.d(Constants.LOG_TAG,"basicIntegrity -> "+basicIntegrity);
                            }
                        }
                    }catch(JSONException e){
                        Log.d(Constants.LOG_TAG,"JSONException when parsing JWS result from SafetyNet API",e);
                        result = null;
                    }
                }
            };

    /**
     * Called when an error occurred when communicating with the SafetyNet API.
     */
    private OnFailureListener mFailureListener = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            // An error occurred while communicating with the service.
            result = null;

            if (e instanceof ApiException) {
                // An error with the Google Play Services API contains some additional details.
                ApiException apiException = (ApiException) e;
                Log.d(Constants.LOG_TAG, "SafetyNet Error: " +
                        CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()) + ": " +
                        apiException.getStatusMessage());
            } else {
                // A different, unknown type of error occurred.
                Log.d(Constants.LOG_TAG, "SafetyNet Error:" + e.getMessage());
            }

        }
    };

    public boolean wasTestSuccesful(){
        return result != null;
    }

    public boolean getCtsProfileMatchResponse(){
        if(!wasTestSuccesful())
            return false;
        return ctsProfileMatch;
    }

    public boolean getBasicIntegrityResponse(){
        if(!wasTestSuccesful())
            return false;
        return basicIntegrity;
    }
}
