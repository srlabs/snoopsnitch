package de.srlabs.patchalyzer;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DirectoryTreeLister {
    private BlockingQueue<Runnable> hashingQueue;
    private Object lock = new Object();
    private JSONObject result = null;
    private long totalSize = 0;
    private long sizeHashed = 0;
    private long sizeHashedLastProgress = 0;
    private ProgressCallback callback;
    public interface ProgressCallback{
        public void reportProgress(double progress);
    }
    private DirectoryTreeLister(File f, ProgressCallback callback) {
        this.callback = callback;
        hashingQueue = new LinkedBlockingDeque();
        try {
            result = makeFilelistRecursive(f);
            callback.reportProgress(0.2); // 20% for making the directory list, 80% for hashing
        } catch (JSONException e) {
            Log.e(Constants.LOG_TAG, "JSONException in makeFilelistRecursive", e);
        }
    }
    private void doHashing(){
        // Start threads
        Vector<Thread> threads = new Vector<Thread>();
        for(int i=0; i<4; i++){
            Thread t = new HashQueueThread();
            t.start();
            threads.add(t);
        }
        // Send null to terminate threads
        for(int i=0;i<threads.size();i++){
            hashingQueue.add(new DummyRunnable());
        }
        // Join threads
        for(Thread t:threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                Log.e(Constants.LOG_TAG, "InterruptedException in doHashing => t.join()", e);
            }
        }
    }
    public static JSONObject makeFilelist(File f, ProgressCallback callback){
        DirectoryTreeLister dtl = new DirectoryTreeLister(f, callback);
        dtl.doHashing();
        callback.reportProgress(1.0);
        return dtl.result;
    }
    private JSONObject makeFilelistRecursive(File f) throws JSONException {
        JSONObject result = new JSONObject();
        File cf = null;
        try {
            cf = f.getCanonicalFile();
        } catch(IOException e){
        }
        if(cf == null || cf.equals(f.getAbsoluteFile())) {
            if (f.isFile()) {
                result.put("type", "FILE");
                long size = f.length();
                result.put("size", size);
                totalSize += size;
                result.put("lastModified", f.lastModified());
                if(f.canRead()) {
                    hashingQueue.add(new HashQueueRunnable(f, result));
                } else{
                    result.put("error", "Can't read file " + f.getAbsolutePath());
                }
            } else if (f.isDirectory()) {
                result.put("type", "DIRECTORY");
                result.put("lastModified", f.lastModified());
                JSONObject children = new JSONObject();
                File[] childFiles = f.listFiles();
                if(childFiles == null)
                    result.put("error", "READ_DIR_ERROR");
                else {
                    for (File child : childFiles) {
                        children.put(child.getName(), makeFilelistRecursive(child));
                    }
                    synchronized(lock) {
                        result.put("children", children);
                    }
                }
            }
        } else{
            result.put("type", "SYMLINK");
            result.put("symlinkDest", cf.getAbsolutePath());
            result.put("size", f.length());
            result.put("lastModified", f.lastModified());
        }
        return result;
    }
    class HashQueueThread extends Thread{
        @Override
        public void run() {
            while(true){
                try {
                    Runnable element = hashingQueue.take();
                    if(element instanceof DummyRunnable)
                        return;
                    element.run();
                } catch (InterruptedException e) {
                }
            }
        }
    }
    class DummyRunnable implements Runnable{
        @Override
        public void run() {
        }
    }
    class HashQueueRunnable implements Runnable{
        private JSONObject json;
        private File f;
        HashQueueRunnable(File f, JSONObject json){
            this.f = f;
            this.json = json;
        }
        @Override
        public void run() {
            String hash = sha256File(f);
            try {
                synchronized (lock) {
                    json.put("sha256", hash);
                }
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, "JSONException in HashQueueRunnable.run()", e);
            }
        }
    }
    public String sha256File(File f){
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(f));
            byte[] buf = new byte[4096];
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            int nread = 0;
            long size = 0;
            while ((nread = bis.read(buf)) != -1) {
                md.update(buf, 0, nread);
                size += nread;
            }
            synchronized (callback) {
                sizeHashed += size;
                if(sizeHashed > sizeHashedLastProgress + 1024*1024){
                    callback.reportProgress(0.2 + 0.8*((double)sizeHashed/(double)totalSize));
                    sizeHashedLastProgress = sizeHashed;
                }
            }
            byte[] digest = md.digest();
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(Constants.LOG_TAG, "NoSuchAlgorithmException", e);
            return "NoSuchAlgorithmException";
        } catch(IOException e){
            Log.e(Constants.LOG_TAG, "IOException in sha256File " + f, e);
            return "IOException";
        } finally{
            try {
                if(bis != null) {
                    bis.close();
                }
            } catch(IOException e){
                Log.e(Constants.LOG_TAG, "IOException in sha256File => close() " + f, e);
            }
        }
    }
}
