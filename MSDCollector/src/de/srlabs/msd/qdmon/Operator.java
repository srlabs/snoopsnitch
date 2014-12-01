package de.srlabs.msd.qdmon;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

	public Operator(SQLiteDatabase db) {
		Cursor c = db.query("serving_cell_info", new String[] {"max(_id)", "mcc", "mnc"}, null, null, null, null, null);
		if (c.moveToFirst()){
			mcc = c.getInt(1);
			mnc = c.getInt(2);
			valid = true;
		};
	}
}