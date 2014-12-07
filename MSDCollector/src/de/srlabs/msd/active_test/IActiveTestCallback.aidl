package de.srlabs.msd.active_test;

interface IActiveTestCallback {
	void testResultsChanged(in Bundle b);
	void testStateChanged();
}