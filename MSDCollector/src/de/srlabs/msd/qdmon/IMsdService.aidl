package de.srlabs.msd.qdmon;

import de.srlabs.msd.qdmon.IMsdServiceCallback;

interface IMsdService {
	boolean isRecording();
	void registerCallback(IMsdServiceCallback callback);
	boolean startRecording();
	boolean stopRecording();
	long getServiceStartTime();
	void writeLog(String logData);
	// Extra file recording for active test:
	long getExtraRecordingId();
	boolean startExtraRecording(String filename);
	boolean endExtraRecording(boolean markForUpload);
}
