package de.srlabs.snoopsnitch.qdmon;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Operator {
	int mcc;
	int mnc;
	boolean valid = false;

	public int getMcc() {
		return mcc;
	}

	public int getMnc() {
		return mnc;
	}

	public boolean isValid() {
		return valid;
	}

	public Operator(Context context) {

	    TelephonyManager mTelephonyManager = (TelephonyManager)
	            context.getSystemService(Context.TELEPHONY_SERVICE);
	    String networkOperator = mTelephonyManager.getNetworkOperator();

		if(networkOperator.length() < 5) {
			return;
		}
		// Some Dual SIM phones return a comma-separated list of network operators.
		if(networkOperator.indexOf(',') > 0){
			networkOperator = networkOperator.substring(0,networkOperator.indexOf(','));
		}

		mcc = Integer.parseInt(networkOperator.substring(0,3));
		mnc = Integer.parseInt(networkOperator.substring(3));
		valid = true;
	}

	public Operator(int currentMcc, int currentMnc) {
		mcc = currentMcc;
		mnc = currentMnc;
		valid = true;
	}
}