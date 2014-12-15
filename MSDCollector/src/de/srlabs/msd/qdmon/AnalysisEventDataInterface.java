package de.srlabs.msd.qdmon;

import java.util.Vector;

import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.RAT;
import de.srlabs.msd.analysis.Risk;
import de.srlabs.msd.analysis.Event;

public interface AnalysisEventDataInterface {
	public Event getEvent(long id);
	public Vector<Event> getEvent(long startTime, long endTime);
	public ImsiCatcher getImsiCatcher(long id);
	public Vector<ImsiCatcher> getImsiCatchers(long startTime,	long endTime);
	public Risk getScores();
	public RAT getCurrentRAT();
}
