package de.srlabs.msd.analysis;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Risk {

	private boolean valid = false;
	private String month = null;
	private double inter = 0.0;
	private double imper = 0.0;
	private double track = 0.0;

	public Risk(SQLiteDatabase db, int currentMCC, int currentMNC) {

		Cursor c = db.query
				("risk_category",
				 new String[] {"max(month)", "avg(intercept)", "avg(impersonation)", "avg(tracking)" },
				 "mcc = ? AND mnc = ?",
				 new String[] {Integer.toString(currentMCC), Integer.toString(currentMNC)},
				 "mcc, mnc, lac", null, null);

		if (c.moveToFirst()){
			this.valid = true;
			this.month = c.getString(0);
			this.inter = c.getInt(1);
			this.imper = c.getInt(2);
			this.track = c.getInt(3);
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

	public double getTrack() {
		return track;
	}
	
	public boolean isValid() {
		return valid;
	}

	public boolean equals(Risk other) {
		return
			valid == other.valid &&
			month == other.month &&
			inter == other.inter &&
			imper == other.imper &&
			track == other.track;
	}
}
