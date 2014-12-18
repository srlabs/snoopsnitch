package de.srlabs.msd.qdmon;

interface IMsdServiceCallback {
	void stateChanged(String reason);
	void internalError();
}
