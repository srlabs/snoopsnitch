package de.srlabs.msd.qdmon;

import java.util.Vector;

import de.srlabs.msd.analysis.AnalysisCallback;
import de.srlabs.msd.analysis.ImsiCatcher;
import de.srlabs.msd.analysis.SMS;

public interface MsdServiceHelperInterface {
	public boolean startRecording();
	public boolean restartRecording();
	public boolean stopRecording();
	public boolean isRecording();
	public void registerAnalysisCallback(AnalysisCallback aCallback);
	public void unregisterAnalysisCallback(AnalysisCallback aCallback);
	public SMS getSMS(long id);
	public Vector<SMS> getSMS(long startTime, long endTime);
	public ImsiCatcher getImsiCatcher(long id);
	public Vector<ImsiCatcher> getImsiCatchers(long startTime,	long endTime);
}
