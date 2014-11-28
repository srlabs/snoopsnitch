package de.srlabs.msd.qdmon;

import de.srlabs.msd.qdmon.IMsdServiceCallback;

interface IMsdService {
	boolean isRecording();
	void registerCallback(IMsdServiceCallback callback);
	boolean startRecording();
	boolean stopRecording();
	long getServiceStartTime();
	void writeLog(String logData);
}
