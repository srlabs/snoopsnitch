package de.srlabs.msd.analysis;

public interface AnalysisCallback {
	public void smsDetected(SMS sms);
	public void imsiCatcherDetected(SMS sms);
}
