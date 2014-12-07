package de.srlabs.msd.active_test;

public interface ActiveTestCallback {
	public void handleTestResults(ActiveTestResults results);
	public void testStateChanged();
	public void internalError(String msg);
}
