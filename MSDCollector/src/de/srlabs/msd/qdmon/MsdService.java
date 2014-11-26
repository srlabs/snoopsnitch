package de.srlabs.msd.qdmon;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import de.srlabs.msd.util.Constants;
import de.srlabs.msd.util.DeviceCompatibilityChecker;
import de.srlabs.msd.util.MsdConfig;
import de.srlabs.msd.util.MsdDatabaseManager;

public class MsdService extends Service{
	public static final String    TAG                   = "msd-service";

	// TODO: Watch storage utilisation and stop recording if limits are exceeded
	// TODO: Watch battery level and stop recording if battery level goes below configured limit

	private final MyMsdServiceStub mBinder = new MyMsdServiceStub();
	class MyMsdServiceStub extends IMsdService.Stub {

		private Vector<IMsdServiceCallback> callbacks = new Vector<IMsdServiceCallback>();

		@Override
		public boolean isRecording() throws RemoteException {
			return recording;
		}

		@Override
		public boolean startRecording() throws RemoteException {
			return MsdService.this.startRecording();
		}

		@Override
		public boolean stopRecording() throws RemoteException {
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
	};
	AtomicBoolean shuttingDown = new AtomicBoolean(false);

	Process helper;
	DataInputStream diagStdout;
	DataOutputStream diagStdin;
	private BufferedReader diagStderr;
	FromDiagThread fromDiagThread;

	private BlockingQueue<DiagMsgWrapper> toParserMsgQueue = new LinkedBlockingQueue<DiagMsgWrapper>();
	private BlockingQueue<DiagMsgWrapper> rawFileWriterMsgQueue = new LinkedBlockingQueue<DiagMsgWrapper>();
	private BlockingQueue<QueueElementWrapper<byte[]>> toDiagMsgQueue = new LinkedBlockingQueue<MsdService.QueueElementWrapper<byte[]>>();
	private BlockingQueue<PendingSqliteStatement> pendingSqlStatements = new LinkedBlockingQueue<PendingSqliteStatement>();

	private BufferedReader parserStdout;
	private DataOutputStream parserStdin;
	private BufferedReader parserStderr;
	private Process parser;
	private SqliteThread sqliteThread = null;
	private ParserErrorThread parserErrorThread;
	private FromParserThread fromParserThread;
	private ToParserThread toParserThread;
	private RawFileWriterThread rawFileWriterThread;
	private ToDiagThread toDiagThread;
	private DiagErrorThread diagErrorThread;
	private volatile boolean shutdownError = false;
	private AtomicBoolean readyForStartRecording = new AtomicBoolean(true);
	private Handler msdServiceMainThreadHandler = new Handler();
	private PeriodicCheckRecordingStateRunnable periodicCheckRecordingStateRunnable = new PeriodicCheckRecordingStateRunnable();
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
		Log.i(TAG,"MsdService.onBind() called");
		return mBinder;
	}

	class PeriodicCheckRecordingStateRunnable implements Runnable{
		@Override
		public void run() {
			checkRecordingState();
			msdServiceMainThreadHandler.postDelayed(this, 1000);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(
				new Thread.UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						handleFatalError("Uncought Exception in MsdService Thread " + t.getClass(), e);
					}
				});
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		info("MsdService.onCreate() called");
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
			cleanupIncompleteOldFiles();
			this.shuttingDown.set(false);
			this.sqliteThread = new SqliteThread();
			sqliteThread.start();
			launchParser();
			launchRawFileWriter();
			launchHelper();
			mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					startLocationRecording();					
				}
			});
			startPhoneStateRecording();
			this.recording  = true;
			msdServiceMainThreadHandler.removeCallbacks(periodicCheckRecordingStateRunnable);
			msdServiceMainThreadHandler.post(periodicCheckRecordingStateRunnable);
			doStartForeground();
			sendStateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
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
			msdServiceMainThreadHandler.removeCallbacks(periodicCheckRecordingStateRunnable);
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
					this.diagStdin.close();
				} catch (IOException e) {
					handleFatalError("IOException while closing diagStdin" , e);
				}
			}
			if(helper != null){
				Thread t = new Thread(){
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
			// Terminate raw file write
			if(rawFileWriterThread != null){
				rawFileWriterMsgQueue.add(new DiagMsgWrapper()); // Send shutdown marker to message queue
				rawFileWriterThread.join(3000);
				if(rawFileWriterThread.isAlive()){
					handleFatalError("Failed to stop rawFileWriter");
				}
				rawFileWriterThread = null;
			}
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
			return !shutdownError;
		} catch (Exception e) {
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
		public void run() {
			try {
				while (true) {
					int data_len = MsdService.this.diagStdout.readInt();
					byte[] individual_buf = new byte[data_len];
					MsdService.this.diagStdout.readFully(individual_buf, 0, data_len);
					for(byte[] buf:fromDev(individual_buf, 0, individual_buf.length)){
						DiagMsgWrapper msg = new DiagMsgWrapper(buf);
						toParserMsgQueue.add(msg);
						rawFileWriterMsgQueue.add(msg);
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

	/**
	 * The Thread saves raw diag messages to files, which can be uploaded later.
	 * Depending on the configuration, the messages are written to an encrypted
	 * and/or unencrypted file.
	 * 
	 */
	class RawFileWriterThread extends Thread{
		private Process openssl;
		GZIPOutputStream unencryptedOutputStream = null;
		OutputStream encryptedOutputStream = null;
		private BufferedReader opensslStderr;
		private boolean closeOutputRunning = false;
		private OpensslErrorThread opensslErrorThread;

		public void run() {
			String currentDumpfileName = null;
			try {
				while(true){
					DiagMsgWrapper msg = rawFileWriterMsgQueue.take();
					if(msg.shutdownMarker){
						info("RawFileWriterThread shutting down due to shutdown marker, OK");
						closeOutput();
						return;
					}
					// Create a new dump file every 10 minutes, name it with the
					// UTC time so that it does not reuse the same filename if
					// the user	switches between different timezones.
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
					// Calendar.MONTH starts counting with 0
					String filename = String.format(Locale.US, "qdmon_%04d-%02d-%02d_%02d-%02dUTC",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY), 10*(c.get(Calendar.MINUTE) / 10));
					if(currentDumpfileName == null || !filename.equals(currentDumpfileName)){
						closeOutput();
						if(MsdConfig.recordUnencryptedDumpfiles(MsdService.this)){
							info("Opening new raw file " + filename + ".gz");
							// Use MODE_APPEND so that it appends to the existing
							// file if the file already exists (e.g. because the
							// user stopped recording or restarted the app in
							// between). Gzip does allow concatenating files so it
							// is acceptable to just append to an existing gzip
							// file.
							unencryptedOutputStream = new GZIPOutputStream(openFileOutput(filename + ".gz", Context.MODE_APPEND));
						}
						if(MsdConfig.recordEncryptedDumpfiles(MsdService.this)){
							String libdir = getApplicationInfo().nativeLibraryDir;
							String openssl_binary = libdir + "/libopenssl.so";
							boolean ok = true;
							if(!((new File(openssl_binary)).exists())){
								handleFatalError("Not recording encrypted dumpfiles since the openssl binary " + openssl_binary + " does not exist");
								ok = false;
							}
							String crtFile = libdir + "/libsmime_crt.so";
							if(!((new File(crtFile)).exists())){
								handleFatalError("Not recording encrypted dumpfiles since the certificate file " + crtFile + " does not exist");
								ok = false;
							}
							if(ok){
								String encryptedOutputFileName = getFilesDir().toString() + "/" + filename + ".smime";
								if((new File(encryptedOutputFileName)).exists()){
									// When restarting recording within a 10 minutes
									// interval, the file may already exist. Since
									// smime files can't simply be appended (like
									// gzip files), we have to create a new
									// filename e.g. by appending a number
									int i;
									for(i=1;i<30 && (new File(encryptedOutputFileName + "." + i)).exists();i++);
									encryptedOutputFileName += "." + i;
									if((new File(encryptedOutputFileName)).exists()){
										handleFatalError("Failed to find a new filename for encrypted output file " + encryptedOutputFileName);
										unencryptedOutputStream.close();
										return;
									}
								}
								info("Writing encrypted output to " + encryptedOutputFileName);
								String cmd[] = {openssl_binary, "smime", "-encrypt", "-binary", "-aes256", "-outform", "DER", "-out", encryptedOutputFileName, crtFile};
								String env[] = {"LD_LIBRARY_PATH=" + libdir, "OPENSSL_CONF=/dev/null", "RANDFILE=/dev/null"};
								info("Launching openssl: " + TextUtils.join(" ",cmd));
								openssl =  Runtime.getRuntime().exec(cmd, env, null);
								encryptedOutputStream = openssl.getOutputStream();
								opensslStderr = new BufferedReader(new InputStreamReader(openssl.getErrorStream()));
								opensslErrorThread = new OpensslErrorThread();
								opensslErrorThread.start();
							}
						}
						currentDumpfileName = filename;
					}
					if(MsdConfig.recordUnencryptedDumpfiles(MsdService.this)){
						// Flush after each write, this may decrease the compression rate
						// Maybe we have to write stuff to a temporary file ang gzip it as a whole file when opening a new file.
						unencryptedOutputStream.write(msg.buf);
						unencryptedOutputStream.flush();
					}
					if(MsdConfig.recordEncryptedDumpfiles(MsdService.this) && encryptedOutputStream != null){
						encryptedOutputStream.write(msg.buf);
						encryptedOutputStream.flush();
					}
				}
			} catch (InterruptedException e) {
				closeOutput();
				handleFatalError("RawFileWriterThread shutting down due to InterruptedException", e);
			} catch (IOException e) {
				closeOutput();
				if(MsdService.this.shuttingDown.get())
					info("RawFileWriterThread: IOException while shutting down: " + e.getMessage());
				else
					handleFatalError("RawFileWriterThread: IOException: " + e.getMessage());
			}
		}
		private void closeOutput(){
			closeOutputRunning  = true;
			try{
				if(unencryptedOutputStream != null){
					unencryptedOutputStream.close();
					unencryptedOutputStream = null;
				}
				if(encryptedOutputStream != null){
					encryptedOutputStream.close();
					encryptedOutputStream = null;
					Thread t = new Thread(){
						public void run() {
							try{
								openssl.waitFor();
							} catch(InterruptedException e){
							}
						};
					};
					t.start();
					t.join(3000);
					t.interrupt();
					try{
						int exitValue = openssl.exitValue();
						info("Openssl terminated with exit value " + exitValue);
						if(exitValue != 0){
							handleFatalError("Openssl terminated with an error, exit value: " + exitValue);
						}
					} catch(IllegalThreadStateException e){
						handleFatalError("Failed to stop diag helper, calling destroy(): " + e.getMessage());		
						helper.destroy();
					}
				}
			} catch (InterruptedException e) {
				handleFatalError("RawFileWriterThread.closeOutput() failed with InterruptedException", e);
			} catch (IOException e) {
				handleFatalError("RawFileWriterThread.closeOutput() failed with IOException", e);
			}
			closeOutputRunning = false;
		}
		class OpensslErrorThread extends Thread{
			@Override
			public void run() {
				try {
					while(true){
						String line = opensslStderr.readLine();
						if(line == null){
							if(closeOutputRunning){
								info("opensslStderr.readLine() returned null while closeOutputRunning is set, OK");
							} else{
								handleFatalError("opensslStderr.readLine() returned null");
							}
							return;
						}
						handleFatalError("Openssl Error: " + line);
					}
				} catch(EOFException e){
					if(shuttingDown.get()){
						info("ParserErrorThread received IOException while shutting down, OK");
					} else{
						handleFatalError("EOFException while reading from opensslStderr: " + e.getMessage());
					}
				} catch(IOException e){
					handleFatalError("IOException while reading from opensslStderr: " + e.getMessage());
				}
			}
		}
	}
	class FromParserThread extends Thread{
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
		public void run() {
			lastAnalysisTime = System.currentTimeMillis();
			MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(MsdService.this));
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			while(true){
				try {
					if(shuttingDown && pendingSqlStatements.isEmpty()){
						info("SqliteThread shutting down due to shuttingDown && pendingSqlStatements.isEmpty()");
						return;
					}
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
						try{

							Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
							String time = String.format(Locale.US, "%02d:%02d:%02d",
									start.get(Calendar.HOUR_OF_DAY),
									start.get(Calendar.MINUTE),
									start.get(Calendar.SECOND));

							if (MsdServiceAnalysis.runCatcherAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.IMSI_DETECTED);
							};
							if (MsdServiceAnalysis.runSMSAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.SMS_DETECTED);
							};
							if (MsdServiceAnalysis.run2GAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.SEC_2G_CHANGED);
							};
							if (MsdServiceAnalysis.run3GAnalysis(MsdService.this, db)) {
								sendStateChanged(StateChangedReason.SEC_3G_CHANGED);
							};
							lastAnalysisTime = System.currentTimeMillis();

							Log.i(TAG,time + ": Analysis took " + (lastAnalysisTime - start.getTimeInMillis()) + "ms");

						} catch(Exception e){
							// Terminate the service with a fatal error if there is a any uncaught Exception in the Analysis
							handleFatalError("Exception during analysis",e);
						}
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
		String env[] = {"LD_LIBRARY_PATH=" + libdir};
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
			throw new IOException("handshake from helper not successful");
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
			return c.getLong(0) + 1;
		} catch(SQLException e){
			handleFatalError("SQLException in getNextRowId(" + tableName + "): ",e);
			return 0;
		}
	}
	private void launchRawFileWriter(){
		this.rawFileWriterThread = new RawFileWriterThread();
		this.rawFileWriterThread.start();
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
	private void sendErrorMessage(String msg, Throwable e){
		if(e == null){
			Log.e(TAG,"sendErrorMessage: " + msg);
		} else{
			Log.e(TAG,"sendErrorMessage: " + msg,e);
			msg += " " + e.getClass().getSimpleName() + ": " + e.getMessage();
		}
		// TODO: Reopen the debug log so that the user can upload an error log
		msdServiceNotifications.showInternalErrorNotification(msg, null);
	}
	private void sendStateChanged(StateChangedReason reason){
		Vector<IMsdServiceCallback> callbacksToRemove = new Vector<IMsdServiceCallback>();
		for(IMsdServiceCallback callback:mBinder.callbacks){
			try {
				callback.stateChanged(reason.name());
			} catch (DeadObjectException e) {
				Log.i(TAG,"DeadObjectException in MsdService.sendStateChanged() => unregistering callback");
				callbacksToRemove.add(callback);
			} catch (RemoteException e) {
				Log.e(TAG,"Exception in MsdService.sendStateChanged() => callback.recordingStateChanged();");
			}
		}
		mBinder.callbacks.removeAll(callbacksToRemove);
	}
	private void handleFatalError(String msg, final Throwable e){
		boolean doShutdown = false;
		if(recording && shuttingDown.compareAndSet(false, true)){
			msg += " => shutting down service";
			if(e == null){
				// Create a dummy exception so that we see the stack trace of a fatal error in the logs.
				try{
					((String)null).toString();
				} catch(Exception e1){
					Log.e(TAG, "Dummy Exception to get stack trace of fatal error:",e1);
				}
			}
			doShutdown = true;
		} else if(recording){
			shutdownError = true;
			msg = "Error while shutting down: " + msg;
		} else{
			msg = "Error while not recording: " + msg;
		}
		Log.e(TAG,"handleFatalError: " + msg,e);
		final String finalMsg = msg;
		if(doShutdown){
			// Call shutdown in the main thread so that the thread causing the Error can terminate
			fatalErrorOccured = true;
			msdServiceMainThreadHandler.post(new Runnable(){
				public void run() {
					shutdownDueToError(finalMsg,e);
				};
			});
		} else{
			// Only send the first fatal error to the UI
			if(!fatalErrorOccured)
				sendErrorMessage(msg, e);
			fatalErrorOccured = true;
		}
	}
	private void handleFatalError(String msg){
		handleFatalError(msg,null);
	}
	private static void info(String msg){
		Log.i(TAG,msg);
	}
	private static void warn(String msg){
		Log.w(TAG,msg);
	}
	private void shutdownDueToError(String msg, Throwable e){
		shutdown(true);
		sendErrorMessage(msg, e);
		Log.e(TAG, "Terminating MsdService after shutting down due to an unexpected error");
		System.exit(1);
	}
	/**
	 * Checks whether all threads are still running and no message queue contains a huge number of messages
	 */
	private void checkRecordingState(){
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
		if(rawFileWriterMsgQueue.size() > 100){
			handleFatalError("testRecordingState(): rawFileWriterMsgQueue contains too many entries");
			ok = false;
		}

		// Do not make SQL queue overflow fatal for now
		sqlQueueSize = pendingSqlStatements.size();
		if (sqlQueueSize > sqlQueueWatermark){
			sqlQueueWatermark = sqlQueueSize;
			warn("DEBUG: SQL queue high mark: " + sqlQueueWatermark);
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
			}
		} else{
			handleFatalError("Failed to get parser pid, parser class name is " + parser.getClass().getName() + " instead of java.lang.ProcessManager$ProcessImpl");
		}
		// TODO: Maybe check memory usage of this Service process as well.
	}
	private void restartRecording(){
		if(!shutdown(false)){
			return;
		}
		startRecording();
	}

	/**
	 * Deletes all files with STATE_RECORDINIG in the database, should be called
	 * before startRecording(). This will delete incomplete old files which are
	 * created when MsdService crashes.
	 */
	private void cleanupIncompleteOldFiles(){
		// TODO: Implement
	}
	private void cleanupRawFiles(){
		// TODO: Cleanup encrypted files after some time, excluding pending files
		// TODO: Cleanup debug logs
		// TODO: Cleanup old files still marked as STATE_RECORDING (this happens if MsdService crashes)
		// Cleanup unencrypted dumpfiles after MsdConfig.getBasebandLogKeepDurationHours()
		for(String filename:fileList()){
			if(!filename.startsWith("qdmon_"))
				continue;
			if(!filename.endsWith("UTC.gz"))
				continue;
			String dateStr = filename.substring("qdmon_".length());
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.set(Calendar.YEAR, Integer.parseInt(dateStr.substring(0,4)));
			c.set(Calendar.MONTH, Integer.parseInt(dateStr.substring(5,7))-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateStr.substring(8,10)));
			c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(dateStr.substring(11,13)));
			c.set(Calendar.MINUTE,Integer.parseInt(dateStr.substring(14,16)));
			info("FILENAME: " + filename + " Reconstructed date: " + c.toString());
			long fileTimeMillis = c.getTimeInMillis();
			long diff = System.currentTimeMillis() - fileTimeMillis;
			info("FILENAME: " + filename + " DIFF: " + diff/1000 + " seconds");
			if(diff > MsdConfig.getBasebandLogKeepDurationHours(MsdService.this) * 60 * 60 * 1000){
				info("Deleting file: " + filename);
				deleteFile(filename);
			}
		}
	}
	private void cleanupDatabase(){
		try{
			MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(MsdService.this));
			SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
			String sql = "DELETE FROM session_info where timestamp < datetime('now','-" + MsdConfig.getSessionInfoKeepDurationHours(MsdService.this) + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			sql = "DELETE FROM location_info where timestamp < datetime('now','-" + MsdConfig.getLocationLogKeepDurationHours(MsdService.this) + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			sql = "DELETE FROM serving_cell_info where timestamp < datetime('now','-" + MsdConfig.getCellInfoKeepDurationHours(MsdService.this) + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			sql = "DELETE FROM neighboring_cell_info where timestamp < datetime('now','-" + MsdConfig.getCellInfoKeepDurationHours(MsdService.this) + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			// Delete everything for now, as we do not pass the next valid sequence number
			// from the app to the parser at the moment
			sql = "DELETE FROM cell_info;";
			info("cleanup: " + sql);
			db.execSQL(sql);
		} catch(SQLException e){
			handleFatalError("SQL Exception during cleanup",e);
		}
	}
}
