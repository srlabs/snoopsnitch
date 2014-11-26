package de.srlabs.msd.analysis;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Risk3G {

	private boolean valid = false;
	private String month = null;
	private double inter = 0.0;
	private double imper = 0.0;

	public Risk3G(SQLiteDatabase db, int currentMCC, int currentMNC) {

		Cursor c = db.query
				("risk_3g",
				 new String[] {"max(month)", "intercept", "impersonation" },
				 "mcc = ? AND mnc = ?",
				 new String[] {Integer.toString(currentMCC), Integer.toString(currentMNC)},
				 null, null, null);

		if (c.moveToFirst()){
			this.valid = true;
			this.month = c.getString(0);
			this.inter = c.getInt(1);
			this.imper = c.getInt(2);
		} else
		{
			this.valid = false;
		}
	}

	public String getMonth() {
		return month;
	}

	public double getInter() {
		return inter;
	}

	public double getImper() {
		return imper;
	}

	public boolean isValid() {
		return valid;
	}

	public boolean equals(Risk3G other) {
		return
			valid == other.valid &&
			month == other.month &&
			inter == other.inter &&
			imper == other.imper;
	}
}
