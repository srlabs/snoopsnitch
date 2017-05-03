package de.srlabs.snoopsnitch.qdmon;

import java.util.Vector;

import de.srlabs.snoopsnitch.analysis.Event;
import de.srlabs.snoopsnitch.analysis.ImsiCatcher;
import de.srlabs.snoopsnitch.analysis.RAT;
import de.srlabs.snoopsnitch.analysis.Risk;

public interface AnalysisEventDataInterface {
	public Event getEvent(long id);
	public Vector<Event> getEvent(long startTime, long endTime);
	public ImsiCatcher getImsiCatcher(long id);
	public Vector<ImsiCatcher> getImsiCatchers(long startTime,	long endTime);
	public Risk getScores();
	public RAT getCurrentRAT();
}
