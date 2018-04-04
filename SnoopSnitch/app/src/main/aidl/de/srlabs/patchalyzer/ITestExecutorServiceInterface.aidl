// ITestExecutorServiceInterface.aidl
package de.srlabs.patchalyzer;
import de.srlabs.patchalyzer.ITestExecutorCallbacks;

// Declare any non-default types here with import statements

interface ITestExecutorServiceInterface {
    void startMakingDeviceInfo();
    boolean isDeviceInfoFinished();
    String getDeviceInfoJson();
    void startBasicTests();
    int getBasicTestsQueueSize();
    String evaluateVulnerabilitiesTests();
    void clearCache();
    boolean updateTestsNeeded();
    void updateCallback(ITestExecutorCallbacks callback);
    void startWork(boolean updateTests, boolean generateDeviceInfo, boolean evaluateTests, boolean uploadTestResults, boolean uploadDeviceInfo);
    boolean isAnalysisRunning();
}
