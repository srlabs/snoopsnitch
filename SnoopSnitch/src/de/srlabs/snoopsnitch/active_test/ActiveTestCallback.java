package de.srlabs.snoopsnitch.active_test;

public interface ActiveTestCallback {
	public void handleTestResults(ActiveTestResults results);
	public void testStateChanged();
	public void internalError(String msg);
	public void deviceIncompatibleDetected();
}
