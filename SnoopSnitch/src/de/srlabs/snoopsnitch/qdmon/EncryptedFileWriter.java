package de.srlabs.snoopsnitch.qdmon;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.EncryptedFileWriterError;

public class EncryptedFileWriter{
	public static final String TAG = "msd-EncryptedFileWriter";
	private String encryptedFilename;
	private boolean compressEncryptedFile;
	private boolean closed = false;
	private Context context;
	private Process openssl;
	private OutputStream encryptedOutputStream;
	private String plaintextFilename;
	private BufferedReader opensslStderr;
	private OpensslErrorThread opensslErrorThread;
	private BlockingQueue<MsgWrapper> msgQueue = new LinkedBlockingQueue<MsgWrapper>();
	private OutputStream plaintextOutputStream;
	private boolean compressPlaintextFile;
	private WriterThread writerThread;
	private long lastWriteTime = 0;
	private long lastFlushTime = 0;	
	private EncryptedFileWriterError error = null;
	
	public EncryptedFileWriter(Context context, String encryptedFilename, boolean compressEncryptedFile, String plaintextFilename, boolean compressPlaintextFile) throws EncryptedFileWriterError {
		this.context = context;
		this.encryptedFilename = encryptedFilename;
		this.compressEncryptedFile = compressEncryptedFile;
		this.plaintextFilename = plaintextFilename;
		this.compressPlaintextFile = compressPlaintextFile;
		openOutput();
	}
	@SuppressLint("NewApi")
	private void openOutput() throws EncryptedFileWriterError {
		if(encryptedFilename != null){
			info("Writing encrypted output to " + encryptedFilename);
			String libdir = context.getApplicationInfo().nativeLibraryDir;
			String openssl_binary = libdir + "/libopenssl.so";
			String crtFile = libdir + "/libsmime_crt.so";
			String cmd[] = {openssl_binary, "smime", "-encrypt", "-binary", "-aes256", "-outform", "DER", "-out", context.getFilesDir() + "/" + encryptedFilename, crtFile};
			String env[] = {"LD_LIBRARY_PATH=" + libdir, "OPENSSL_CONF=/dev/null", "RANDFILE=/dev/null"};
			info("Launching openssl: " + TextUtils.join(" ",cmd));
			try {
				openssl =  Runtime.getRuntime().exec(cmd, env, null);
			} catch (IOException e) {
				throw new EncryptedFileWriterError("IOException while launching openssl for file" + encryptedFilename,e);
			}
			encryptedOutputStream = openssl.getOutputStream();
			if(compressEncryptedFile){
				try {
					encryptedOutputStream = new GZIPOutputStream(encryptedOutputStream);
				} catch (IOException e) {
					throw new EncryptedFileWriterError("IOException while opening GZIPOutputStream in EncryptedFileWrite.openOutput, file=" + encryptedFilename);
				}
			}
			opensslStderr = new BufferedReader(new InputStreamReader(openssl.getErrorStream()));
			opensslErrorThread = new OpensslErrorThread();
			opensslErrorThread.start();
		}
		if(plaintextFilename != null){
			try {
				plaintextOutputStream = context.openFileOutput(plaintextFilename, Context.MODE_APPEND);
				if(compressPlaintextFile){
					plaintextOutputStream = new GZIPOutputStream(plaintextOutputStream);
				}
			} catch (IOException e) {
				throw new EncryptedFileWriterError("FileNotFoundException while opening plaintext output in EncryptedFileWrite.openOutput, file=" + encryptedFilename);
			}
		}
		writerThread = new WriterThread();
		writerThread.start();
		lastFlushTime = System.currentTimeMillis();
		closed = false;
	}
	class MsgWrapper{
		byte[] buf;
		public MsgWrapper(byte[] buf){
			this.buf = buf;
		}
	}
	class ShutdownMsgWrapper extends MsgWrapper{
		public ShutdownMsgWrapper() {
			super(null);
		}
	}
	class FlushMsgWrapper extends MsgWrapper{
		public Object markerReached = new Object();
		public Object flushDone = new Object();
		public FlushMsgWrapper() {
			super(null);
		}
	}
	class OpensslErrorThread extends Thread {
		private boolean closeOutputRunning;
		@Override
		public void run() {
			try {
				while(true){
					String line = opensslStderr.readLine();
					if(line == null){
						if(closeOutputRunning){
							info("opensslStderr.readLine() returned null while closeOutputRunning is set, OK");
						} else{
							throw new RuntimeException(new EncryptedFileWriterError("opensslStderr.readLine()u retrned null for file " + encryptedFilename));
						}
						return;
					}
					throw new RuntimeException(new EncryptedFileWriterError("Openssl Error for " + encryptedFilename + ": " + line));
				}
			} catch(EOFException e){
				if(closeOutputRunning){
					info("OpensslErrorThread received IOException while shutting down, OK");
				} else{
					throw new RuntimeException(new EncryptedFileWriterError("EOFException while reading from opensslStderr for " + encryptedFilename + ": " + e.getMessage()));
				}
			} catch(IOException e){
				throw new RuntimeException(new EncryptedFileWriterError("IOException while reading from opensslStderr for " + encryptedFilename + ": " + e.getMessage()));
			}
		}
	}
	class WriterThread extends Thread{

		@Override
		public void run() {
			try {
				while(true){
					MsgWrapper msg = msgQueue.take();
					if(msg instanceof ShutdownMsgWrapper){
						return;
					}
					if(msg instanceof FlushMsgWrapper){
						FlushMsgWrapper flushMsg = (FlushMsgWrapper)msg;
						synchronized(flushMsg.flushDone){
							synchronized(flushMsg.markerReached){
								flushMsg.markerReached.notify();
							}
							flushMsg.flushDone.wait();
						}
					} else{
						if(encryptedOutputStream != null){
							encryptedOutputStream.write(msg.buf);
						}
						if(plaintextOutputStream != null){
							plaintextOutputStream.write(msg.buf);
						}
					}
				}
			} catch (InterruptedException e) {
				error = new EncryptedFileWriterError("EncryptedFileWriter.WriterThread shutting down due to InterruptedException, file=" + encryptedFilename, e);
			} catch (IOException e) {
				try {
					error = new EncryptedFileWriterError("EncryptedFileWriter.WriterThread: IOException, file=" + encryptedFilename, e);
					close();
				} catch (EncryptedFileWriterError x)
				{
					// Don't overwrite earlier exception
				}
			}
		}
	}
	
	private void info(String msg){
		MsdLog.i(TAG + ":" + encryptedFilename, msg);
	}
	
	private void info(boolean execute, String msg){
		if (execute) {
			info(msg);
		}
	}
	
	public synchronized void write(byte[] data) throws EncryptedFileWriterError{
		if(closed){
			throw new IllegalStateException("Can't write data, EncrypteFileWriter is already closed");
		}
		if(error != null){
			throw error;
		}
		lastWriteTime = System.currentTimeMillis();
		msgQueue.add(new MsgWrapper(data));
	}
	
	public synchronized void write(String str) throws EncryptedFileWriterError{
		write(str.getBytes());
	}
	public synchronized void close() throws EncryptedFileWriterError{
		closed = true;
		// Use a WakeLock during close() so that the openssl process doesn't
		// hang and can terminate cleanly after closing its standard input
		PowerManager.WakeLock wl = null;
		try{
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
			wl.acquire();
			// Send shutdown marker
			msgQueue.add(new ShutdownMsgWrapper());
			boolean joinFailed = false;
			writerThread.join(3000);
			if(writerThread.isAlive()){
				joinFailed = true;
				info("EncryptedFileWriter.close() failed to stop writerThread");
			}
			writerThread.join();
			info(joinFailed, "Join succeeded");

			writerThread = null;
			opensslErrorThread.closeOutputRunning = true;
			if(encryptedOutputStream != null)
				encryptedOutputStream.close();
			encryptedOutputStream = null;
			if(plaintextOutputStream != null)
				plaintextOutputStream.close();
			plaintextOutputStream = null;
			if(openssl != null){
				info("Waiting for openssl to terminate during close()");
				Thread t = new Thread(){
					@Override
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
					info("openssl terminated with exit value " + exitValue);
				} catch(IllegalThreadStateException e){
					openssl.destroy();
					throw new EncryptedFileWriterError("EncryptedFileWriter.close() for file " + encryptedFilename + " failed to stop openssl, calling destroy(): " + e.getMessage());

				}
				openssl = null;
			}
			if(opensslErrorThread != null){
				opensslErrorThread.join(3000);
				if(opensslErrorThread.isAlive()){
					throw new EncryptedFileWriterError("EncryptedFileWriter.close() for file " + encryptedFilename + " failed to stop opensslErrorThread");
				}
				opensslErrorThread.join();
				opensslErrorThread = null;
			}
		} catch(InterruptedException e){
			throw new EncryptedFileWriterError("InterruptedException in EncryptedFileWriter.close() for file " + encryptedFilename,e);
		} catch (IOException e1) {
			throw new EncryptedFileWriterError("IOException in EncryptedFileWriter.close() for file " + encryptedFilename,e1);
		} finally{
			if(wl != null)
				wl.release();
		}
	}
	public synchronized void flush() throws EncryptedFileWriterError{
		lastFlushTime = System.currentTimeMillis();
		info("EncryptedFileWriter.flush called, queue size=" + getQueueSize());
		try{
			// Add a marker to the message queue and wait until the marker (and all messages before it) has been reached
			FlushMsgWrapper flushMsg = new FlushMsgWrapper();
			synchronized(flushMsg.markerReached){
				msgQueue.add(flushMsg);
				flushMsg.markerReached.wait();
			}
			if(encryptedOutputStream != null) {
				encryptedOutputStream.flush();
			}
			if(plaintextOutputStream != null) {
				if(compressPlaintextFile) {
					// GZIPOutputStream doesn't allow reliable flushing of output,
					// so let's just reopen the file with MODE_APPEND, gzip files
					// can be concatenated.
					plaintextOutputStream.close();
					plaintextOutputStream = context.openFileOutput(plaintextFilename, Context.MODE_APPEND);
					if(compressPlaintextFile){
						plaintextOutputStream = new GZIPOutputStream(plaintextOutputStream);
					}
				} else {
					plaintextOutputStream.flush();
				}
			}
			synchronized (flushMsg.flushDone) {
				flushMsg.flushDone.notifyAll();
			}
			info("EncryptedFileWriter.flush done");
		} catch(IOException e){
			throw new EncryptedFileWriterError("IOException in EncryptedFileWriter.flush(), file=" + encryptedFilename,e);
		} catch(Exception e){
			throw new EncryptedFileWriterError("Exception in EncryptedFileWriter.flush(), file=" + encryptedFilename,e);
		}
	}
	public synchronized void flushIfUnflushedDataSince(long millis) throws EncryptedFileWriterError{
		if(lastWriteTime == 0)
			return;
		if(lastWriteTime > lastFlushTime + millis){
			flush();
		}
	}
	public int getQueueSize(){
		return msgQueue.size();
	}

	public String getEncryptedFilename() {
		return encryptedFilename;
	}
	public String getPlaintextFilename() {
		return plaintextFilename;
	}
}
