package de.srlabs.msd.qdmon;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import de.srlabs.msd.analysis.GSMmap;
import de.srlabs.msd.upload.DumpFile;
import de.srlabs.msd.upload.MsdServiceUploadThread;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msd.util.MsdConfig;
import de.srlabs.msd.util.MsdDatabaseManager;
import de.srlabs.msd.util.MsdLog;

public class MsdService extends Service{
	public static final String    TAG                   = "msd-service";

	// TODO: Watch storage utilisation and stop recording if limits are exceeded
	// TODO: Watch battery level and stop recording if battery level goes below configured limit

	private final MyMsdServiceStub mBinder = new MyMsdServiceStub();

	private  long activeTestTimestamp = 0;
	private boolean recordingStartedForActiveTest = false;
	class MyMsdServiceStub extends IMsdService.Stub {

		private Vector<IMsdServiceCallback> callbacks = new Vector<IMsdServiceCallback>();

		@Override
		public synchronized boolean isRecording() throws RemoteException {
			return recording;
		}

		@Override
		public synchronized boolean startRecording() throws RemoteException {
			return MsdService.this.startRecording();
		}

		@Override
		public synchronized boolean stopRecording() throws RemoteException {
			if(!isRecording()){
				sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
				return true;
			}
			if(System.currentTimeMillis() < activeTestTimestamp + 120 * 1000){
				// ignore stopRecording if active test is running
				sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
				return false;
			}
			return MsdService.this.shutdown(false);
		}

		@Override
		public void registerCallback(IMsdServiceCallback callback) throws RemoteException {
			info("registerCallback() called");
			if(!callbacks.contains(callback))
				callbacks.add(callback);
			info("registerCallback() returns");
		}

		@Override
		public long getServiceStartTime() throws RemoteException {
			// Do nothing here, the real Service doesn't deliver dummy events
			return 0;
		}

		@Override
		public void writeLog(String logData) throws RemoteException {
			MsdService.this.writeLog(logData);
		}

		@Override
		public long getExtraRecordingId() throws RemoteException {
			return extraRecordingFileId;
		}

		@Override
		public boolean startExtraRecording(String filename) throws RemoteException {
			try{
				activeTestTimestamp = System.currentTimeMillis();
				return MsdService.this.startExraRecording(filename);
			} catch(Exception e){
				handleFatalError("Exception in startExtraRecording:", e);
				return false;
			}
		}

		@Override
		public boolean endExtraRecording(boolean markForUpload)
				throws RemoteException {
			try{
				activeTestTimestamp = System.currentTimeMillis();
				return MsdService.this.endExtraRecording(markForUpload);
			} catch(Exception e){
				handleFatalError("Exception in endExtraRecording:", e);
				return false;
			}
		}

		@Override
		public void triggerUploading() throws RemoteException {
			MsdService.this.triggerUploading();
		}

		@Override
		public long reopenAndUploadDebugLog() throws RemoteException {
			return MsdService.this.reopenAndUploadDebugLog();
		}

		@Override
		public int getParserNetworkGeneration() throws RemoteException {
			return parserRatGeneration;
		}

		@Override
		public synchronized void startActiveTest() throws RemoteException {
			activeTestTimestamp = System.currentTimeMillis();
			if(!isRecording()){
				startRecording();
				recordingStartedForActiveTest = true;
			}
		}

		@Override
		public void stopActiveTest() throws RemoteException {
			activeTestTimestamp = 0;
			if(recordingStartedForActiveTest){
				stopRecording();
				recordingStartedForActiveTest = false;
			}
		}
	};
	AtomicBoolean shuttingDown = new AtomicBoolean(false);

	Process helper;
	DataInputStream diagStdout;
	DataOutputStream diagStdin;
	private BufferedReader diagStderr;
	FromDiagThread fromDiagThread;

	private BlockingQueue<DiagMsgWrapper> toParserMsgQueue = new LinkedBlockingQueue<DiagMsgWrapper>();
	private BlockingQueue<QueueElementWrapper<byte[]>> toDiagMsgQueue = new LinkedBlockingQueue<MsdService.QueueElementWrapper<byte[]>>();
	private BlockingQueue<PendingSqliteStatement> pendingSqlStatements = new LinkedBlockingQueue<PendingSqliteStatement>();
	private long pendingSqlStatementsEmptyTimestamp = 0;

	private BufferedReader parserStdout;
	private DataOutputStream parserStdin;
	private BufferedReader parserStderr;
	private Process parser;
	private SqliteThread sqliteThread = null;
	private ParserErrorThread parserErrorThread;
	private FromParserThread fromParserThread;
	private ToParserThread toParserThread;
	private ToDiagThread toDiagThread;
	private DiagErrorThread diagErrorThread;
	private volatile boolean shutdownError = false;
	private AtomicBoolean readyForStartRecording = new AtomicBoolean(true);
	private PeriodicCheckRecordingStateRunnable periodicCheckRecordingStateRunnable = new PeriodicCheckRecordingStateRunnable();
	private ExceptionHandlingRunnable periodicCheckRecordingStateRunnableWrapper = new ExceptionHandlingRunnable(periodicCheckRecordingStateRunnable);
	private PeriodicFlushRunnable periodicFlushRunnable = new PeriodicFlushRunnable();
	private boolean recording = false;
	private LocationManager locationManager;
	private MyLocationListener myLocationListener;
	private MyPhoneStateListener myPhoneStateListener;
	private TelephonyManager telephonyManager;
	private PendingSqliteStatement last_sc_insert;
	private int sqlQueueWatermark = 0;
	private long oldHeapSize = 0;
	private long oldStackSize = 0;
	private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
	private boolean fatalErrorOccured = false;

	private MsdServiceNotifications msdServiceNotifications = new MsdServiceNotifications(this);

	private EncryptedFileWriter rawWriter;
	private long rawLogFileId = 0;
	private EncryptedFileWriter debugLogWriter;
	private long debugLogFileStartTime = 0;
	private long debugLogFileId = 0;

	private Object currentRawWriterBaseFilename;

	private StringBuffer logBuffer = null;

	private long extraRecordingStartTime = 0;
	private EncryptedFileWriter extraRecordingRawFileWriter;
	private long extraRecordingFileId = 0;

	private MsdServiceUploadThread uploadThread = null;
	private DownloadDataJsThread downloadDataJsThread = null;

	public int parserRatGeneration = 0;

	private long lastCleanupTime = 0;

	class QueueElementWrapper<T>{
		T obj;
		boolean done = false;
		public QueueElementWrapper(T obj) {
			this.obj = obj;
		}
		public QueueElementWrapper() {
			this.done = true;
		}
		public boolean isDone() {
			return done;
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		info("MsdService.onBind() called");
		return mBinder;
	}

	public void triggerUploading() {
		info("MsdService.triggerUploading() called");
		if(uploadThread != null && uploadThread.isAlive())
			uploadThread.requestUploadRound();
		if(uploadThread == null || !uploadThread.isAlive()){
			uploadThread = new MsdServiceUploadThread(this);
			uploadThread.requestUploadRound();
			info("MsdService.triggerUploading() calling uploadThread.start()");
			uploadThread.start();
		}
		// Refresh the app_data.json file
		if(downloadDataJsThread == null || !downloadDataJsThread.isAlive()){
			downloadDataJsThread = new DownloadDataJsThread();
			downloadDataJsThread.start();
		}
	}

	public long reopenAndUploadDebugLog() {
		long result = openOrReopenDebugLog(true,true);
		triggerUploading();
		return result;
	}

	public boolean endExtraRecording(boolean markForUpload) {
		if(extraRecordingRawFileWriter == null)
			return false;
		EncryptedFileWriter copyExtraRecordingRawFileWriter = extraRecordingRawFileWriter;
		extraRecordingRawFileWriter = null;
		copyExtraRecordingRawFileWriter.close();
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		DumpFile df = DumpFile.get(db, extraRecordingFileId);
		df.endRecording(db);
		if(markForUpload){
			df.updateState(db, DumpFile.STATE_AVAILABLE, DumpFile.STATE_PENDING, null);
		}
		MsdDatabaseManager.getInstance().closeDatabase();
		extraRecordingStartTime = 0;
		extraRecordingFileId = 0;
		if(markForUpload)
			triggerUploading();
		return true;
	}

	public boolean startExraRecording(String filename) {
		if(!recording)
			return false;
		this.extraRecordingStartTime = System.currentTimeMillis();
		this.extraRecordingRawFileWriter = new EncryptedFileWriter(this, filename + ".gz.smime", true, filename + ".gz", true);
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		DumpFile df = new DumpFile(filename + ".gz.smime",DumpFile.TYPE_ENCRYPTED_QDMON);
		df.insert(db);
		this.extraRecordingFileId = df.getId();
		MsdDatabaseManager.getInstance().closeDatabase();
		return true;
	}

	public void writeLog(String logData) {
		if(debugLogWriter == null){
			if(logBuffer == null){
				logBuffer = new StringBuffer();
				logBuffer.append(logData);
			}
		} else{
			debugLogWriter.write(logData);
			debugLogWriter.flushIfUnflushedDataSince(10000);
		}
	}

	class PeriodicCheckRecordingStateRunnable implements Runnable{
		@Override
		public void run() {
			checkRecordingState();
			mainThreadHandler.postDelayed(periodicCheckRecordingStateRunnableWrapper, 5000);
		}
	}

	class PeriodicFlushRunnable implements Runnable{
		@Override
		public void run() {
			if(shuttingDown.get())
				return;
			if(debugLogWriter != null)
				debugLogWriter.flushIfUnflushedDataSince(10000);
			mainThreadHandler.postDelayed(new ExceptionHandlingRunnable(this), 5000);
		}
	}

	/**
	 * This wrapper class handles all uncaught Exceptions in a Runnable. This is
	 * neccessary since Thread.setDefaultUncaughtExceptionHandler will stop the
	 * main Looper Thread and we still need it for the shutdown of the Service.
	 * 
	 * 
	 */
	class ExceptionHandlingRunnable implements Runnable{
		Runnable r;
		public ExceptionHandlingRunnable(Runnable r) {
			this.r = r;
		}
		@Override
		public void run() {
			try{
				r.run();
			} catch(Exception e){
				handleFatalError("Uncaught Exception in ExceptionHandlingRunnable => " + r.getClass(), e);
			}
		}
	}


	@Override
	public void onCreate() {
		super.onCreate();
		MsdLog.init(this);
		MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(MsdService.this));
		cleanupIncompleteOldFiles();
		openOrReopenDebugLog(false,false);
		mainThreadHandler.post(new ExceptionHandlingRunnable(periodicFlushRunnable));
		Thread.setDefaultUncaughtExceptionHandler(
				new Thread.UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						handleFatalError("Uncaught Exception in MsdService Thread " + t.getClass(), e);
					}
				});
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		info("MsdService.onCreate() called");
		sendStateChanged(StateChangedReason.ANALYSIS_DONE);
	}
	private void doStartForeground() {
		Notification notification = msdServiceNotifications.getForegroundNotification();
		startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, notification);
	}
	private void doStopForeground(){
		stopForeground(true);
	}
	@Override
	public void onDestroy() {
		info("MsdService.onDestroy() called, shutting down");
		shutdown(false);
		closeDebugLog(false);
		super.onDestroy();
	}
	private void startLocationRecording(){
		myLocationListener = new MyLocationListener();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if(MsdConfig.gpsRecordingEnabled(MsdService.this))
			locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 10000, 0, myLocationListener);
		if(MsdConfig.networkLocationRecordingEnabled(MsdService.this))
			locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 10000, 0, myLocationListener);
	}
	private void stopLocationRecording(){
		if(locationManager == null)
			return;
		locationManager.removeUpdates(myLocationListener);
		myLocationListener = null;
		locationManager = null;
	}
	private void startPhoneStateRecording(){
		myPhoneStateListener = new MyPhoneStateListener();
		telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CELL_INFO|PhoneStateListener.LISTEN_CELL_LOCATION);
	}
	private void stopPhoneStateRecording(){
		telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		myPhoneStateListener = null;
	}
	private synchronized boolean startRecording() {
		if(!readyForStartRecording.compareAndSet(true, false)){
			handleFatalError("MsdService.startRecording called but readyForStartRecording is not true. Probably there was an error during the last shutdown");
			return false;
		}
		try {
			info("startRecording() called");
			this.shuttingDown.set(false);
			this.sqliteThread = new SqliteThread();
			sqliteThread.start();
			launchParser();
			openOrReopenRawWriter();
			launchHelper();
			mainThreadHandler.post(new ExceptionHandlingRunnable(new Runnable(){
				@Override
				public void run() {
					startLocationRecording();					
				}
			}));
			mainThreadHandler.post(new ExceptionHandlingRunnable(new Runnable(){
				@Override
				public void run() {
					startPhoneStateRecording();
				}
			}));
			this.recording  = true;
			mainThreadHandler.removeCallbacks(periodicCheckRecordingStateRunnableWrapper);
			mainThreadHandler.post(periodicCheckRecordingStateRunnableWrapper);
			doStartForeground();
			sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
			//  Use the following snippet to test handling of fatal errors.
			//			mainThreadHandler.postDelayed(new ExceptionHandlingRunnable(new Runnable() {				
			//				@Override
			//				public void run() {
			//					throw new IllegalStateException("Let's test error reporting");
			//				}
			//			}), 3000);
			info("startRecording() finished successfully");
			return true;
		} catch (Exception e) { 
			handleFatalError("Exception in startRecording(): ", e);
			return false;
		}
	}

	private synchronized boolean shutdown(boolean shuttingDownAlreadySet){
		try{
			info("MsdService.shutdown(" + shuttingDownAlreadySet + ") called");
			if(shuttingDownAlreadySet){
				if(!this.shuttingDown.get()){
					handleFatalError("MsdService.shutdown(true) called but shuttingDown is not set");
					return false;
				}
			} else{
				if (!this.shuttingDown.compareAndSet(false, true)){
					handleFatalError("MsdService.shutdown(false) called while shuttingDown is already set");
					return false;
				}
			}
			shutdownError = false;
			mainThreadHandler.removeCallbacks(periodicCheckRecordingStateRunnableWrapper);
			// DIAG Helper
			if(helper != null){
				try{
					this.helper.exitValue();
				} catch(IllegalThreadStateException e){
					// The helper is still running, so let's send DisableLoggingCmds
					if(toDiagThread != null){
						for(byte[] singleCmd:DisableLoggingCmds.cmds){
							this.toDiagMsgQueue.add(new QueueElementWrapper<byte[]>(singleCmd));
						}
					}
				}
			}
			if(toDiagThread != null){
				this.toDiagMsgQueue.add(new QueueElementWrapper<byte[]>()); // Send shutdown marker to message queue
				this.toDiagThread.join(3000);

				if(toDiagThread.isAlive()){
					handleFatalError("Failed to stop toDiagThread");
				}
			}
			if(diagStdin != null){
				try {
					info("Closing diagStdin");
					this.diagStdin.close();
				} catch (IOException e) {
					handleFatalError("IOException while closing diagStdin" , e);
				}
			}
			if(helper != null){
				Thread t = new Thread(){
					@Override
					public void run() {
						try{
							helper.waitFor();
						} catch(InterruptedException e){
						}
					};
				};
				t.start();
				t.join(3000);
				t.interrupt();
				try{
					int exitValue = helper.exitValue();
					info("Helper terminated with exit value " + exitValue);
				} catch(IllegalThreadStateException e){
					handleFatalError("Failed to stop diag helper, calling destroy(): " + e.getMessage());		
					helper.destroy();	
				}
				helper = null;
			}
			if(fromDiagThread != null)
				this.fromDiagThread.join();
			diagStdin = null;
			diagStdout = null;
			diagStderr = null;
			// Terminate rawWriter
			closeRawWriter();
			// Termiante parser
			if(parser != null){
				toParserMsgQueue.add(new DiagMsgWrapper()); // Send shutdown marker to message queue
				this.toParserThread.join(3000);
				if(toParserThread.isAlive()){
					handleFatalError("Failed to stop toParserThread");
				}
				try{
					this.parserStdin.close();
				} catch (IOException e) {
					handleFatalError("IOException while closing parserStdin" , e);
				}
				info("Waiting for parser to terminate after closing parserStdin");
				Thread t = new Thread(){
					@Override
					public void run() {
						try{
							parser.waitFor();
						} catch(InterruptedException e){
						}
					};
				};
				t.start();
				t.join(3000);
				t.interrupt();
				try{
					int exitValue = parser.exitValue();
					info("Parser terminated with exit value " + exitValue);
				} catch(IllegalThreadStateException e){
					handleFatalError("Failed to stop parser, calling destroy(): " + e.getMessage());
					parser.destroy();
				}
				this.fromParserThread.interrupt();
				this.fromParserThread.join(3000);
				if(this.fromParserThread.isAlive()){
					handleFatalError("Failed to stop fromParserThread");
				}
				this.parserErrorThread.join(3000);
				if(this.parserErrorThread.isAlive()){
					handleFatalError("Failed to stop parserErrorThread");
				}
				parser = null;
				parserStdin = null;
				parserStdout = null;
				parserStderr = null;
			}
			stopLocationRecording();
			stopPhoneStateRecording();
			if(sqliteThread != null){
				sqliteThread.shuttingDown = true;
				// Add finish marker at end of pendingSqlStatements so that sqliteThread shuts down
				pendingSqlStatements.add(new PendingSqliteStatement(null));
				sqliteThread.join(3000);
				if(sqliteThread.isAlive()){
					handleFatalError("Failed to stop sqliteThread");
				}
				sqliteThread = null;
			}
			if(!toDiagMsgQueue.isEmpty()){
				handleFatalError("shutdown(): toDiagMsgQueue is not empty");			
			}
			if(!toParserMsgQueue.isEmpty()){
				handleFatalError("shutdown(): diagMsgQueue is not empty");
			}
			if(!pendingSqlStatements.isEmpty()){
				handleFatalError("shutdown(): pendingSqlStatements is not empty");			
			}
			if(!shutdownError)
				info("MsdService.shutdown completed successfully");
			this.recording = false;
			this.shuttingDown.set(false);
			sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
			this.readyForStartRecording.set(!shutdownError);
			doStopForeground();
			if(debugLogWriter != null)
				debugLogWriter.flush();
			return !shutdownError;
		} catch (Exception e) {
			// Prevent data loss by making sure that rawWriter is always closed during shutdown
			if(rawWriter != null){
				rawWriter.close();
				rawWriter = null;
			}
			handleFatalError("Received Exception during shutdown", e);
			return false;
		}
	}

	/**
	 * 
	 * This Thread handles communication with the diag helper process
	 *
	 */
	class FromDiagThread extends Thread{
		@Override
		public void run() {
			try {
				while (true) {
					int data_len = MsdService.this.diagStdout.readInt();
					byte[] individual_buf = new byte[data_len];
					MsdService.this.diagStdout.readFully(individual_buf, 0, data_len);
					for(byte[] buf:fromDev(individual_buf, 0, individual_buf.length)){
						DiagMsgWrapper msg = new DiagMsgWrapper(buf);
						toParserMsgQueue.add(msg);
						rawWriter.write(buf);
						if(extraRecordingRawFileWriter != null){
							try{
								extraRecordingRawFileWriter.write(buf);
							} catch(NullPointerException e){
								// The check extraRecordingRawFileWriter != null is not thread safe, so let's just ignore a NullPointerException
							}
						}
					}
				}
			} catch (EOFException e) {
				if(shuttingDown.get()){
					info("FromDiagThread shutting down due to EOFException while shuttingDown is set");
				} else{
					handleFatalError("FromDiagThread received EOFException but shuttingDown is not set!");
				}
			} catch (IOException e) {
				handleFatalError("FromDiagThread received IOException but shuttingDown is not set!");
			}
		}
	}
	class ToDiagThread extends Thread{
		@Override
		public void run() {
			try {
				while(true){
					QueueElementWrapper<byte[]> elem = toDiagMsgQueue.take();
					if(elem.isDone()){
						if(shuttingDown.get())
							info("ToDiagThread received DONE from toDiagMsgQueue while shuttingDown is set, OK");
						else
							handleFatalError("ToDiagThread received DONE from toDiagMsgQueue but shuttingDown is not set");
						return;
					}					
					byte[] msgData = elem.obj;
					byte[] frame = toDev(new DiagMsg(msgData).frame());
					diagStdin.writeInt(frame.length);
					diagStdin.write(frame);
					diagStdin.flush();
				}
			} catch (InterruptedException e) {
				if(shuttingDown.get())
					info("ToDiagThread shutting down due to InterruptedException while shuttingDown is set, OK");
				else
					handleFatalError("ToDiagThread received InterruptedException but shuttingDown is not set",e);					
			} catch (IOException e) {
				handleFatalError("ToDiagThread: IOException while writing to helper: " + e.getMessage());
			}
		}
	}
	class DiagErrorThread extends Thread{
		@Override
		public void run() {
			try {
				while(true){
					String line = diagStderr.readLine();
					if(line == null){
						if(shuttingDown.get()){
							info("diagStderr.readLine() returned null while shutting down, OK");
						} else{
							handleFatalError("diagStderr.readLine() returned null");
						}
						return;
					}
					handleFatalError(line);
				}
			} catch(EOFException e){
				if(shuttingDown.get()){
					info("DiagErrorThread received EOFException while shutting down, OK");
				} else{
					handleFatalError("EOFException while reading from diagStderr: " + e.getMessage());
				}
			} catch(IOException e){
				handleFatalError("IOException while reading from diagStderr: " + e.getMessage());				
			}
		}
	}
	class DiagMsgWrapper{
		byte[] buf;
		boolean shutdownMarker = false;
		public DiagMsgWrapper() {
			shutdownMarker = true;
		}
		public DiagMsgWrapper(byte[] buf){
			this.buf = buf;
		}
	}
	/**
	 * This Thread takes diag messages from diagMsgQueue and writes the messages to the parser.
	 *
	 */
	class ToParserThread extends Thread{
		@Override
		public void run() {
			try {
				while(true){
					DiagMsgWrapper msg = toParserMsgQueue.take();
					if(msg.shutdownMarker){
						info("ToParserThread shutting down due to shutdown marker, OK");
						return;
					}
					//info("Writing message to parser, length=" + msg.buf.length);
					parserStdin.write(msg.buf);
					parserStdin.flush();
				}
			} catch (InterruptedException e) {
				handleFatalError("ToParserThread shutting down due to InterruptedException");
			} catch (IOException e) {
				if(MsdService.this.shuttingDown.get())
					info("ToParserThread: IOException while writing to parser while shutting down: " + e.getMessage());
				else
					handleFatalError("ToParserThread: IOException while writing to parser: " + e.getMessage());
			}
		}
	}

	class FromParserThread extends Thread{
		@Override
		public void run() {
			try {
				while(true){
					String line = parserStdout.readLine();
					if(line == null){
						if(shuttingDown.get()){
							info("parserStdout.readLine() returned null while shutting down, OK");
						} else{
							handleFatalError("parserStdout.readLine() returned null");
						}
						return;
					}
					if(line.trim().length() == 0)
						continue; // Ignore empty lines
					if(line.startsWith("SQL:")){
						String sql = line.substring(4);
						info("FromParserThread enqueueing SQL Statement: " + sql);
						pendingSqlStatements.add(new PendingSqliteStatement(sql));
					} else if(line.startsWith("RAT:")){
						String parserRat = line.substring("RAT:".length()).trim();
						info("Parser RAT: " + parserRat);
						if(parserRat.equals("GSM")){
							parserRatGeneration = 2;
						} else if(parserRat.equals("3G")){
							parserRatGeneration = 3;
						} else if(parserRat.equals("LTE")){
							parserRatGeneration = 4;
						} else if(parserRat.equals("UNKNOWN")){
							parserRatGeneration = 0;
						} else{
							handleFatalError("Invalid RAT: output from parser: " + line);
						}
					} else{
						info("Parser: " + line);
					}
				}
			} catch(IOException e){
				if(shuttingDown.get()){
					info("FromParserThread received IOException while shutting down, OK");
				} else{
					handleFatalError("IOException while reading from Parser: " + e.getMessage());
				}
			}
		}
	}
	class ParserErrorThread extends Thread{
		@Override
		public void run() {
			try {
				while(true){
					String line = parserStderr.readLine();
					if(line == null){
						if(shuttingDown.get()){
							info("parserStderr.readLine() returned null while shutting down, OK");
						} else{
							handleFatalError("parserStderr.readLine() returned null");
						}
						return;
					}
					handleFatalError("Parser Error: " + line);
				}
			} catch(EOFException e){
				if(shuttingDown.get()){
					info("ParserErrorThread received IOException while shutting down, OK");
				} else{
					handleFatalError("EOFException while reading from parserStderr: " + e.getMessage());
				}
			} catch(IOException e){
				handleFatalError("IOException while reading from parserStderr: " + e.getMessage());
			}
		}
	}
	/**
	 * This Thread takes SQL statements from pendingSqlStatements and executes them, using transactions when possible
	 *
	 */
	class SqliteThread extends Thread{
		boolean shuttingDown = false;
		private long lastAnalysisTime;

		@Override
		public void run() {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
			lastAnalysisTime = System.currentTimeMillis();
			MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(MsdService.this));
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			while(true){
				try {
					if(shuttingDown && pendingSqlStatements.isEmpty()){
						info("SqliteThread shutting down due to shuttingDown && pendingSqlStatements.isEmpty()");
						return;
					}
					if(pendingSqlStatements.isEmpty())
						pendingSqlStatementsEmptyTimestamp = System.currentTimeMillis();
					PendingSqliteStatement sql = pendingSqlStatements.take();
					if(sql.isShutdownMarker()){
						if(shuttingDown)
							info("SqliteThread terminating due to finish marker while shuttingDown is set");
						else
							handleFatalError("SqliteThread received finish marker but shutdown is not set!");
						return;
					}
					try{
						sql.run(db);
						sql.postRunHook();
					} catch(SQLException e){
						handleFatalError("SQLException " + e.getMessage() + " while running: " + sql);
						return;
					}
					if(System.currentTimeMillis() - lastAnalysisTime > Constants.ANALYSIS_INTERVAL_MS){
						info("Starting analysis");
						wl.acquire();
						class AnalysisStackTraceLogRunnable implements Runnable{
							Thread t = Thread.currentThread();
							boolean stopped = false;
							@Override
							public void run() {
								if(stopped)
									return;
								StackTraceElement stackTrace[] = t.getStackTrace();
								info("Analysis Stack trace:");
								for(StackTraceElement e:stackTrace){
									info("  " + e);
								}
								mainThreadHandler.postDelayed(new ExceptionHandlingRunnable(this),100);
							}
						}
						AnalysisStackTraceLogRunnable analysisStackTraceLogRunnable = null;
						// For debugging delays in the analysis, we can dump a Stack Trace of this Thread every 100 ms.
						boolean dumpAnalysisStackTraces = true;
						if(dumpAnalysisStackTraces){
							analysisStackTraceLogRunnable = new AnalysisStackTraceLogRunnable();
							mainThreadHandler.post(new ExceptionHandlingRunnable(analysisStackTraceLogRunnable));
						}
						try{
							long analysisStartCpuTimeNanos = android.os.Debug.threadCpuTimeNanos();
							Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
							String time = String.format(Locale.US, "%02d:%02d:%02d",
									start.get(Calendar.HOUR_OF_DAY),
									start.get(Calendar.MINUTE),
									start.get(Calendar.SECOND));
							updateLocationInfo(db,false);
							info("updateLocationInfo done");
							// Update location info

							if (MsdServiceAnalysis.runCatcherAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.CATCHER_DETECTED);
							};
							if (MsdServiceAnalysis.runEventAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.SMS_DETECTED);
							};
							if (MsdServiceAnalysis.runSecurityAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.SEC_METRICS_CHANGED);
							};
							lastAnalysisTime = System.currentTimeMillis();

							info(time + ": Analysis took " + (lastAnalysisTime - start.getTimeInMillis()) + "ms" + " CPU=" + (android.os.Debug.threadCpuTimeNanos()-analysisStartCpuTimeNanos)/1000000 + "ms");
							if(dumpAnalysisStackTraces){
								analysisStackTraceLogRunnable.stopped = true;
							}
							sendStateChanged(StateChangedReason.ANALYSIS_DONE);

							// TODO: This should be done somewhere else, when we really detect a change from
							// telephony service
							sendStateChanged(StateChangedReason.RAT_CHANGED);

						} catch(Exception e){
							// Terminate the service with a fatal error if there is a any uncaught Exception in the Analysis
							handleFatalError("Exception during analysis",e);
						}
						wl.release();
					}
				} catch (InterruptedException e) {
					if(!pendingSqlStatements.isEmpty()){
						handleFatalError("SqliteThread received InterruptedException but pendingSqlStatements is not empty!");
					}
					info("SqliteThread terminating due to InterruptedException");
					return;
				}
			}
		}
		void updateLocationInfo(SQLiteDatabase db, boolean allTimes){
			Cursor location_info = null, session_info = null;
			try{
				class LocationRow{
					long timestamp;
					double latitude;
					double longitude;
				}
				LocationRow loc1 = null, loc2 = null;
				String location_info_sql = "SELECT timestamp, latitude, longitude FROM location_info";
				if(!allTimes)
					location_info_sql += " WHERE timestamp > datetime('now','-30 minutes');";
				location_info = db.rawQuery(location_info_sql, null);
				if(!location_info.moveToFirst()){
					return; // No location updates in past 15 minutes => Nothing to do
				}
				String session_info_sql = "SELECT id, timestamp FROM session_info";
				if(!allTimes)
					session_info_sql += " WHERE timestamp > datetime('now','-15 minutes');";
				session_info = db.rawQuery(session_info_sql, null);
				while(session_info.moveToNext()){
					long session_id = session_info.getLong(session_info.getColumnIndexOrThrow("id"));
					long session_timestamp_ms = session_info.getLong(session_info.getColumnIndexOrThrow("timestamp")) * 1000L;
					if(session_info.getPosition() == 0){
						db.delete("si_loc", "id >= " + session_id,null);
					}
					// loc1 should be before the current session and loc2 after the current session. Then we can just pick the nearest timestamp.
					// If only one location update was found, it is stored in loc1
					while((loc1 == null || loc2 == null || loc2.timestamp < session_timestamp_ms) && location_info.moveToNext()){
						LocationRow new_loc = new LocationRow();
						new_loc.timestamp = location_info.getLong(location_info.getColumnIndexOrThrow("timestamp")) * 1000L;
						new_loc.latitude = location_info.getDouble(location_info.getColumnIndexOrThrow("latitude"));
						new_loc.longitude = location_info.getDouble(location_info.getColumnIndexOrThrow("longitude"));
						if(loc1 == null){
							loc1 = new_loc;
						} else if(loc2 == null){
							loc2 = new_loc;
						} else{
							loc1 = loc2;
							loc2 = new_loc;
						}
					}
					// Find the best match
					LocationRow loc = loc1;
					if(loc2 != null && Math.abs(session_timestamp_ms - loc2.timestamp) < Math.abs(session_timestamp_ms - loc.timestamp)){
						loc = loc2;
					}
					if(loc != null){
						ContentValues values = new ContentValues();
						values.put("id", session_id);
						values.put("latitude",loc.latitude);
						values.put("longitude", loc.longitude);
						values.put("valid", (Math.abs(session_timestamp_ms - loc.timestamp) < 1000 * Constants.LOC_MAX_DELTA) ? 1:0);
						db.insert("si_loc", null, values);
					}
				}
			} catch(Exception e){
				handleFatalError("Error in updateLocationInfo",e);
			} finally{
				if(location_info != null)
					location_info.close();
				if(session_info != null)
					session_info.close();
			}
		}
	}
	class PendingSqliteStatement{
		Long generatedRowId = null;
		String table = null;
		ContentValues values = null;
		String sql = null;
		public PendingSqliteStatement(String sql) {
			this.sql = sql;
		}
		public PendingSqliteStatement(String table, ContentValues values) {
			this.table = table;
			this.values = values;
		}
		/**
		 * SqliteThread will terminate when encountering an object with neither a table name nor an SQL statement set.
		 * @return
		 */
		public boolean isShutdownMarker(){
			return sql == null && table == null;
		}
		public void run(SQLiteDatabase db) throws SQLException{
			preRunHook();
			if(table != null){
				generatedRowId = db.insert(table, null, values);
			}
			if(sql != null){
				for(String statement:sql.split(";")){
					if(statement.trim().length() > 0){
						db.execSQL(statement);
					}
				}
			}
		}
		@Override
		public String toString() {
			if(sql != null)
				return sql;
			else
				return "INSERT " + table + ": " + values.toString();
		}

		/**
		 * This method is run directly before executing the SQL statement. It
		 * can be overwritten to add a reference to a row inserted by another
		 * pending statement (which must be before this statement in the queue).
		 */
		void preRunHook(){}
		/**
		 * This method is run directly after executing the SQL statement.
		 */
		void postRunHook(){}
	}
	class MyLocationListener implements LocationListener{
		@Override
		public void onLocationChanged(Location location) {
			ContentValues values = new ContentValues();
			if(location.hasAccuracy())
				values.put("accuracy",location.getAccuracy());
			values.put("latitude", location.getLatitude());
			values.put("longitude", location.getLongitude());
			if(location.hasAltitude())
				values.put("altitude", location.getAltitude());
			values.put("provider_name",location.getProvider());
			pendingSqlStatements.add(new PendingSqliteStatement("location_info", values));
		}
		@Override
		public void onProviderDisabled(String provider) {
		}
		@Override
		public void onProviderEnabled(String provider) {
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}
	@SuppressLint("NewApi")
	class MyPhoneStateListener extends PhoneStateListener{
		@Override
		public void onCellLocationChanged(CellLocation location) {
			if (location instanceof GsmCellLocation) {
				GsmCellLocation gsmLoc = (GsmCellLocation) location;
				String networkOperator = telephonyManager.getNetworkOperator();
				if(networkOperator.length() < 5){
					warn("Invalid networkOperatr: " + networkOperator);
					return;
				}
				String mcc = networkOperator.substring(0,3);
				String mnc = networkOperator.substring(3);
				ContentValues values = new ContentValues();
				values.put("mcc", mcc);
				values.put("mnc", mnc);
				values.put("network_type", telephonyManager.getNetworkType());
				int cid = gsmLoc.getCid();
				if(cid != Integer.MAX_VALUE && cid != -1)
					values.put("cid", cid);
				int lac = gsmLoc.getLac();
				if(lac != Integer.MAX_VALUE && lac != -1)
					values.put("lac", lac);
				values.put("psc", gsmLoc.getPsc());
				last_sc_insert = new PendingSqliteStatement("serving_cell_info", values);
				pendingSqlStatements.add(last_sc_insert);
				// Some phones may not send onCellInfoChanged(). So let's record
				// the neighboring cells as well if the current serving cell
				// changes.
				doCellinfoList(telephonyManager.getAllCellInfo());
			} else
				warn("onCellLocationChanged() called with invalid location class: " + location.getClass());
		}

		@Override
		public void onCellInfoChanged(List<CellInfo> cellInfo) {
			if(cellInfo == null || cellInfo.size() == 0)
				return;
			// I do not know whether this code will be reached at all. Most
			// phones only call this method with null as a parameter. So let's
			// send a message so that we find out when it is called.
			String msg = "onCellInfoChanged(" + ((cellInfo == null) ? "null" : cellInfo.size()) + ")"; 
			info(msg);
		}
		void doCellinfoList(List<CellInfo> cellInfo){
			if(cellInfo != null){
				if(last_sc_insert != null){
					for(CellInfo ci:cellInfo){
						if (ci instanceof CellInfoGsm) {
							CellInfoGsm ci_gsm = (CellInfoGsm) ci;
							ContentValues values = new ContentValues();
							values.put("mcc",ci_gsm.getCellIdentity().getMcc());
							values.put("mnc", ci_gsm.getCellIdentity().getMnc());
							values.put("lac", ci_gsm.getCellIdentity().getLac());
							values.put("cid",ci_gsm.getCellIdentity().getCid());
							NeighboringPendingSqliteStatement stmt = new NeighboringPendingSqliteStatement(values, last_sc_insert);
							pendingSqlStatements.add(stmt);
						} else if (ci instanceof CellInfoWcdma) {
							CellInfoWcdma ci_wcdma = (CellInfoWcdma) ci;
							ContentValues values = new ContentValues();
							values.put("mcc",ci_wcdma.getCellIdentity().getMcc());
							values.put("mnc", ci_wcdma.getCellIdentity().getMnc());
							values.put("lac", ci_wcdma.getCellIdentity().getLac());
							values.put("cid",ci_wcdma.getCellIdentity().getCid());
							values.put("psc",ci_wcdma.getCellIdentity().getPsc());
							NeighboringPendingSqliteStatement stmt = new NeighboringPendingSqliteStatement(values, last_sc_insert);
							pendingSqlStatements.add(stmt);
						}
					}
				}
			}
		}
	}
	class NeighboringPendingSqliteStatement extends PendingSqliteStatement{
		PendingSqliteStatement last_sc_insert;
		public NeighboringPendingSqliteStatement(ContentValues values, PendingSqliteStatement last_sc_insert) {
			super("neighboring_cell_info", values);
			this.last_sc_insert = last_sc_insert;
		}
		@Override
		void preRunHook() {
			this.values.put("last_sc_id", last_sc_insert.generatedRowId);
		}
	}

	class DownloadDataJsThread extends Thread{

		private GSMmap gsmmap;

		public DownloadDataJsThread() {
			this.gsmmap = new GSMmap(MsdService.this);
		}

		@Override
		public void run() {
			// info("DownloadDataJsThread.run() called");
			// Check for a new version at most once in 24 hours
			long lastCheckTime = PreferenceManager.getDefaultSharedPreferences(MsdService.this).getLong("data_js_last_check_time",0);
			if(System.currentTimeMillis() > lastCheckTime + 24*3600*1000){
				try {
					info("DownloadDataJsThread.run(): Checking if there is a new version on the server");
					// Using Apache HttpClient since HttpURLConnection is very buggy:
					// http://stackoverflow.com/questions/14454942/httpurlconnection-ifmodifiedsince-generates-utc-time-instead-of-gmt
					// https://code.google.com/p/android/issues/detail?id=58637
					// 
					HttpClient httpClient = new DefaultHttpClient();
					// The Apache HttpClient library shipped with Android does not support SNI.
					// http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache
					SchemeRegistry schemeRegistry = httpClient.getConnectionManager().getSchemeRegistry();
					schemeRegistry.register(new Scheme("https", new TlsSniSocketFactory(), 443));
					HttpGet httpGet = new HttpGet("https://gsmmap.org/assets/data/app_data.json");
					String localFileLastModified = PreferenceManager.getDefaultSharedPreferences(MsdService.this).getString("data_js_last_modified_header",null);
					if(localFileLastModified != null){
						httpGet.addHeader("If-Modified-Since",localFileLastModified);
					}
					HttpResponse resp = httpClient.execute(httpGet);
					int statusCode = resp.getStatusLine().getStatusCode();
					if(statusCode == 200){ // OK
						InputStream in = resp.getEntity().getContent();
						// Cache everything in memory and write it to disk only
						// if we are sure that the file has been fully
						// transmitted
						ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
						int i;
						i = in.read();
						while (i != -1)
						{
							byteArrayOutputStream.write(i);
							i = in.read();
						}
						in.close();
						byte[] buf = byteArrayOutputStream.toByteArray();
						info("Received new data.json, size=" + buf.length);
						FileOutputStream os = openFileOutput("app_data.json", 0);
						os.write(buf);
						// Update saved last modified time
						Header[] lastModifiedHeaders = resp.getHeaders("Last-Modified");
						if(lastModifiedHeaders.length == 1){
							String lastModifiedHeader = lastModifiedHeaders[0].getValue();
							info("lastModifiedHeader: " + lastModifiedHeader);
							Editor editor = PreferenceManager.getDefaultSharedPreferences(MsdService.this).edit();
							editor.putString("data_js_last_modified_header",lastModifiedHeader);
							editor.commit();
						}
						//  Parse data
						try{
							gsmmap.parse(new String(buf));
						} catch(Exception e){
							handleFatalError("Exception while parsing newly downloaded app_data.json",e);
						}
						//  FIXME: Should have a dedicated reason for changed data
						sendStateChanged(StateChangedReason.ANALYSIS_DONE);
					} else if(statusCode == 304){ // Not Modified
						info("checkAndDownloadDataJs() received 304 not modified response");
					} else{ // Unexpected
						MsdLog.e(TAG,"Unexpected HTTP response code" + statusCode + " in DownloadDataJsThread.run()");
						return;
					}				
				} catch (Exception e) {
					MsdLog.e(TAG,"Exception in DownloadDataJsThread.run()",e);
					return;
				}
				// Update last check time
				Editor editor = PreferenceManager.getDefaultSharedPreferences(MsdService.this).edit();
				editor.putLong("data_js_last_check_time",System.currentTimeMillis());
				editor.commit();
			}
		}
	}
	/**
	 * Sets up the parser
	 * * Launch parser binary
	 * * Wait for handshake
	 * * starts parserErrorThread, fromParserThread and toParserThread
	 * @throws IOException
	 */
	private void launchParser() throws IOException {
		if(parser != null)
			throw new IllegalStateException("launchParser() called but parser!=null");
		String libdir = this.getApplicationInfo().nativeLibraryDir;
		String parser_binary = libdir + "/libdiag_import.so";
		long nextSessionInfoId = getNextRowId("session_info");
		long nextCellInfoId = getNextRowId("cell_info");
		String cmd[] = {parser_binary, "" + nextSessionInfoId, "" + nextCellInfoId};
		info("Launching parser: " + TextUtils.join(" ",cmd));
		// Warning: /data/local/tmp is not accessible by default, must be manually changed to 755 (including parent directories)
		//String cmd[] = {libdir+"/libstrace.so","-f","-v","-s","1000","-o","/data/local/tmp/parser.strace.log",parser_binary};
		String env[] = {"LD_LIBRARY_PATH=" + libdir, "LD_PRELOAD=" + libdir + "/libcompat.so"};
		parser =  Runtime.getRuntime().exec(cmd, env, null);
		this.parserStdout = new BufferedReader(new InputStreamReader(parser.getInputStream()));
		this.parserStdin = new DataOutputStream(parser.getOutputStream());
		this.parserStderr = new BufferedReader(new InputStreamReader(parser.getErrorStream()));
		char[] handshakeBytes = new char[10];
		try{
			parserStdout.read(handshakeBytes);
		} catch(IOException e){
			this.parser = null;
			this.parserStdout = null;
			this.parserStdin = null;
			this.parserStderr = null;
			throw new IOException("handshake from parser not successful:" + e.getMessage());
		}
		String handshake = new String(handshakeBytes).trim();
		if (handshake.equals("PARSER_OK")) {
			info("Parser handshake OK");
		} else{
			this.parser = null;
			this.parserStdout = null;
			this.parserStdin = null;
			this.parserStderr = null;
			throw new IOException("handshake from parser not successful");
		}
		try {
			int ret = parser.exitValue();
			this.parser = null;
			this.parserStdout = null;
			this.parserStdin = null;
			this.parserStderr = null;
			throw new IOException("parser exited prematurely (" + ret + ")");
		} catch (IllegalThreadStateException e) {
			// parser still running, success.
		}
		this.parserErrorThread = new ParserErrorThread();
		this.parserErrorThread.start();
		this.fromParserThread = new FromParserThread();
		this.fromParserThread.start();
		this.toParserThread = new ToParserThread();
		this.toParserThread.start();
	}
	/**
	 * Gets the next row id for a given database table.
	 * @param tableName
	 * @return
	 */
	private long getNextRowId(String tableName) {
		try{
			MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(MsdService.this));
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			Cursor c = db.rawQuery("SELECT MAX(_ROWID_) FROM " + tableName, null);
			if(!c.moveToFirst()){
				handleFatalError("getNextRowId(" + tableName + ") failed because c.moveToFirst() returned false, this shouldn't happen");
				return 0;
			}
			long result = c.getLong(0) + 1;
			c.close();
			return result;
		} catch(SQLException e){
			handleFatalError("SQLException in getNextRowId(" + tableName + "): ",e);
			return 0;
		}
	}
	private void launchHelper() throws IOException {
		String libdir = this.getApplicationInfo().nativeLibraryDir;
		String diag_helper = libdir + "/libdiag-helper.so";
		String suBinary = DeviceCompatibilityChecker.getSuBinary();
		if(suBinary == null){
			throw new IllegalStateException("No working su binary found, can't start recording");
		}
		String cmd[] = { suBinary, "-c", "exec " + diag_helper + " run"};

		info("Launching helper: " + TextUtils.join(" ",cmd));
		helper = Runtime.getRuntime().exec(cmd);

		this.diagStdout = new DataInputStream(helper.getInputStream());
		this.diagStdin = new DataOutputStream(helper.getOutputStream());
		this.diagStderr = new BufferedReader(new InputStreamReader(helper.getErrorStream()));
		byte[] handshakeBytes = new byte[4];
		MsdService.this.diagStdout.read(handshakeBytes);
		String handshake = new String(handshakeBytes, "ASCII");
		if (handshake.equals("OKAY")) {
			info("handshake from helper successful");
		} else{
			throw new IOException("handshake from helper not successful");
		}
		try {
			int ret = helper.exitValue();
			throw new IOException("helper exited prematurely (" + ret + ")");
		} catch (IllegalThreadStateException e) {
			// helper still running, success.
		}
		this.diagErrorThread = new DiagErrorThread();
		this.diagErrorThread.start();
		this.fromDiagThread = new FromDiagThread();
		this.fromDiagThread.start();
		this.toDiagThread = new ToDiagThread();
		this.toDiagThread.start();
		for(byte[] singleCmd:SetupLoggingCmds.cmds){
			this.toDiagMsgQueue.add(new QueueElementWrapper<byte[]>(singleCmd));
		}
	}

	private static final int USER_SPACE_LOG_TYPE = 32;

	private static List<byte[]> fromDev(byte[] data, int offset, int length) throws IllegalStateException {
		ByteBuffer in = ByteBuffer.wrap(data, 0, length).order(ByteOrder.nativeOrder());
		List<byte[]> msgs = new ArrayList<byte[]>();
		int type = in.getInt();
		if (type == USER_SPACE_LOG_TYPE) {
			int nelem = in.getInt();
			for (int i = 0; i < nelem; ++i) {
				int len = in.getInt();
				if(len > 1024*1024)
					throw new IllegalStateException("TLV Length field from diag is bigger than 1MB");
				byte[] bytes = new byte[len];
				in.get(bytes);
				msgs.add(bytes);
			}
		}

		return msgs;
	}

	private static byte[] toDev(byte[] bytes) {
		ByteBuffer result = ByteBuffer.allocate(bytes.length + 4).order(ByteOrder.nativeOrder());
		result.putInt(USER_SPACE_LOG_TYPE);
		result.put(bytes);
		return result.array();
	}
	private void sendFatalErrorMessage(String msg, Throwable e){
		if(e == null){
			MsdLog.e(TAG,"sendFatalErrorMessage: " + msg);
		} else{
			MsdLog.e(TAG,"sendFatalErrorMessage: " + msg,e);
			msg += " " + e.getClass().getSimpleName() + ": " + e.getMessage();
		}
		closeDebugLog(true);
		msdServiceNotifications.showInternalErrorNotification(msg, debugLogFileId);
	}
	private void sendStateChanged(StateChangedReason reason){
		Vector<IMsdServiceCallback> callbacksToRemove = new Vector<IMsdServiceCallback>();
		for(IMsdServiceCallback callback:mBinder.callbacks){
			try {
				callback.stateChanged(reason.name());
			} catch (DeadObjectException e) {
				info("DeadObjectException in MsdService.sendStateChanged() => unregistering callback");
				callbacksToRemove.add(callback);
			} catch (RemoteException e) {
				warn("Exception in MsdService.sendStateChanged() => callback.recordingStateChanged();");
			}
		}
		mBinder.callbacks.removeAll(callbacksToRemove);
	}
	void handleFatalError(String msg, final Throwable e){
		boolean doShutdown = false;
		if(recording && shuttingDown.compareAndSet(false, true)){
			msg += " => shutting down service";
			if(e == null){
				// Create a dummy exception so that we see the stack trace of a fatal error in the logs.
				try{
					((String)null).toString();
				} catch(Exception e1){
					MsdLog.e(TAG, "Dummy Exception to get stack trace of fatal error:",e1);
				}
			}
			doShutdown = true;
		} else if(recording){
			shutdownError = true;
			msg = "Error while shutting down: " + msg;
		} else{
			msg = "Error while not recording: " + msg;
		}
		MsdLog.e(TAG,"handleFatalError: " + msg,e);
		final String finalMsg = msg;
		if(doShutdown){
			// Call shutdown in the main thread so that the thread causing the Error can terminate
			fatalErrorOccured = true;
			mainThreadHandler.post(new ExceptionHandlingRunnable(new Runnable(){
				@Override
				public void run() {
					Log.e(TAG,"shutdownDueToError(finalMsg,e);");
					shutdownDueToError(finalMsg,e);
				};
			}));
		} else{
			// Only send the first fatal error to the UI
			if(!fatalErrorOccured)
				sendFatalErrorMessage(msg, e);
			fatalErrorOccured = true;
		}
	}
	void handleFatalError(String msg){
		handleFatalError(msg,null);
	}
	private static void info(String msg){
		MsdLog.i(TAG,msg);
	}
	private static void warn(String msg){
		MsdLog.w(TAG,msg);
	}
	private void shutdownDueToError(String msg, Throwable e){
		shutdown(true);
		MsdLog.e(TAG, "Sending internalError() to all callbacks");
		Vector<IMsdServiceCallback> callbacksToRemove = new Vector<IMsdServiceCallback>();
		for(IMsdServiceCallback callback:mBinder.callbacks){
			try {
				callback.internalError();
			} catch (DeadObjectException e1) {
				info("DeadObjectException in MsdService.sendStateChanged() => unregistering callback");
				callbacksToRemove.add(callback);
			} catch (RemoteException e1) {
				warn("Exception in MsdService.sendStateChanged() => callback.recordingStateChanged();");
			}
		}
		mBinder.callbacks.removeAll(callbacksToRemove);
		sendFatalErrorMessage(msg, e);
		MsdLog.e(TAG, "Terminating MsdService after shutting down due to an unexpected error");
		System.exit(1);
	}
	/**
	 * Checks whether all threads are still running and no message queue contains a huge number of messages
	 */
	private synchronized void checkRecordingState(){
		if(shuttingDown.get())
			return;
		boolean ok = true;
		int sqlQueueSize;
		// Check all threads
		if(!sqliteThread.isAlive()){
			handleFatalError("testRecordingState(): sqliteThread has died");
			ok = false;
		}
		if(!diagErrorThread.isAlive()){
			handleFatalError("testRecordingState(): diagErrorThread has died");
			ok = false;
		}
		if(!fromDiagThread.isAlive()){
			handleFatalError("testRecordingState(): fromDiagThread has died");
			ok = false;
		}
		if(!toDiagThread.isAlive()){
			handleFatalError("testRecordingState(): toDiagThread has died");
			ok = false;
		}
		if(!parserErrorThread.isAlive()){
			handleFatalError("testRecordingState(): parserErrorThread has died");
			ok = false;
		}
		if(!fromParserThread.isAlive()){
			handleFatalError("testRecordingState(): fromParserThread has died");
			ok = false;
		}
		if(!toParserThread.isAlive()){
			handleFatalError("testRecordingState(): toParserThread has died");
			ok = false;
		}
		// Check message queues
		if(toDiagMsgQueue.size() > 100){
			handleFatalError("testRecordingState(): toDiagMsgQueue contains too many entries");		
			ok = false;	
		}
		if(toParserMsgQueue.size() > 100){
			handleFatalError("testRecordingState(): diagMsgQueue contains too many entries");
			ok = false;
		}		
		if(rawWriter.getQueueSize() > 100){
			handleFatalError("testRecordingState(): rawWriter contains too many queue entries");
			ok = false;
		}

		// Do not make SQL queue overflow fatal for now
		sqlQueueSize = pendingSqlStatements.size();
		if (sqlQueueSize > sqlQueueWatermark){
			sqlQueueWatermark = sqlQueueSize;
			warn("DEBUG: SQL queue high mark: " + sqlQueueWatermark);
		}

		if(pendingSqlStatements.isEmpty())
			pendingSqlStatementsEmptyTimestamp = System.currentTimeMillis();

		if( System.currentTimeMillis() > pendingSqlStatementsEmptyTimestamp + 60*1000){
			// TODO: The issue of the stuck analysis is not fixed and we will not have
			// time to do this until version 0.9. Do not throw an exception for now.
			warn("SQL Statements are waiting for more than one minute, current queue size: " + pendingSqlStatements.size());
		}
		// Terminate extra file recording if the ActiveTestService doesn't terminate it (e.g. because it disappears)		
		if(extraRecordingRawFileWriter != null){
			if(System.currentTimeMillis() > extraRecordingStartTime + 10*60*1000)
				endExtraRecording(false);
		}

		// Check parser memory usage by evaluating the stack/heap size in /proc/pid/maps

		// The (abstract) java.lang.Process class does not contain a method for
		// accessing the pid of the process (because java is a cross-platform
		// language and programmers are not supposed to access platform specific
		// data like the process id). However, the actual class of the
		// process does contain a pid field (declared as private), which can be
		// accessed via reflection.
		if(parser.getClass().getName().equals("java.lang.ProcessManager$ProcessImpl")) {
			try {
				Field f = parser.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				int pid = f.getInt(parser);
				// Some examples of the maps file entries for stack/heap
				// 019b4000-019b7000 rw-p 00000000 00:00 0          [heap]
				// be9d9000-be9fa000 rw-p 00000000 00:00 0          [stack]
				BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/" + pid + "/maps")));
				long heapSize = 0;
				long stackSize = 0;
				for(String line=r.readLine();line != null; line = r.readLine()){
					// Using BigInteger is required here because java doesn't
					// support unsigned integer/long types. A signed long would
					// be enough for a 32 bit address (and therefore current
					// Android systems) but 64 bit Android systems have already
					// been announced.
					int posMinus = line.indexOf("-");
					int posSpace = line.indexOf(" ");
					BigInteger startAddr = new BigInteger(line.substring(0,posMinus), 16);
					BigInteger endAddr = new BigInteger(line.substring(posMinus + 1,posSpace), 16);;
					long mappingSize = endAddr.subtract(startAddr).longValue();
					if(line.contains("[heap]")){
						heapSize += mappingSize;
					}
					if(line.contains("[stack]")){
						stackSize += mappingSize;
					}
				}
				r.close();

				// Log heap size only if something changed
				if (heapSize != oldHeapSize)
				{
					info("Parser heap size: " + (heapSize/1024) + " KiB");
					oldHeapSize = heapSize;
				}

				// Log stack size only if something changed
				if (stackSize != oldStackSize)
				{
					info("Parser stack size: " + (stackSize/1024) + " KiB");
					oldStackSize = stackSize;
				}

				if(heapSize > 128*1024*1024){ // Maximum allowed heap size: 128 MiB, change to 30 KiB for testing the restarting, it will be exceeded during the first call
					warn("Restarting recording due to excessive parser heap size (" + (heapSize/1024) + " KiB)" );
					restartRecording();
				} else if(stackSize > 16*1024*1024){ // Maximum allowed stack size: 16 MiB, this should definitely be enough
					warn("Restarting recording to excessive parser stack size (" + (stackSize/1024) + " KiB)" );
					restartRecording();
				}
			} catch (Exception e) {
				handleFatalError("Failed to get parser memory consumption",e);
				ok = false;
			}
		} else{
			handleFatalError("Failed to get parser pid, parser class name is " + parser.getClass().getName() + " instead of java.lang.ProcessManager$ProcessImpl");
			ok = false;
		}
		// TODO: Maybe check memory usage of this Service process as well.
		if(ok){
			openOrReopenRawWriter();
			openOrReopenDebugLog(false, false);
			cleanup();
		}
	}
	private void openOrReopenRawWriter() {
		// Create a new dump file every 10 minutes, name it with the
		// UTC time so that it does not reuse the same filename if
		// the user	switches between different timezones.
		long timestamp = System.currentTimeMillis();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(timestamp);
		// Calendar.MONTH starts counting with 0
		String baseFilename = String.format(Locale.US, "qdmon_%04d-%02d-%02d_%02d-%02dUTC",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY), 10*(c.get(Calendar.MINUTE) / 10));
		if(rawWriter != null && currentRawWriterBaseFilename != null && currentRawWriterBaseFilename.equals(baseFilename))
			return; // No reopen needed
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		// Save the old writer
		currentRawWriterBaseFilename = baseFilename;
		EncryptedFileWriter oldRawWrite = rawWriter;
		long oldRawLogFileId = rawLogFileId;
		// Get the filename for the new file
		// When restarting recording within a 10 minutes interval, the file may
		// already exist. Since smime files can't simply be appended (like gzip
		// files), we have to create a new filename e.g. by appending a number
		String encryptedFilename = null;
		String plaintextFilename = null;
		for(int i=0;i<300;i++){
			plaintextFilename = baseFilename + (i>0 ? "." + i : "") +  ".gz";
			encryptedFilename = plaintextFilename + ".smime";
			if((new File(getFilesDir().toString() + "/" + plaintextFilename)).exists() || (new File(getFilesDir().toString() + "/" + encryptedFilename)).exists() ){
				plaintextFilename = null;
				encryptedFilename = null;
			} else{
				break;
			}
		}
		if(encryptedFilename == null){
			handleFatalError("Couldn't find a non-existing filename for raw qdmon dump, baseFilename=" + baseFilename);
			return;
		}
		rawWriter = new EncryptedFileWriter(this, encryptedFilename, true, plaintextFilename, true);
		// Register the file in database
		// Set calendar to start of 10 minute interval
		c.setTimeInMillis(timestamp);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND,0);
		c.set(Calendar.MINUTE,10*(c.get(Calendar.MINUTE)/10));
		DumpFile df = new DumpFile(encryptedFilename,DumpFile.TYPE_ENCRYPTED_QDMON,timestamp,c.getTimeInMillis() + 10*60*1000);
		df.insert(db);
		rawLogFileId = df.getId();
		if(oldRawWrite != null){
			// There is an open debug logfile already, so let's close it and set the end time in the database
			oldRawWrite.close();
			df = DumpFile.get(db, oldRawLogFileId);
			df.endRecording(db);
		}
		MsdDatabaseManager.getInstance().closeDatabase();
		triggerUploading();
	}
	private void closeRawWriter(){
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		rawWriter.close();
		rawWriter = null;
		currentRawWriterBaseFilename = null;
		DumpFile df = DumpFile.get(db, rawLogFileId);
		df.endRecording(db);
		rawLogFileId = 0;
		MsdDatabaseManager.getInstance().closeDatabase();
	}

	/**
	 * Closes the debug logfile. This method should be called when the service
	 * is terminated by the Android system or when it is terminated due to a
	 * fatal error.
	 * 
	 * Please note that messages sent after closeDebugOutput will be written to
	 * Android Logcat only and not to a new debug logfile.
	 * 
	 * @param crash
	 */
	private void closeDebugLog(boolean crash){
		if(debugLogWriter == null)
			return;
		info("MsdService.closeDebugLog(" + crash + ") called, closing log " + debugLogWriter.getEncryptedFilename());
		EncryptedFileWriter tmp = debugLogWriter; // Make sure there are no writes to debugLogWriter after the close()
		debugLogWriter = null;
		tmp.close();
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		DumpFile df = DumpFile.get(db, debugLogFileId);
		df.endRecording(db);
		if(crash){
			df.updateCrash(db, true);
		}
		MsdDatabaseManager.getInstance().closeDatabase();
	}

	/**
	 * Opens or reopens the debug log so that it only contains e.g. 1 hour of
	 * output (which should be enough to debug a crash).
	 */
	private long openOrReopenDebugLog(boolean forceReopen, boolean markForUpload){
		if(!forceReopen && debugLogFileStartTime + 3600 * 1000 > System.currentTimeMillis())
			return 0; // No reopen needed
		debugLogFileStartTime = System.currentTimeMillis();
		SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
		// Save the existing writer so that it can be closed after the new one has been opened (so we can't loose any messages).
		EncryptedFileWriter oldDebugLogWriter = debugLogWriter;
		long oldDebugLogId = debugLogFileId;
		// Create a new dump file every 10 minutes, name it with the
		// UTC time so that it does not reuse the same filename if
		// the user	switches between different timezones.
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		// Calendar.MONTH starts counting with 0
		String timestampStr = String.format(Locale.US, "%04d-%02d-%02d_%02d-%02d-%02dUTC",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
		String plaintextFilename = null;
		String encryptedFilename = null;
		for(int i=0;i<10;i++){
			plaintextFilename = "debug_" + timestampStr + (i>0 ? "." + i : "") +  ".gz";
			encryptedFilename = plaintextFilename + ".smime";
			if((new File(getFilesDir().toString() + "/" + plaintextFilename)).exists() || (new File(getFilesDir().toString() + "/" + encryptedFilename)).exists() ){
				plaintextFilename = null;
				encryptedFilename = null;
			} else{
				break;
			}
		}
		if(encryptedFilename == null){
			handleFatalError("openOrReopenDebugLog(): Couldn't find an available filename for debug log");
			return 0;
		}
		// Uncomment the following line to disable plaintext logs (might be good for the final release):
		// plaintextFilename = null;
		EncryptedFileWriter newDebugLogWriter = new EncryptedFileWriter(this, encryptedFilename, true, plaintextFilename, true);
		newDebugLogWriter.write(MsdLog.getLogStartInfo(this));
		if(logBuffer != null){
			newDebugLogWriter.write("LOGBUFFER: " + logBuffer.toString() + ":LOGBUFFER_END");
			logBuffer = null;
		}
		// Everything logged from other threads until now still gets to the old file.
		debugLogWriter = newDebugLogWriter;
		DumpFile df = new DumpFile(encryptedFilename,DumpFile.TYPE_DEBUG_LOG);
		// TODO: The following line makes sure that ALL debug logs are uploaded. This should be removed for the final release.
		df.setState(DumpFile.STATE_RECORDING_PENDING);
		df.insert(db);
		debugLogFileId = df.getId();
		if(oldDebugLogWriter != null){
			// There is an open debug logfile already, so let's close it and set the end time in the database
			oldDebugLogWriter.close();
			df = DumpFile.get(db, oldDebugLogId);
			if(markForUpload)
				df.markForUpload(db);
			df.endRecording(db);
		}
		MsdDatabaseManager.getInstance().closeDatabase();
		triggerUploading(); // TODO: Remove this for final version.
		return oldDebugLogId;
	}
	private void restartRecording(){
		if(!shutdown(false)){
			return;
		}
		startRecording();
	}

	/**
	 * Deletes all files with STATE_RECORDINIG in the database, should be called
	 * in onCreate(). This will delete incomplete old files which are
	 * created when MsdService crashes.
	 */
	private void cleanupIncompleteOldFiles(){
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
		try{
			wl.acquire();
			info("Starting cleanupIncompleteOldFiles");
			long cleanupStartTime = System.currentTimeMillis();
			long cleanupStartCpuTimeNanos = android.os.Debug.threadCpuTimeNanos();
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			Vector<DumpFile> files = DumpFile.getFiles(db, " state = " + DumpFile.STATE_RECORDING + " OR state = " + DumpFile.STATE_RECORDING_PENDING);
			for(DumpFile df:files){
				info("Deleting old file " + df.getFilename());
				deleteFile(df.getFilename());
				df.delete(db);
			}
			info("cleanupIncompleteOldFiles took " + (System.currentTimeMillis() - cleanupStartTime) + "ms, CPU time: " + ((android.os.Debug.threadCpuTimeNanos()-cleanupStartCpuTimeNanos)/1000000) + "ms");
		} catch(Exception e){
			handleFatalError("Exception during cleanup",e);
		} finally{
			MsdDatabaseManager.getInstance().closeDatabase();
			wl.release();
		}
	}
	private void cleanup(){
		if(System.currentTimeMillis() > lastCleanupTime + 3600 * 1000){
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
			try{
				wl.acquire();
				info("Starting cleanup");
				long cleanupStartTime = System.currentTimeMillis();
				long cleanupStartCpuTimeNanos = android.os.Debug.threadCpuTimeNanos();
				SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
				info("Cleaning files");
				cleanupFiles(db);
				info("Cleaning files completed, cleaning database");
				cleanupDatabase(db);
				info("Cleanup took " + (System.currentTimeMillis() - cleanupStartTime) + "ms, CPU time: " + ((android.os.Debug.threadCpuTimeNanos()-cleanupStartCpuTimeNanos)/1000000) + "ms");
			} catch(Exception e){
				handleFatalError("Exception during cleanup",e);
			} finally{
				MsdDatabaseManager.getInstance().closeDatabase();
				wl.release();
			}
			lastCleanupTime = System.currentTimeMillis();
		}
	}
	private void cleanupFiles(SQLiteDatabase db){
		// Read all files to a HashMap and then iterate over a directory listing, saves doing an SQL query for each file
		Vector<DumpFile> dbFiles = DumpFile.getFiles(db, "");
		HashMap<String,DumpFile> filesByName = new HashMap<String,DumpFile>();
		for(DumpFile df:dbFiles){
			filesByName.put(df.getFilename(), df);
		}
		HashSet<String> existingFiles = new HashSet<String>();
		// Iterate over all files and delete if:
		// * There is no database entry for the file
		// * The file is older than the configured keep duration, doesn't contain an event and is not pending for upload
		for(String filename:fileList()){
			// Plaintext files are not deleted automatically.
			boolean debug = filename.startsWith("debug") && filename.endsWith(".smime");
			boolean qdmon = filename.startsWith("qdmon") && filename.endsWith(".smime");
			if(debug || qdmon){
				DumpFile df = filesByName.get(filename);
				boolean containsEvent = false;
				int keepDurationHours = 0;
				if(debug){
					keepDurationHours = MsdConfig.getDebugLogKeepDurationHours(this);
					if(df != null && df.isCrash())
						containsEvent = true;
				} else if(qdmon){
					keepDurationHours = MsdConfig.getBasebandLogKeepDurationHours(this);
					if(df != null && (df.isSms() || df.isImsi_catcher()))
						containsEvent = true;
				}
				boolean delete = false;
				if(df == null)
					delete = true; // No database entry => Delete file
				long fileAgeMillis = System.currentTimeMillis() - (df.getEnd_time() > df.getStart_time() ? df.getEnd_time() : df.getStart_time());
				if(df != null && !containsEvent && keepDurationHours > 0 && fileAgeMillis > keepDurationHours * 3600 * 1000)
					delete = true; // File doesn't contain an event (crash, SMS, IMSI) and it is older than the configured keep duration => Delete it
				if(df != null && df.getState() == DumpFile.STATE_PENDING)
					delete = false; // Don't delete files pending for upload
				if(df != null && (df.getState() == DumpFile.STATE_RECORDING || df.getState() == DumpFile.STATE_RECORDING_PENDING))
					delete = false; // Don't delete files currently recording, artifacts will be cleaned up in cleanupIncompleteOldFiles()
				if(delete){
					deleteFile(filename);
					info("Deleted file " + filename + ((df != null) ? " and corresponding database entry" : ""));
					if(df != null){
						df.delete(db);
						filesByName.remove(filename);
					}
				} else{
					existingFiles.add(filename);
				}
			}
		}
		// Iterate over all database entries and delete it if the corresponding
		// file doesn't exist any more and the state is not UPLOADED.
		for(Entry<String,DumpFile> entry:filesByName.entrySet()){
			DumpFile df = entry.getValue();
			if(existingFiles.contains(entry.getKey()))
				continue; // Don't delete entries as long as the file hasn't been deleted
			if(df.getState() == DumpFile.STATE_UPLOADED)
				continue; // Keep track of uploaded files so that we can still show that an event has been uploaded
			// File doesn't exist and state is not UPLOADED => The database entry can be removed
			info("Deleting database entry for file " + df.getFilename());
			df.delete(db);
		}
	}
	private void cleanupDatabase(SQLiteDatabase db) throws SQLException{
		String sql;
		int keepDuration = MsdConfig.getAnalysisInfoKeepDurationHours(MsdService.this);
		if(keepDuration > 0){
			sql = "DELETE FROM session_info WHERE timestamp < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM sms_meta WHERE id < (SELECT min(id) FROM session_info);";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM serving_cell_info WHERE timestamp < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM neighboring_cell_info WHERE timestamp < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM cell_info WHERE last_seen < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM arfcn_list WHERE id < (select min(id) from cell_info);";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM events WHERE timestamp < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);

			sql = "DELETE FROM paging_info WHERE timestamp < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
		}
		keepDuration = MsdConfig.getLocationLogKeepDurationHours(MsdService.this);
		if(keepDuration > 0){
			sql = "DELETE FROM location_info where timestamp < datetime('now','-" + keepDuration + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
		}
	}
}
