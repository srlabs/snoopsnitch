package de.srlabs.snoopsnitch.qdmon;

interface IMsdServiceCallback {
	void stateChanged(String reason);
	void internalError();
}
