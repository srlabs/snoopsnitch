package de.srlabs.snoopsnitch;

import java.text.DateFormat;
import java.util.Calendar;

import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.PermissionChecker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class NetworkInfoActivity extends BaseActivity {

	private final String TAG="SNSN: NetworkActivity";
	protected final int refreshInterval = 60 * 1000;
	private Context context;
	private Handler handler = new Handler();
	private NetworkInfoRunnable networkInfoRunnable = new NetworkInfoRunnable();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_network_info);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		context = this.getApplicationContext();
		updateNetworkInfo();
		handler.postDelayed(networkInfoRunnable, refreshInterval);
	}

	@Override
	protected void onResume() 
	{
		updateNetworkInfo();
		handler.postDelayed(networkInfoRunnable, refreshInterval);
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		handler.removeCallbacks(networkInfoRunnable);
		super.onPause();
	}


	class NetworkInfoRunnable implements Runnable{
		@Override
		public void run() 
		{
			updateNetworkInfo();
			handler.postDelayed(this, refreshInterval);
		}
	};


	private void updateNetworkInfo() {

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();

		//  TMSI
		setCurrentTMSI(db);

		//  USIM
		setUSIM(db);
		
		//  Set transaction
		if(PermissionChecker.isAccessingPhoneStateAllowed(context)) {
			setTransaction(db);
		}
		else{
			MsdLog.w(TAG,"Setting Transaction information not allowed - User did not grant READ_PHONE_STATE permission.");
		}
		
		MsdDatabaseManager.getInstance().closeDatabase();
	}

	private void setCurrentTMSI(SQLiteDatabase db) {

		String tmsiText = "-";
		TextView tmsi = (TextView) findViewById(R.id.networkInfoCurrentTmsi);

		Cursor query = db.rawQuery
				("SELECT max(id), ifnull(ifnull(new_tmsi, tmsi), '???') FROM session_info " +
		         "WHERE domain = 0 AND (tmsi NOT NULL OR new_tmsi NOT NULL) ORDER BY id", null);

		if(query.moveToFirst())
		{
			tmsiText = query.getString(1);
		}
		tmsi.setText(tmsiText);
	}

	private void setUSIM(SQLiteDatabase db) {

		String usimText = context.getResources().getString(R.string.network_info_usim_unknown);
		TextView usim = (TextView) findViewById(R.id.networkInfoCurrentUSIM);

		Cursor query = db.rawQuery
				("SELECT ifnull(max(auth), 0) FROM session_info WHERE domain = 0 AND auth > 0 AND (rat = 1 OR auth > 1)", null);

		if(query.moveToFirst())
		{
			int auth = query.getInt(0);
			switch (auth)
			{
				case 1:
					// Only authentications with A3/A8
					usimText = context.getResources().getString(R.string.network_info_usim_not_present);
					break;
				case 2:
					// UMTS AKA authentications happened
					usimText = context.getResources().getString(R.string.network_info_usim_present);
					break;
			}
		}

		usim.setText(usimText);
	}
	
	private void setTextView(int ID, String value) {
		TextView view = (TextView) findViewById(ID);
		if (value == null)
		{
			view.setText("-");
		} else
		{
			view.setText(value);
		}
		view.invalidate();
	}

	private void setVisibility(boolean visible, int ID) {
		TextView view = (TextView) findViewById(ID);
		view.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	private void setVisibility2(boolean visible, int ID1, int ID2) {
		setVisibility(visible, ID1);
		setVisibility(visible, ID2);
	}

	private String toAuthString(int value) {
		switch (value) {
			case 0:  return context.getResources().getString(R.string.common_none);
			case 1:  return "GSM A3/A8";
			case 2:  return "UMTS AKA";
			default: return "-";
		}
	}
	
	private String toRATString(int value) {
		switch (value) {
			case 0: return "2G";
			case 1: return "3G";
			case 2: return "LTE";
			default: return "unknown";
		}
	}

	private String toCipherString(int rat, int value) {
		if (value > 4) {
			return "-";
		}
		switch (rat) {
			case 0: // 2G
				return "A5/" + Integer.toString(value);
			case 1: // 3G
				return "UEA/" + Integer.toString(value);
			case 2: // LTE
				return "EEA/" + Integer.toString(value);
			default:
				return context.getResources().getString(R.string.common_invalid);
		}
	}

	private String toGprsCipherString(int value){
		return "GEA/" + value;
	}

	private String toIntegrityString(int rat, int ia) {
		switch (rat) {
			case 0: // 2G
				return "- (" + ia + ")";
			case 1: // 3G
				return "UIA/" + ia;
			case 2: // LTE
				return "EIA/" + ia;
			default:
				return context.getResources().getString(R.string.common_invalid);
		}
	}

	private String toDirectionString(int mo, int mt) {
		if (mo > 0 && mt > 0)
		{
			return context.getResources().getString(R.string.common_invalid) + "(MO+MT)";
		} else if (mt > 0)
		{
			return context.getResources().getString(R.string.network_info_mobile_terminated);
		} else if (mo > 0)
		{
			return context.getResources().getString(R.string.network_info_mobile_originated);
		} else
		{
			return context.getResources().getString(R.string.common_none);
		}
	}

	private String toPagingString(int mi) {
		switch (mi) {
			case 0:  return null; 
			case 1:  return "IMSI";
			case 2:  return "IMEI";
			case 3:  return "IMEISV";
			case 4:  return "TMSI";
			default: return context.getResources().getString(R.string.common_invalid);
		}
	}
	
	private String toTypeString(int locupd, int abort, int call, int sms) {

		String result = "";

		if (locupd > 0) {
			result += "location update ";
		}

		if (abort > 0) {
			result += "aborted ";
		}

		if (call > 0) {
			result += "call ";
		}

		if (sms > 0) {
			result += "sms ";
		}
		return result;
	}

	private String toLUResultString(int lu_acc, int lu_reject) {
		if (lu_acc > 0 && lu_reject > 0)
		{
			return context.getResources().getString(R.string.common_invalid);
		} else if (lu_acc > 0)
		{
			return context.getResources().getString(R.string.network_info_lu_accepted);
		} else if (lu_reject > 0)
		{
			return context.getResources().getString(R.string.network_info_lu_rejected);
		} else
		{
			return null;
		}
	}
	
	private String toLUTypeString(int lu_type) {
		switch (lu_type)
		{
			case 0:  return "normal";
			case 1:  return "periodic";
			case 2:  return "IMSI attach";
			case 3:  return "reserved";
			default: return "-";
		}
	}

	/**
	 * REQUIRED PERMISSION:
	 * 	READ_PHONE_STATE
	 */
	private void setTransaction(SQLiteDatabase db) {
		
		long timestamp;
		int rat, mcc, mnc, lac, cid, rnc, arfcn;
		int auth, cipher, integrity, duration;
		int mo, mt, paging_mi, t_locupd, lu_acc, lu_type, lu_reject, rej_cause;
		int lu_mcc, lu_mnc, lu_lac, t_abort, t_call, t_sms, id;
		int cipher_gprs=0;
		boolean gprs_cipher_info_available = false;
		String msisdn, IMSI;
		TelephonyManager telephonyManager;

		String q =
				"SELECT max(id) as id, strftime('%s', timestamp) as timestamp, " +
				"rat, mcc, mnc, lac, cid, arfcn, auth, cipher, integrity, " +
				"duration, mobile_orig, mobile_term, paging_mi, " +
				"t_locupd, lu_acc, lu_type, lu_reject, lu_rej_cause, " +
				"lu_mcc, lu_mnc, lu_lac, t_abort, t_call, t_sms, msisdn " +
				"from session_info where domain = 0 order by timestamp";

		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		IMSI = telephonyManager.getSubscriberId();

		Cursor query = db.rawQuery (q, null);
		if(query.moveToFirst())
		{
			timestamp = query.getLong(query.getColumnIndexOrThrow("timestamp"));
			id        = query.getInt(query.getColumnIndexOrThrow("id"));
			rat       = query.getInt(query.getColumnIndexOrThrow("rat"));
			mcc       = query.getInt(query.getColumnIndexOrThrow("mcc"));
			mnc       = query.getInt(query.getColumnIndexOrThrow("mnc"));
			lac       = query.getInt(query.getColumnIndexOrThrow("lac"));
			cid       = query.getInt(query.getColumnIndexOrThrow("cid"));
			rnc = cid >> 16;
			arfcn       = query.getInt(query.getColumnIndexOrThrow("arfcn"));
			auth      = query.getInt(query.getColumnIndexOrThrow("auth"));
			cipher    = query.getInt(query.getColumnIndexOrThrow("cipher"));
			integrity = query.getInt(query.getColumnIndexOrThrow("integrity"));
			duration  = query.getInt(query.getColumnIndexOrThrow("duration"));
			mo        = query.getInt(query.getColumnIndexOrThrow("mobile_orig"));
			mt        = query.getInt(query.getColumnIndexOrThrow("mobile_term"));
			paging_mi = query.getInt(query.getColumnIndexOrThrow("paging_mi"));
			t_locupd  = query.getInt(query.getColumnIndexOrThrow("t_locupd"));
			t_abort   = query.getInt(query.getColumnIndexOrThrow("t_abort"));
			t_call    = query.getInt(query.getColumnIndexOrThrow("t_call"));
			t_sms     = query.getInt(query.getColumnIndexOrThrow("t_sms"));
			msisdn    = query.getString(query.getColumnIndexOrThrow("msisdn"));
			lu_acc    = query.getInt(query.getColumnIndexOrThrow("lu_acc"));
			lu_type   = query.getInt(query.getColumnIndexOrThrow("lu_type"));
			lu_reject = query.getInt(query.getColumnIndexOrThrow("lu_reject"));
			rej_cause = query.getInt(query.getColumnIndexOrThrow("lu_rej_cause"));
			lu_mcc    = query.getInt(query.getColumnIndexOrThrow("lu_mcc"));
			lu_mnc    = query.getInt(query.getColumnIndexOrThrow("lu_mnc"));
			lu_lac    = query.getInt(query.getColumnIndexOrThrow("lu_lac"));

			if(rat == 0){ // Query GPRS Cipher when using 2G
				String q_gprs =
						"SELECT max(id) as id, strftime('%s', timestamp) as timestamp, " +
						"cipher from session_info where rat = 0 and domain = 1 " +
						"and t_attach and att_acc order by timestamp";
				Cursor query_gprs = db.rawQuery (q_gprs, null);
				if(query_gprs.moveToFirst()){
					long gprs_timestamp = query_gprs.getLong(query_gprs.getColumnIndexOrThrow("timestamp"));
					// Only show GPRS session if time difference between GSM session and GPRS session is below 24h
					if(Math.abs(timestamp - gprs_timestamp) < 24*3600){
						cipher_gprs = query_gprs.getInt(query_gprs.getColumnIndexOrThrow("cipher"));
						gprs_cipher_info_available = true;
					}
				}
			}

			// Set timestamp
			setTextView(R.id.networkInfoTimestamp, String.valueOf(DateFormat.getDateTimeInstance().format(timestamp * 1000L)));
			setTextView(R.id.networkInfoCurrentInternalID, Integer.toString(id));
			setTextView(R.id.networkInfoCurrentRAT, toRATString(rat));
			setTextView(R.id.networkInfoCurrentMCC, Integer.toString(mcc));
			setTextView(R.id.networkInfoCurrentMNC, Integer.toString(mnc));
			setTextView(R.id.networkInfoCurrentLAC, Integer.toString(lac));
			setTextView(R.id.networkInfoCurrentCID, Integer.toString(cid));
			setTextView(R.id.networkInfoCurrentRNC, Integer.toString(rnc));
			setTextView(R.id.networkInfoCurrentARFCN, Integer.toString(arfcn));
			setTextView(R.id.networkInfoCurrentAuth, toAuthString(auth));
			setTextView(R.id.networkInfoCurrentCipher, toCipherString(rat, cipher));

			// GPRS cipher
			gprs_cipher_info_available = false; // Disable display of Cipher (GPRS) for now
			setVisibility2(gprs_cipher_info_available, R.id.networkInfoCurrentCipherGprs,R.id.txtCipherGprs);
			if(gprs_cipher_info_available){
				setTextView(R.id.networkInfoCurrentCipherGprs, toGprsCipherString(cipher_gprs));
			}

			setTextView(R.id.networkInfoCurrentIntegrity, toIntegrityString(rat, integrity));
			setTextView(R.id.networkInfoCurrentDuration, Integer.toString(duration) + " ms");
			setTextView(R.id.networkInfoCurrentDirection, toDirectionString(mo, mt));
			setTextView(R.id.networkInfoCurrentPaging, toPagingString(paging_mi));
			setTextView(R.id.networkInfoCurrentType, toTypeString(t_locupd, t_abort, t_call, t_sms));
			setTextView(R.id.networkInfoCurrentMSISDN, msisdn);

			setTextView(R.id.networkInfoPreviousMCC, Integer.toString(lu_mcc));
			setTextView(R.id.networkInfoPreviousMNC, Integer.toString(lu_mnc));
			setTextView(R.id.networkInfoPreviousLAC, Integer.toString(lu_lac));
			setTextView(R.id.networkInfoLUResult, toLUResultString(lu_acc, lu_reject));
			setTextView(R.id.networkInfoLUType, toLUTypeString(lu_type));
			setTextView(R.id.networkInfoLURejectCause, Integer.toString(rej_cause));

			setTextView(R.id.networkInfoCurrentImsi, IMSI);

			// Disable invalid elements

			// paging
			setVisibility2(paging_mi != 0, R.id.txtPaging, R.id.networkInfoCurrentPaging);

			// location update
			setVisibility(t_locupd == 1,    				R.id.networkInfoLupd);
			setVisibility2(t_locupd == 1,    				R.id.txtLUType, R.id.networkInfoLUType);
			setVisibility2(t_locupd == 1,    				R.id.txtLUResult, R.id.networkInfoLUResult);
			setVisibility2(t_locupd == 1 && lu_reject == 1, R.id.txtLURejectCause, R.id.networkInfoLURejectCause);
			setVisibility2(t_locupd == 1 && lu_acc == 1,    R.id.txtPreviousMCC, R.id.networkInfoPreviousMCC);
			setVisibility2(t_locupd == 1 && lu_acc == 1,    R.id.txtPreviousMNC, R.id.networkInfoPreviousMNC);
			setVisibility2(t_locupd == 1 && lu_acc == 1,    R.id.txtPreviousLAC, R.id.networkInfoPreviousLAC);

			// MSISDN
			setVisibility2(t_call == 1 || t_sms == 1,    	R.id.txtMSISDN, R.id.networkInfoCurrentMSISDN);

			// MCC, MNC, LAC or CID may not be present
			setVisibility2(mcc > 0,    						R.id.txtMCC, R.id.networkInfoCurrentMCC);
			setVisibility2(mnc > 0,    						R.id.txtMNC, R.id.networkInfoCurrentMNC);
			setVisibility2(lac > 0,    						R.id.txtLAC, R.id.networkInfoCurrentLAC);
			setVisibility2(cid > 0,    						R.id.txtCID, R.id.networkInfoCurrentCID);
			setVisibility2(rnc > 0,    						R.id.txtRNC, R.id.networkInfoCurrentRNC);
			setVisibility2(arfcn > 0,  						R.id.txtARFCN, R.id.networkInfoCurrentARFCN);

			// Duration may be 0
			setVisibility2(duration > 0,    				R.id.txtDuration, R.id.networkInfoCurrentDuration);
		}
	}

}
