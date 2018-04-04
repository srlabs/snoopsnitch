// ITestExecutorCallbacks.aidl
package de.srlabs.patchalyzer;

// Declare any non-default types here with import statements

interface ITestExecutorCallbacks {
    void updateProgress(double progressPercent);
    void showErrorMessage(String text);
    void showStatusMessage(String text);
    void showOutdatedError(String updateUrl);
    void showNoCVETestsForApiLevel(String message);
    void finished(String analysisResultString);
    void cancelled();
}
