// ITestExecutorServiceInterface.aidl
package de.srlabs.patchalyzer;
import de.srlabs.patchalyzer.ITestExecutorCallbacks;
import de.srlabs.patchalyzer.ITestExecutorDashboardCallbacks;

// Declare any non-default types here with import statements

interface ITestExecutorServiceInterface {
    void startMakingDeviceInfo();
    boolean isDeviceInfoFinished();
    String getDeviceInfoJson();
    void startBasicTests();
    String evaluateVulnerabilitiesTests();
    void clearCache();
    void updateCallback(ITestExecutorCallbacks callback);
    void updateDashboardCallback(ITestExecutorDashboardCallbacks callback);
    void startWork(boolean updateTests, boolean generateDeviceInfo, boolean evaluateTests, boolean uploadTestResults, boolean uploadDeviceInfo);
    boolean isAnalysisRunning();
    void requestCancelAnalysis();
}
