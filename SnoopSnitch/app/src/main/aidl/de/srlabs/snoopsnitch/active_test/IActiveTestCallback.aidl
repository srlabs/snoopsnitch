package de.srlabs.snoopsnitch.active_test;

interface IActiveTestCallback {
	void testResultsChanged(in Bundle b);
	void testStateChanged();
	void deviceIncompatibleDetected();
}