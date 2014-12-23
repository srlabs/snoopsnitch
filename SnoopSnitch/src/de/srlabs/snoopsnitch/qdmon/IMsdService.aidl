package de.srlabs.snoopsnitch.qdmon;

import de.srlabs.snoopsnitch.qdmon.IMsdServiceCallback;

interface IMsdService {
	// Recording
	boolean isRecording();
	void registerCallback(IMsdServiceCallback callback);
	boolean startRecording();
	boolean stopRecording();
	// Needed for DummyMsdService
	long getServiceStartTime();
	// Reporting log messages from UI or ActiveTestService to the central LOG written by MsdService
	void writeLog(String logData);
	// Extra file recording for active test:
	long getExtraRecordingId();
	boolean startExtraRecording(String filename);
	boolean endExtraRecording(boolean markForUpload);
	void startActiveTest();
	void stopActiveTest();
	// Uploading
	void triggerUploading();
	// Retopens the debug log and marks it for uploading
	long reopenAndUploadDebugLog();
	// Returns the network generation detected by the parser (2,3,4) or 0 if it is unknown.
	int getParserNetworkGeneration();
}
