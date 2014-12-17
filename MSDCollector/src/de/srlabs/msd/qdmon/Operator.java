package de.srlabs.msd.qdmon;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;

class Operator {
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

		mcc = Integer.parseInt(networkOperator.substring(0,3));
		mnc = Integer.parseInt(networkOperator.substring(3));
		valid = true;
	}
}