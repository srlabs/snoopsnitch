package de.srlabs.msd.active_test;

import java.util.Vector;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import de.srlabs.msd.active_test.ActiveTestResults.SingleTestState;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.MsdLog;

public class DummyActiveTestService extends Service{
	private static final String TAG = "dummy-msd-active-test-service";
	private final MyActiveTestServiceStub mBinder = new MyActiveTestServiceStub();
	private ActiveTestResults results = new ActiveTestResults();
	private ProgressTickRunnable progressTickRunnable;
	private Handler handler = new Handler();
	private Vector<IActiveTestCallback> callbacks = new Vector<IActiveTestCallback>();
	private String ownNumber;
	private StateMachine stateMachine;
	private boolean testRunning;
	class MyActiveTestServiceStub extends IActiveTestService.Stub {
		public void registerCallback(IActiveTestCallback callback) throws RemoteException {
			if(!callbacks.contains(callback))
				callbacks.add(callback);
			broadcastTestStateChanged();
			broadcastTestResults();
		}

		@Override
		public boolean startTest(String ownNumber) throws RemoteException {
			return DummyActiveTestService.this.startTest(ownNumber);
		}

		@Override
		public void stopTest() throws RemoteException {
			DummyActiveTestService.this.stopTest();
		}

		@Override
		public void clearResults() throws RemoteException {
			results = new ActiveTestResults();
			broadcastTestResults();
		}
		@Override
		public void clearCurrentFails() throws RemoteException {
			results.getCurrentNetworkOperatorRatTestResults().clearFails();
			broadcastTestResults();
		}
		@Override
		public void clearCurrentResults() throws RemoteException {
			results.getCurrentNetworkOperatorRatTestResults().clearFails();
			broadcastTestResults();
		}
		@Override
		public boolean isTestRunning() throws RemoteException {
			return testRunning;
		}

		@Override
		public void setForegroundActivityClass(String className)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}
	}
	class ProgressTickRunnable implements Runnable{
		boolean stopped = false;
		@Override
		public void run() {
			if(stopped)
				return;
			stateMachine.progressTick();
			handler.postDelayed(this, 1000);
		}
	}
	enum State {
		ROUND_START, CALL_MO, CALL_MO_ACTIVE, SMS_MO, CALL_MT_API, CALL_MT_WAITING, CALL_MT_ACTIVE, SMS_MT_API, SMS_MT_WAITING, PAUSE, END
	}
	class StateMachine{
		State state = State.ROUND_START;
		long nextTimeoutMillis = 0;
		/**
		 * Make sure that this test actually stops after stopTest(), even if there is a Runnable
		 */
		private boolean testStopped = false;
		Vector<PendingEvent> pendingEvents = new Vector<DummyActiveTestService.StateMachine.PendingEvent>();
		private Runnable iterateRunnable = new Runnable() {
			@Override
			public void run() {
				iterate();
			}
		};
		abstract class PendingEvent{
			long time;
			public PendingEvent(long delayMs) {
				this.time = System.currentTimeMillis() + delayMs;
			}
			abstract void run();
		}
		void currentTestSuccess(){
			results.getCurrentTest().success();
		}
		public void postIterateRunnable(int delayMillis) {
			handler.postDelayed(this.iterateRunnable , delayMillis);
		}
		void setState(State newState, String msg, long timeout){
			if(timeout > 0)
				nextTimeoutMillis = System.currentTimeMillis() + timeout;
			else
				nextTimeoutMillis = 0; // Disable previous timeout
			String logMsg = "setState: " + state.name() + " => " + newState.name();
			if(msg != null)
				logMsg += " : " + msg;
			debugInfo(logMsg);
			state = newState;
		}
		void iterate(){
			if(testStopped)
				return;
			results.setDummyNetworkOperatorAndRat();
			debugInfo("iterate() called");
			// Find the action with the lowest run count and then trigger this action
			int numSmsMo = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.SMS_MO);
			int numCallMo = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.CALL_MO);
			int numSmsMt = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.SMS_MT);
			int numCallMt = results.getCurrentNetworkOperatorRatTestResults().getNumRuns(TestType.CALL_MT);
			State nextState = State.SMS_MO;
			int minRunCount = numSmsMo;
			if(numCallMo < minRunCount){
				minRunCount = numCallMo;
				nextState = State.CALL_MO;
			}
			if(numSmsMt < minRunCount){
				minRunCount = numSmsMt;
				nextState = State.SMS_MT_API;
			}
			if(numCallMt < minRunCount){
				minRunCount = numCallMt;
				nextState = State.CALL_MT_API;
			}
			if(minRunCount >= results.getNumIterations()){
				nextState = State.END;
			}
			debugInfo("iterate(): numSmsMo=" + numSmsMo + "  numCallMo=" + numCallMo + "  numSmsMt=" + numSmsMt + "  numCallMt=" + numCallMt + "  nextState=" + nextState.name());
			if(nextState == State.SMS_MO){
				setState(State.SMS_MO, "iterate()",Constants.SMS_MO_TIMEOUT);
				results.startTest(TestType.SMS_MO,Constants.SMS_MO_TIMEOUT);
				results.getCurrentTest().stateTestRunning();
				if(results.getCurrentTest().getNum() == 1){
					pendingEvents.add(new PendingEvent(Constants.SMS_MO_TIMEOUT){
						@Override
						void run() {
							results.getCurrentTest().failTimeout();
							iterate();
						}
					});
				} else{
					pendingEvents.add(new PendingEvent(2000){
						@Override
						void run() {
							currentTestSuccess();
							iterate();
						}
					});
				}
			} else if(nextState == State.CALL_MO){
				setState(State.CALL_MO, "iterate()",Constants.CALL_MO_TIMEOUT);
				results.startTest(TestType.CALL_MO,Constants.CALL_MO_TIMEOUT + Constants.CALL_MO_ACTIVE_TIMEOUT);
				results.getCurrentTest().stateWaiting();
				if(results.getCurrentTest().getNum() == 2){
					pendingEvents.add(new PendingEvent(Constants.CALL_MO_TIMEOUT){
						@Override
						void run() {
							results.getCurrentTest().failTimeout();
							iterate();
						}
					});
				} else{
					// Sequence: Dialing => Outgoing call ringing => SUCCESS
					pendingEvents.add(new PendingEvent(2000){
						@Override
						void run() {
							results.getCurrentTest().stateTestRunning();
							pendingEvents.add(new PendingEvent(2000){
								@Override
								void run() {
									currentTestSuccess();
									iterate();
								}
							});
						}
					});
				}
			} else if(nextState == State.SMS_MT_API){
				setState(State.SMS_MT_API, "iterate()",Constants.API_TIMEOUT);
				results.startTest(TestType.SMS_MT,Constants.API_TIMEOUT + Constants.SMS_MT_TIMEOUT);
				results.getCurrentTest().stateApiRunning();
				if(results.getCurrentTest().getNum() == 3){
					pendingEvents.add(new PendingEvent(Constants.API_TIMEOUT){
						@Override
						void run() {
							results.getCurrentTest().failApiTimeout();
							iterate();
						}
					});
				} else{
					// Sequence: API Running => Waiting for incoming SMS => Success
					pendingEvents.add(new PendingEvent(2000){
						@Override
						void run() {
							results.getCurrentTest().stateWaiting();
							pendingEvents.add(new PendingEvent(2000){
								@Override
								void run() {
									currentTestSuccess();
									iterate();
								}
							});
						}
					});
				}
			} else if(nextState == State.CALL_MT_API){
				setState(State.CALL_MT_API, "iterate()",Constants.API_TIMEOUT);
				results.startTest(TestType.CALL_MT,Constants.API_TIMEOUT + Constants.CALL_MT_TIMEOUT + Constants.CALL_MT_ACTIVE_TIMEOUT);
				results.getCurrentTest().stateApiRunning();
				if(results.getCurrentTest().getNum() == 4){
					pendingEvents.add(new PendingEvent(Constants.API_TIMEOUT){
						@Override
						void run() {
							results.getCurrentTest().failApiTimeout();
							iterate();
						}
					});
				} else{
					// Sequence: API Running => Waiting for incoming call => Incoming call ringing => Success
					pendingEvents.add(new PendingEvent(2000){
						@Override
						void run() {
							results.getCurrentTest().stateWaiting();
							pendingEvents.add(new PendingEvent(2000){
								@Override
								void run() {
									results.getCurrentTest().stateTestRunning();
									pendingEvents.add(new PendingEvent(2000){
										@Override
										void run() {
											currentTestSuccess();
											iterate();
										}
									});
								}
							});
						}
					});
				}
			} else if(nextState == State.END){
				setState(State.END,"iterate() reached state END",0);
				results.testRoundComplete();
				DummyActiveTestService.this.stopTest();
			} else{
				handleFatalError("Invalid nextState in StateMachine.iterate()");
			}
			broadcastTestResults();
		}
		/**
		 * Called once per second, sends state updates to UI and aborts tests after timeout
		 */
		void progressTick(){
			Vector<PendingEvent> eventsToDo = new Vector<DummyActiveTestService.StateMachine.PendingEvent>();
			debugInfo("stateMachine.progressTick called");
			for(PendingEvent event:pendingEvents){
				if(System.currentTimeMillis() > event.time){
					eventsToDo.add(event);
				}
			}
			for(PendingEvent event:eventsToDo){
				debugInfo("stateMachine.progressTick runnning event");
				event.run();
			}
			pendingEvents.removeAll(eventsToDo);
			broadcastTestResults();
		}
		public void stopTest(){
			handler.removeCallbacks(this.iterateRunnable);
			testStopped = true;
			SingleTestState currentTest = results.getCurrentTest();
			if(currentTest != null)
				currentTest.fail("Test aborted with stopTest()");
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		MsdLog.i(TAG,"ActiveTestService.onBind() called");
		Thread.setDefaultUncaughtExceptionHandler(
				new Thread.UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						handleFatalError("Uncaught Exception in ActiveTestService Thread " + t.getClass(), e);
					}
				});
		this.results.setDummyNetworkOperatorAndRat();
		return mBinder;
	}

	public void debugInfo(String msg) {
		MsdLog.i(TAG,msg);
	}
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		return START_NOT_STICKY;
	}

	private boolean startTest(String ownNumber){
		this.ownNumber = ownNumber;
		this.results.setDummyNetworkOperatorAndRat();
		this.results.clearErrorLog();
		this.testRunning = true;
		stateMachine = new StateMachine();
		stateMachine.postIterateRunnable(0);
		progressTickRunnable = new ProgressTickRunnable();
		handler.postDelayed(progressTickRunnable, 1000);
		broadcastTestStateChanged();
		broadcastTestResults();
		return true;
	}
	private void stopTest(){
		if(progressTickRunnable != null){
			progressTickRunnable.stopped = true;
			progressTickRunnable = null;
		}
		if(stateMachine != null){
			stateMachine.stopTest();
			stateMachine = null;
		}
		testRunning = false;
		broadcastTestStateChanged();
		broadcastTestResults();
	}
	private void broadcastTestResults() {
		debugInfo("broadcastTestResults() called");
		Bundle b = new Bundle();
		b.putSerializable("results", results);
		Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
		for(IActiveTestCallback callback:callbacks){
			try {
				callback.testResultsChanged(b);
			} catch (Exception e) {
				debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
				callbacksToRemove.add(callback);
			}
		}
		callbacks.removeAll(callbacksToRemove);
		if(callbacks.size() == 0){
			handleFatalError("Terminating active test since all callbacks have disappeared");
		}
	}
	private void broadcastTestStateChanged() {
		Vector<IActiveTestCallback> callbacksToRemove = new Vector<IActiveTestCallback>();
		for(IActiveTestCallback callback:callbacks){
			try {
				callback.testStateChanged();
			} catch (Exception e) {
				debugInfo("Removing callback due to " + e.getClass().getCanonicalName());
				callbacksToRemove.add(callback);
			}
		}
		callbacks.removeAll(callbacksToRemove);
		if(callbacks.size() == 0){
			handleFatalError("Terminating active test since all callbacks have disappeared");
		}
	}
	private void handleFatalError(String msg){
		handleFatalError(msg,null);
	}
	private void handleFatalError(String msg, final Throwable e){
		if(e != null)
			msg += ": " + e.getClass().getCanonicalName() + ": " + e.getMessage() + "  Stack: " + Log.getStackTraceString(e);
		MsdLog.e(TAG,"handleFatalError: " + msg);
		results.setFatalError(msg);
		broadcastTestResults();
		stopTest();
		stopSelf(); // Terminate this service after a fatal error
	}
}
