package de.srlabs.msd.qdmon;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class MsdService extends Service{
	public static final String    TAG                   = "msd-service";
	/**
	 * Registers a client Messenger (given via the msg.replyTo) for all
	 * notifications from this service.
	 */
	public static final int MSG_REGISTER_CLIENT = 1;
	/**
	 * Unregisters a client messenger, reverses the effect of
	 * MSG_REGISTER_CLIENT
	 */
	public static final int MSG_UNREGISTER_CLIENT = 2;
	/**
	 * Start the recording of baseband messages, phone info and GPS locations
	 */
	public static final int MSG_START_RECORDING = 3;
	/**
	 * Stops recording again
	 */
	public static final int MSG_STOP_RECORDING = 4;
	/**
	 * Restarts recording, used e.g. after changing any settings related to what
	 * data is recorded so that the configuration changes are actually applied
	 */
	public static final int MSG_RESTART_RECORDING = 5;
	/**
	 * The service sends this message to all registered clients when the
	 * recording state changes either due to a
	 * MSG_START_RECORDING/MSG_STOP_RECORDING or due to an internal error of
	 * this service. msg.arg1 indicates whether recording is running (msg.arg1
	 * == 1) or not (msg.arg1 == 0).
	 */
	public static final int MSG_RECORDING_STATE = 6;
	/**
	 * The service sends this message to all registered clients when an error
	 * occurs. The textual error message (msg.obj.toString()) should be
	 * displayed to the user.
	 */
	public static final int MSG_ERROR_STR = 7;
	/**
	 * Sent from the user interface to the service to trigger a cleanup
	 * operation. The app should send this message after changing the privacy
	 * settings so that old data is actually purged. During the normal operation
	 * of the service, the cleanup will be done automatically.
	 */
	public static final int MSG_TRIGGER_CLEANUP = 8;
	/**
	 * This message is sent by MsdService to indicate that a new session has
	 * been added to the database. It should be used to trigger the IMSI Catcher
	 * detection.
	 */
	public static final int MSG_NEW_SESSION = 9;

	List<Messenger>               clients               = new ArrayList<Messenger>();
	final Messenger               serviceMessenger      = new Messenger(new IncomingHandler());

	AtomicBoolean shuttingDown          = new AtomicBoolean(false);

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
	private Handler periodicCheckRecordingStateHandler = new Handler();
	private PeriodicCheckRecordingStateRunnable periodicCheckRecordingStateRunnable = new PeriodicCheckRecordingStateRunnable();
	private boolean recording = false;
	private LocationManager locationManager;
	private MyLocationListener myLocationListener;
	private MyPhoneStateListener myPhoneStateListener;
	private TelephonyManager telephonyManager;
	private PendingSqliteStatement last_sc_insert;

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
		return this.serviceMessenger.getBinder();
	}

	@SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				clients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				clients.remove(msg.replyTo);
				break;
			case MSG_START_RECORDING:
				Log.i(TAG, "MsdService.IncomingHandler.handleMessage(MSG_START_RECORDING)");
				if(recording)
					sendErrorMessage("MsdService.IncomingHandler.handleMessage(MSG_START_RECORDING) received while already recording");
				else
					MsdService.this.startRecording();
				break;
			case MSG_STOP_RECORDING:
				Log.i(TAG, "MsdService.IncomingHandler.handleMessage(MSG_STOP_RECORDING)");
				if(recording)
					MsdService.this.shutdown(false);
				else
					sendErrorMessage("MsdService.IncomingHandler.handleMessage(MSG_STOP_RECORDING) received while not recording");
				break;
			case MSG_RESTART_RECORDING:
				Log.i(TAG, "MsdService.IncomingHandler.handleMessage(MSG_RESTART_RECORDING)");
				if(recording)
					MsdService.this.shutdown(false);
				else
					sendErrorMessage("MsdService.IncomingHandler.handleMessage(MSG_STOP_RECORDING) received while not recording");
				MsdService.this.startRecording();
				break;
			case MSG_TRIGGER_CLEANUP:
				Log.i(TAG, "MsdService.IncomingHandler.handleMessage(MSG_TRIGGER_CLEANUP)");
				cleanupDatabase();
				cleanupRawFiles();
				break;
			default:
				sendErrorMessage("MsdService.IncomingHandler.handleMessage(unknown message: " + msg.what + ")");
				super.handleMessage(msg);
			}
		}
	}
	class PeriodicCheckRecordingStateRunnable implements Runnable{
		@Override
		public void run() {
			checkRecordingState();
			periodicCheckRecordingStateHandler.postDelayed(this, 1000);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		info("MsdService.onCreate() called");
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
		if(MsdServiceConfig.gpsRecordingEnabled())
			locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 10000, 0, myLocationListener);
		if(MsdServiceConfig.networkLocationRecordingEnabled())
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
	private synchronized void startRecording() {
		if(!readyForStartRecording.compareAndSet(true, false)){
			handleFatalError("MsdService.startRecording called but readyForStartRecording is not true. Probably there was an error during the last shutdown");
			return;
		}
		try {
			this.shuttingDown.set(false);
			this.sqliteThread = new SqliteThread();
			sqliteThread.start();
			launchParser();
			launchRawFileWriter();
			launchHelper();
			startLocationRecording();
			startPhoneStateRecording();
			broadcastMessage(Message.obtain(null, MSG_RECORDING_STATE, 1, 0));
			this.recording  = true;
			periodicCheckRecordingStateHandler.removeCallbacks(periodicCheckRecordingStateRunnable);
			periodicCheckRecordingStateHandler.post(periodicCheckRecordingStateRunnable);
		} catch (IOException e) { 
			Log.e(TAG, "IO exception in startRecording()", e);
			shutdown(false);
			broadcastMessage(Message.obtain(null, MSG_RECORDING_STATE, 0, 0));
		}
	}

	private synchronized void shutdown(boolean shuttingDownAlreadySet){
		try{
			info("MsdService.shutdown(" + shuttingDownAlreadySet + ") called");
			if(shuttingDownAlreadySet){
				if(!this.shuttingDown.get()){
					handleFatalError("MsdService.shutdown(true) called but shuttingDown is not set");
					return;
				}
			} else{
				if (!this.shuttingDown.compareAndSet(false, true)){
					handleFatalError("MsdService.shutdown(false) called while shuttingDown is already set");
					return;
				}
			}
			shutdownError = false;
			periodicCheckRecordingStateHandler.removeCallbacks(periodicCheckRecordingStateRunnable);
			// DIAG Helper
			try{
				this.helper.exitValue();
			} catch(IllegalThreadStateException e){
				// The helper is still running, so let's send DisableLoggingCmds
				for(byte[] singleCmd:DisableLoggingCmds.cmds){
					this.toDiagMsgQueue.add(new QueueElementWrapper<byte[]>(singleCmd));
				}
				//this.toDiagMsgQueue.addAll(Arrays.asList(DisableLoggingCmds.cmds));
			}
			// this.toDiagMsgQueue;
			//this.toDiagThread.interrupt();
			this.toDiagMsgQueue.add(new QueueElementWrapper<byte[]>());
			this.toDiagThread.join(3000);

			if(toDiagThread.isAlive()){
				handleFatalError("Failed to stop toDiagThread");
			}
			try {
				this.diagStdin.close();
			} catch (IOException e) {
				handleFatalError("IOException while closing diagStdin" , e);
			}
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
			this.fromDiagThread.join();
			diagStdin = null;
			diagStdout = null;
			diagStderr = null;
			// Terminate raw file write
			rawFileWriterMsgQueue.add(new DiagMsgWrapper()); // Send shutdown marker to message queue
			rawFileWriterThread.join(3000);
			if(rawFileWriterThread.isAlive()){
				handleFatalError("Failed to stop rawFileWriter");
			}
			// Termiante parser
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
			t = new Thread(){
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
			stopLocationRecording();
			stopPhoneStateRecording();
			sqliteThread.shuttingDown = true;
			// Add finish marker at end of pendingSqlStatements so that sqliteThread shuts down
			pendingSqlStatements.add(new PendingSqliteStatement(null));
			sqliteThread.join(3000);
			if(sqliteThread.isAlive()){
				handleFatalError("Failed to stop sqliteThread");
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
			Message msg = Message.obtain(null, MSG_RECORDING_STATE, 0, 0);
			broadcastMessage(msg);
			this.recording = false;
			this.readyForStartRecording.set(!shutdownError);
			this.shuttingDown.set(false);
		} catch (InterruptedException e1) {
			handleFatalError("Received InterruptedException during shutdown");
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
					info("Writing message to parser, length=" + msg.buf.length);
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
	class RawFileWriterThread extends Thread{
		public void run() {
			String currentDumpfileName = null;
			GZIPOutputStream out = null;
			try {
				while(true){
					DiagMsgWrapper msg = rawFileWriterMsgQueue.take();
					if(msg.shutdownMarker){
						info("ToParserThread shutting down due to shutdown marker, OK");
						if(out != null)
							out.close();
						return;
					}
					// Create a new dump file every 10 minutes, name it with the
					// UTC time so that it does not reuse the same filename if
					// the user	switches between different timezones.
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
					// Calendar.MONTH starts counting with 0
					String filename = String.format(Locale.US, "qdmon_%04d-%02d-%02d_%02d-%02dUTC.gz",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY), 10*(c.get(Calendar.MINUTE) / 10));
					if(currentDumpfileName == null || !filename.equals(currentDumpfileName)){
						info("Opening new raw file " + filename);
						// Use MODE_APPEND so that it appends to the existing
						// file if the file already exists (e.g. because the
						// user stopped recording or restarted the app in
						// between). Gzip does allow concatenating files so it
						// is acceptable to just append to an existing gzip
						// file.
						out = new GZIPOutputStream(openFileOutput(filename, Context.MODE_APPEND));
						currentDumpfileName = filename;
					}
					out.write(msg.buf);
				}
			} catch (InterruptedException e) {
				handleFatalError("RawFileWriterThread shutting down due to InterruptedException");
			} catch (IOException e) {
				if(MsdService.this.shuttingDown.get())
					info("ToParserThread: IOException while writing to parser while shutting down: " + e.getMessage());
				else
					handleFatalError("ToParserThread: IOException while writing to parser: " + e.getMessage());
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
						pendingSqlStatements.add(new PendingSqliteStatement(sql){
							@Override
							void postRunHook() {
								broadcastMessage(Message.obtain(null, MSG_NEW_SESSION));
							}
						});
					} else{
						info("FromParserThread received invalid line: " + line);						
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
		public void run() {
			MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(MsdService.this);
			SQLiteDatabase db = msdSQLiteOpenHelper.getWritableDatabase();
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
				info("Generated row " + generatedRowId + " in table " + table);
			}
			if(sql != null)
				db.execSQL(sql);
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
					sendErrorMessage("Invalid networkOperatr: " + networkOperator);
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
				sendErrorMessage("onCellLocationChanged() called with invalid location class: " + location.getClass());
		}

		@Override
		public void onCellInfoChanged(List<CellInfo> cellInfo) {
			if(cellInfo == null || cellInfo.size() == 0)
				return;
			// I do not know whether this code will be reached at all. Most
			// phones only call this method with null as a parameter. So let's
			// send a message so that we find out when it is called.
			String msg = "onCellInfoChanged(" + ((cellInfo == null) ? "null" : cellInfo.size()) + ")"; 
			sendErrorMessage(msg);
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
		String cmd[] = {parser_binary};
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
	private void launchRawFileWriter(){
		this.rawFileWriterThread = new RawFileWriterThread();
		this.rawFileWriterThread.start();
	}
	private void launchHelper() throws IOException {
		String libdir = this.getApplicationInfo().nativeLibraryDir;
		String diag_helper = libdir + "/libdiag-helper.so";
		String cmd[] = { "su", "-c", "exec " + diag_helper + " run"};

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

	private void broadcastMessage(Message msg) {
		for (Messenger r : this.clients) {
			try {
				Message sendMsg = Message.obtain(msg);
				r.send(sendMsg);
			} catch (RemoteException e) {
				// XXX handle disappearing client
			}
		}
		msg.recycle();
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
	private void sendErrorMessage(String msg){
		sendErrorMessage(msg, null);
	}
	private void sendErrorMessage(String msg, Exception e){
		if(e == null){
			Log.e(TAG,msg);
		} else{
			Log.e(TAG,msg,e);
			msg += e.getMessage();
		}
		Message sendMsg = Message.obtain(null, MSG_ERROR_STR);
		sendMsg.obj = msg;
		broadcastMessage(sendMsg);
	}
	private void handleFatalError(String msg, Exception e){
		boolean doShutdown = false;
		if(recording && shuttingDown.compareAndSet(false, true)){
			msg += " => shutting down service";
			doShutdown = true;
		} else if(recording){
			shutdownError = true;
			msg = "Error while shutting down: " + msg;
		} else{
			msg = "Error while not recording: " + msg;
		}
		sendErrorMessage(msg, e);
		if(doShutdown){
			// Call shutdown in the main thread so that the thread causing the Error can terminate
			periodicCheckRecordingStateHandler.post(new Runnable(){
				public void run() {
					shutdown(true);					
				};				
			});
		}
	}
	private void handleFatalError(String msg){
		handleFatalError(msg,null);
	}
	private static void info(String msg){
		Log.i(TAG,msg);
	}
	/**
	 * Checks whether all threads are still running and no message queue contains a huge number of messages
	 */
	private void checkRecordingState(){
		if(shuttingDown.get())
			return;
		boolean ok = true;
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
		if(pendingSqlStatements.size() > 100){
			handleFatalError("testRecordingState(): pendingSqlStatements contains too many entries");
			ok = false;
		}
		if(ok)
			info("testRecordingState(): Everything is OK");
	}
	private void cleanupRawFiles(){
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
			if(diff > MsdServiceConfig.getBasebandLogKeepDurationHours() * 60 * 60 * 1000){
				info("Deleting file: " + filename);
				deleteFile(filename);
			}
		}
	}
	private void cleanupDatabase(){
		try{
			MsdSQLiteOpenHelper msdSQLiteOpenHelper = new MsdSQLiteOpenHelper(MsdService.this);
			SQLiteDatabase db = msdSQLiteOpenHelper.getWritableDatabase();
			String sql = "DELETE FROM Session_Info where timestamp < datetime('now','-" + MsdServiceConfig.getSessionInfoKeepDurationHours() + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			sql = "DELETE FROM location_info where timestamp < datetime('now','-" + MsdServiceConfig.getLocationLogKeepDurationHours() + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			sql = "DELETE FROM serving_cell_info where timestamp < datetime('now','-" + MsdServiceConfig.getCellInfoKeepDurationHours() + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
			sql = "DELETE FROM neighboring_cell_info where timestamp < datetime('now','-" + MsdServiceConfig.getCellInfoKeepDurationHours() + " hours');";
			info("cleanup: " + sql);
			db.execSQL(sql);
		} catch(SQLException e){
			handleFatalError("SQL Exception during cleanup",e);
		}
	}
}
