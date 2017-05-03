package de.srlabs.snoopsnitch.analysis;

import java.util.Calendar;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.srlabs.snoopsnitch.qdmon.MsdSQLiteOpenHelper;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class GSMmap {

	private static SQLiteDatabase db;

	public GSMmap(Context context) {
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
		db = MsdDatabaseManager.getInstance().openDatabase();
	}

	public boolean dataPresent(){
		boolean result;
		Cursor c = db.query("gsmmap_operators", new String[] {"id"}, null, null, null, null, null);
		result = c.getCount() > 0;
		c.close();
		return result;
	}

	/**
	 * Checks whether enough data is available on gsmmap.org to skip an active test
	 * @param mcc - current MCC
	 * @param mnc - current MNC
	 * @param networkType
	 * 2: GSM
	 * 3: 3G
	 * 4: LTE
	 */
	public static boolean dataSufficient(int mcc, int mnc, int networkType) {
		Cursor c;
		String interTable, imperTable;
		int interYear, interMonth, imperYear, imperMonth;
		double interSize, imperSize;

		switch (networkType) {
			case 2:
				interTable = "gsmmap_inter";
				imperTable = "gsmmap_imper";
				break;
			case 3:
				interTable = "gsmmap_inter3G";
				imperTable = "gsmmap_imper3G";
				break;
			case 4:
				//  Keep collecting data for LTE
				return false;
			default:
				return true;
		}

		c = db.rawQuery ("SELECT year, month, size FROM gsmmap_codes as gc, " + interTable +  " as i ON gc.id = i.id WHERE mcc=? and mnc=? ORDER BY year DESC, month DESC LIMIT 1",
				new String[] {Integer.toString(mcc), Integer.toString(mnc)});
		if (!c.moveToFirst()) {
			//  Not found - keep collecting
			c.close();
			return false;
		}

		interYear  = c.getInt(0);
		interMonth = c.getInt(1);
		interSize  = c.getDouble(2);

		if (interSize < 1.0) {
			c.close();
			return false;
		}

		c = db.rawQuery ("SELECT year, month, size FROM gsmmap_codes as gc, " + imperTable +  " as i ON gc.id = i.id WHERE mcc=? and mnc=? ORDER BY year DESC, month DESC LIMIT 1",
				new String[] {Integer.toString(mcc), Integer.toString(mnc)});
		if (!c.moveToFirst()) {
			//  Not found - keep collecting
			c.close();
			return false;
		}

		imperYear  = c.getInt(0);
		imperMonth = c.getInt(1);
		imperSize  = c.getDouble(2);

		if (imperSize < 1.0) {
			c.close();
			return false;
		}

		c.close();
		return isCurrent(imperYear, imperMonth) && isCurrent(interYear, interMonth);
	}

	private static boolean isCurrent(int year, int month) {
		Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar dataTime = Calendar.getInstance();
		dataTime.clear();
		dataTime.set(Calendar.YEAR, year);
		dataTime.set(Calendar.MONTH, month);
		dataTime.set(Calendar.DAY_OF_MONTH, 0);
		dataTime.set(Calendar.HOUR_OF_DAY, 0);
		dataTime.set(Calendar.MINUTE, 0);
		dataTime.set(Calendar.SECOND, 0);

		//  Hard code to approximately 2 months
		return (currentTime.getTimeInMillis() - dataTime.getTimeInMillis()) < 60 * 24 * 60 * 60 * 1000L;
	}

	public void parse(String text) throws JSONException {

		JSONObject gsmmapData;
		long operator_id = 0;
		gsmmapData = new JSONObject(text);

		db.beginTransaction();

		try {

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
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private void updateValue(long operator_id, JSONObject values, String kind)
			throws JSONException {
		if (values.has(kind)) {
			JSONArray value = values.getJSONArray(kind);
			for (int v = 0, vs = value.length(); v < vs; v++) {

				JSONObject val = value.getJSONObject(v);
				double size = 0.0;

				//  Should not happen, except when upstream data is broken
				if (val.isNull("monthIndex") || val.isNull("value")) {
					continue;
				}

				if (!val.isNull("size")) {
					size = val.getDouble("size");
				}

				int monthIndex = val.getInt("monthIndex");
				int year  = 2011 + monthIndex / 12;
				int month = monthIndex % 12 + 1;

				ContentValues cont = new ContentValues();
				cont.put("id", operator_id);
				cont.put("year", Integer.toString(year));
				cont.put("month", Integer.toString(month));
				cont.put("value", val.getDouble("value"));
				cont.put("size", size);
				db.insert("gsmmap_" + kind, null, cont);
				cont.clear();
			}
		}
	}

}
