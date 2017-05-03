package de.srlabs.snoopsnitch.analysis;

import java.util.Vector;

import de.srlabs.snoopsnitch.qdmon.Operator;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Risk {

	private Vector<Score> inter = null;
	private Vector<Score> imper = null;
	private Vector<Score> track = null;
	private Vector<Score> inter3G = null;
	private Vector<Score> imper3G = null;
	private String operatorName = null;
	private String operatorColor = null;
	private Operator op = null;
	private int mcc = 0;
	private int mnc = 0;
	private boolean valid = false;

	private Vector<Risk> serverData = null;

	public Risk(SQLiteDatabase db, Operator currentOperator) {

		op = currentOperator;
		mcc = op.getMcc();
		mnc = op.getMnc();

		//  Set inter, imper and track values
		this.inter = query2GScores(db, mcc, mnc, "intercept");
		this.imper = query2GScores(db, mcc, mnc, "impersonation");
		this.track = query2GScores(db, mcc, mnc, "tracking");
		this.inter3G = query3GScores(db, mcc, mnc, "intercept3G");

		//  Impersonation makes no sense for 3G in its current form
		//  this.imper3G = query3GScores(db, mcc, mnc, "impersonation3G");
		this.imper3G = new Vector<Score>();

		queryOperatorData(db, mcc, mnc);
 
		// Set other operators
		serverData = new Vector<Risk>();
		Cursor c = db.rawQuery
			("select go.id, go.name, go.color from gsmmap_operators AS go, gsmmap_codes AS gc ON go.id = gc.id WHERE mcc=? GROUP BY gc.id",
			new String[] {Integer.toString(mcc)});

		if (c.moveToFirst()){
			do {
				serverData.add(new Risk(db, c.getLong(0), c.getString(1), c.getString(2), mcc));
			} while (c.moveToNext());
		}
		c.close();

	}

	public Risk(SQLiteDatabase db, long id, String operatorName, String operatorColor, int mcc) {

		this.mcc = mcc;
		this.operatorName = operatorName;
		this.operatorColor = operatorColor;

		this.inter = retrieveValues(db, id, "gsmmap_inter");
		this.imper = retrieveValues(db, id, "gsmmap_imper");
		this.track = retrieveValues(db, id, "gsmmap_track");
		this.inter3G = retrieveValues(db, id, "gsmmap_inter3G");
		//  this.imper3G = retrieveValues(db, id, "gsmmap_imper3G");
		this.imper3G = new Vector<Score>();
	}

	private Vector<Score> retrieveValues(SQLiteDatabase db, long id, String table) {

		Vector<Score> result = new Vector<Score>();
		Cursor c = db.rawQuery
			("select year, month, value from " + table + " where id=? order by year, month",
			new String[] {Long.toString(id)});

		if (c.moveToFirst()){
			do {
				int year = c.getInt(0);
				int month = c.getInt(1);
				double score = c.getDouble(2);
				result.add(new Score(year, month, score));
			} while (c.moveToNext());
		}
		c.close();
		return result;
	}

	private void queryOperatorData(SQLiteDatabase db, int currentMCC, int currentMNC) {

		Cursor c = db.rawQuery
			("SELECT go.name, go.color FROM gsmmap_operators AS go, gsmmap_codes AS gc ON go.id = gc.id WHERE mcc=? and mnc=? GROUP BY gc.id",
			new String[] {Integer.toString(currentMCC), Integer.toString(currentMNC)});

		if (!c.moveToFirst()){
			return;
		}

		this.valid = true;
		this.operatorName  = c.getString(0);
		this.operatorColor = c.getString(1);
		c.close();
	}

	private Vector<Score> query2GScores(SQLiteDatabase db, int currentMCC, int currentMNC, String scoreName) {
		return queryScores(db, currentMCC, currentMNC, "risk_category", scoreName);
	}

	private Vector<Score> query3GScores(SQLiteDatabase db, int currentMCC, int currentMNC, String scoreName) {
		return queryScores(db, currentMCC, currentMNC, "risk_3G", scoreName);
	}

	private Vector<Score> queryScores(SQLiteDatabase db, int currentMCC, int currentMNC, String tableName, String scoreName) {
		Vector<Score> result = new Vector<Score>();

		Cursor c = db.query
				(tableName,
				 new String[] {"month", "avg(" + scoreName + ") as score" },
				 "mcc = ? AND mnc = ?",
				 new String[] {Integer.toString(currentMCC), Integer.toString(currentMNC)},
				 "month", "score is not null", "month");

		while (c.moveToNext()) {
			String[] monthString = c.getString(0).split("-");
			int year  = Integer.parseInt(monthString[0]);
			int month = Integer.parseInt(monthString[1]);
			double value = c.getDouble(1);
			result.add(new Score(year, month, value));
		}

		c.close();
		return result;
	}

	public Vector<Score> getInter() {
		return inter;
	}

	public Vector<Score> getImper() {
		return imper;
	}

	public Vector<Score> getTrack() {
		return track;
	}

	public Vector<Score> getInter3G() {
		return inter3G;
	}

	public Vector<Score> getImper3G() {
		return imper3G;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public String getOperatorColor() {
		if (operatorColor == null) {
			return "#111111";
		}
		return operatorColor;
	}

	public Vector<Risk> getServerData() {
		return serverData;
	}

	public int getMcc() {
		return mcc;
	}

	public int getMnc() {
		return mnc;
	}

	public boolean operatorUnknown() {
		return op.isValid() && !valid;
	}

	public boolean changed(Risk previous) {

		// result list changed
		if (this.imper.size() != previous.imper.size() ||
			this.inter.size() != previous.inter.size() ||
			this.track.size() != previous.track.size() ||
			this.inter3G.size() != previous.inter3G.size() ||
			this.imper3G.size() != previous.imper3G.size()) return true;

		return
			lastScoreChanged(this.imper, previous.imper) ||
			lastScoreChanged(this.inter, previous.inter) ||
			lastScoreChanged(this.track, previous.track) ||
			lastScoreChanged(this.imper3G, previous.imper3G) ||
			lastScoreChanged(this.inter3G, previous.inter3G);
	}

	private boolean lastScoreChanged(Vector<Score> current, Vector<Score> previous) {
		// last result changed
		if (!current.isEmpty() && !previous.isEmpty()) {
			return current.lastElement() != previous.lastElement();
		}
		return false;
	}
}
