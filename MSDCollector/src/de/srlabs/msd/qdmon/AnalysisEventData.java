package de.srlabs.msd.qdmon;

import java.io.IOException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.analysis.SMS.Type;
import de.srlabs.msd.util.MsdDatabaseManager;
import de.srlabs.msd.util.Utils;

public class AnalysisEventData implements AnalysisEventDataInterface{
	private SQLiteDatabase db;

	public AnalysisEventData(Context context) {

		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		this.db = MsdDatabaseManager.getInstance().openDatabase();

		// TODO: Factor out GSMmap data handling into own class.
		String text = null;
		try {
			// text = readFromExternal("data.js");
			text = Utils.readFromAssets(context, "data.js");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (!gsmmapDataPresent()) {
			try {
				parseGSMmapData(text);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean gsmmapDataPresent(){
		boolean result;
		Cursor c = db.query("gsmmap_operators", new String[] {"id"}, null, null, null, null, null);
		result = c.moveToFirst();
		c.close();
		return result;
	}

	private void parseGSMmapData(String text) throws JSONException {

		JSONObject gsmmapData;
		long operator_id = 0;
		gsmmapData = new JSONObject(text.split("^\\s*var\\s*data\\s*=\\s*", 2)[1]);

		// Empty all GSMmap databases
		db.delete("gsmmap_operators", null, null);
		db.delete("gsmmap_codes", null, null);
		db.delete("gsmmap_inter", null, null);
		db.delete("gsmmap_imper", null, null);
		db.delete("gsmmap_track", null, null);
		db.delete("gsmmap_inter3G", null, null);
		db.delete("gsmmap_imper3G", null, null);

		// Iterate overall countries
		JSONArray countries = gsmmapData.getJSONArray("countries");
		for (int c = 0, cs = countries.length(); c < cs; c++) {

			JSONObject country = countries.getJSONObject(c);

			// Iterate over all operators
			JSONArray operators = country.getJSONArray("operators");
			for (int o = 0, os = operators.length(); o < os; o++) {

				JSONObject operator = operators.getJSONObject(o);

				String operator_name = operator.get("name").toString();
				String operator_color = operator.has("color") ? operator.get("color").toString() : null;

				// Store operator in database
				ContentValues opval = new ContentValues();
				opval.put("id", operator_id);
				opval.put("name", operator_name);
				opval.put("color", operator_color);
				db.insert("gsmmap_operators", null, opval);
				opval.clear();

				// Store all values
				JSONObject values = operator.getJSONObject("values");
				updateValue(operator_id, values, "inter");
				updateValue(operator_id, values, "imper");
				updateValue(operator_id, values, "track");
				updateValue(operator_id, values, "inter3G");
				updateValue(operator_id, values, "imper3G");

				// Store all MCC/MNC combinations for an operator
				JSONArray mcc_mncs = operator.getJSONArray("mcc_mnc");
				for (int m = 0, ms = mcc_mncs.length(); m < ms; m++) {

					JSONObject mcc_mnc = mcc_mncs.getJSONObject(m);
					int mcc = mcc_mnc.getInt("mcc");
					int mnc = mcc_mnc.getInt("mnc");

					ContentValues codes_val = new ContentValues();
					codes_val.put("id", operator_id);
					codes_val.put("mcc", mcc);
					codes_val.put("mnc", mnc);
					db.insert("gsmmap_codes", null, codes_val);
					codes_val.clear();
				}
				operator_id++;
			}
		}
	}

	private void updateValue(long operator_id, JSONObject values, String kind)
			throws JSONException {
		if (values.has(kind)) {
			JSONArray value = values.getJSONArray(kind);
			for (int v = 0, vs = value.length(); v < vs; v++) {

				JSONObject val = value.getJSONObject(v);
				int monthIndex = val.getInt("monthIndex");
				int year  = 2011 + monthIndex / 12;
				int month = monthIndex % 12 + 1;

				ContentValues cont = new ContentValues();
				cont.put("id", operator_id);
				cont.put("year", Integer.toString(year));
				cont.put("month", Integer.toString(month));
				cont.put("value", val.getDouble("value"));
				db.insert("gsmmap_" + kind, null, cont);
				cont.clear();
			}
		}
	}

	private static String[] sms_cols =
			new String[] {"strftime('%s',timestamp)", "id", "mcc", "mnc", "lac", "cid", "latitude", "longitude", "smsc", "msisdn", "sms_type"};

	static private void logCatcher(ImsiCatcher c) {

			Log.i("CATCHER","Catcher: " + c.getStartTime() +
					", ID="  + c.getId() +
					", MCC=" + c.getMcc() +
					", MNC=" + c.getMnc() +
					", LAC=" + c.getLac() +
					", CID=" + c.getCid() +
					", Score=" + c.getScore()
					);
	}

	static private SMS smsFromCursor(Cursor c) {
		Type sms_type;

		switch (c.getInt(10))
		{
			// Binary SMS
			case 0:	sms_type = Type.BINARY_SMS;
					break;

			// Silent SMS
			case 1:	sms_type = Type.SILENT_SMS;
					break;

			// Invalid type from database
			default: sms_type = Type.INVALID_SMS;
		}

		return new SMS
				(c.getLong(0),		// timestamp
				 c.getLong(1),		// id
				 c.getInt(2),		// mcc
				 c.getInt(3),		// mnc
				 c.getInt(4),		// lac
				 c.getInt(5),		// cid
				 c.getDouble(6),	// latitude
				 c.getDouble(7),	// longitude
				 c.getString(8),	// smsc
				 c.getString(9),	// msisdn
				 sms_type			// SMS type
				);
	}

	@Override
	public SMS getSMS(long id) {
		Cursor c = db.query("sms", sms_cols, "id = ?", new String[] {Long.toString(id)}, null, null, null);
		if(!c.moveToFirst()) {
			throw new IllegalStateException("Requesting non-existing SMS " + Long.toString(id));
		}
		return smsFromCursor (c);
	}

	@Override
	public Vector<SMS> getSMS(long startTime, long endTime) {

		Vector<SMS> result = new Vector<SMS>();

		Cursor c = db.query("sms", sms_cols, "strftime('%s',timestamp) >= ? AND strftime('%s',timestamp) <= ?",
				new String[] {Long.toString(startTime), Long.toString(endTime)}, null, null, null);

		if(c.moveToFirst()) {
			do {
				result.add(smsFromCursor(c));
			} while (c.moveToNext());
		}
		return result;
	}

	private static String[] catcher_cols =
			new String[] {"strftime('%s',timestamp)", "strftime('%s',timestamp) + duration/1000", "id", "mcc", "mnc", "lac", "cid", "latitude", "longitude", "score"};

	static private ImsiCatcher catcherFromCursor(Cursor c) {

		return new ImsiCatcher
				(c.getLong(0),		// startTime
				 c.getLong(1),		// endTime
				 c.getInt(2),		// id
				 c.getInt(3),		// mcc
				 c.getInt(4),		// mnc
				 c.getInt(5),		// lac
				 c.getInt(6),		// cid
				 c.getDouble(7),	// latitude
				 c.getDouble(8),	// longitude
				 c.getDouble(9) 	// score
				);
	}

	@Override
	public ImsiCatcher getImsiCatcher(long id) {
		Cursor c = db.query("catcher", catcher_cols, "id = ?", new String[] {Long.toString(id)}, null, null, null);
		if(!c.moveToFirst()) {
			throw new IllegalStateException("Requesting non-existing IMSI catcher");
		}
		return catcherFromCursor (c);
	}

	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime) {

		ImsiCatcher catcher;
		Vector<ImsiCatcher> result = new Vector<ImsiCatcher>();

		Cursor c = db.query("catcher", catcher_cols, "strftime('%s',timestamp) >= ? AND strftime('%s',timestamp) <= ?",
				new String[] {Long.toString(startTime), Long.toString(endTime)}, null, null, null);

		if(c.moveToFirst()) {
			do {
				catcher = catcherFromCursor(c);
				logCatcher(catcher);
				result.add(catcher);
			} while (c.moveToNext());
		}
		return result;
	}

	@Override
	public Risk getScores(Operator operator) {
		return new Risk(db, operator.getMcc(), operator.getMnc());
	}
}
