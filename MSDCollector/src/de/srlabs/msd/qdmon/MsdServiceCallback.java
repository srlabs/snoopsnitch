package de.srlabs.msd.qdmon;

public interface MsdServiceCallback {
	public static final int ERROR_STORAGE_FULL = 1;
	public static final int ERROR_RECORDING_STOPPED_BATTERY_LEVEL = 2;
	public void recordingStarted();
	public void recordingStopped();
	public void internalError(String errorMsg);
	public void error(int id);
}
