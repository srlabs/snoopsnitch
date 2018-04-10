// ITestExecutorCallbacks.aidl
package de.srlabs.patchalyzer;

// Declare any non-default types here with import statements

interface ITestExecutorCallbacks {
    void updateProgress(double progressPercent);
    void showErrorMessage(String text);
    void showNoCVETestsForApiLevel(String message);
    void finished(String analysisResultString, boolean isBuildCertified);
    void reloadViewState();
    void handleFatalError(String stickyErrorMessage);
}
