package de.srlabs.msd.qdmon;

import java.util.Vector;

import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.analysis.SMS;

public interface AnalysisEventDataInterface {
	public SMS getSMS(long id);
	public Vector<SMS> getSMS(long startTime, long endTime);
	public ImsiCatcher getImsiCatcher(long id);
	public Vector<ImsiCatcher> getImsiCatchers(long startTime,	long endTime);
	public Risk getScores();
}
