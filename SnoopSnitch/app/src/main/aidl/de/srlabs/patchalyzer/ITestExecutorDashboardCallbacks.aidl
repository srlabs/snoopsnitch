// ITestExecutorDashboardCallbacks.aidl
package de.srlabs.patchalyzer;

// Declare any non-default types here with import statements

interface ITestExecutorDashboardCallbacks {
    void finished(String analysisResultString, boolean isBuildCertified, long currentAnalysisTimestamp);
    void handleFatalError(String stickyErrorMessage, long currentAnalysisTimestamp);
}
