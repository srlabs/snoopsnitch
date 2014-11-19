package de.srlabs.msd.qdmon;

import java.util.Vector;

import android.content.Context;

import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;

public class AnalysisEventData implements AnalysisEventDataInterface{
	private Context context;

	public AnalysisEventData(Context context) {
		this.context = context;
	}

	@Override
	public SMS getSMS(long id) {
		// TODO: Read from database
		return null;
	}

	@Override
	public Vector<SMS> getSMS(long startTime, long endTime) {
		// TODO: Read from database
		return null;
	}

	@Override
	public ImsiCatcher getImsiCatcher(long id) {
		// TODO: Read from database
		return null;
	}

	@Override
	public Vector<ImsiCatcher> getImsiCatchers(long startTime, long endTime) {
		// TODO: Read from database
		return null;
	}
}
