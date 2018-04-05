package de.srlabs.patchalyzer.java_basic_tests;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.srlabs.patchalyzer.Constants;

public class SELinuxTest implements JavaBasicTest {
    @Override
    public Boolean runTest(Context c) throws IOException, InterruptedException {
        File testFile = new File("/sys/fs/selinux/enforce");

        if(!testFile.exists())
            throw new IOException("SELinux testfile does not exist: /sys/fs/selinux/enforce");

        BufferedReader reader = new BufferedReader(new FileReader(testFile));
        String line = reader.readLine();
        Log.d(Constants.LOG_TAG,"SELinux Test: "+line);
        return line.equals("1");

        //return false;
        /*String[] cmd = {"getenforce"};
        Process p = Runtime.getRuntime().exec(cmd);
        byte[] buf = new byte[1024];
        int numRead = p.getInputStream().read(buf);
        String stdoutData = "";
        if(numRead > 0){
            stdoutData = new String(buf, 0, numRead);
        }
        numRead = p.getErrorStream().read(buf);
        String stderrData = "";
        if(numRead > 0) {
            stderrData = new String(buf, 0, numRead);
        }
        p.waitFor();
        int exitCode = p.exitValue();
        if(exitCode == 0 && stderrData.length() == 0 && stdoutData.trim().equals("Enforcing")){
            return true;
        } else if(exitCode == 0 && stderrData.length() == 0 && stdoutData.trim().equals("Permissive")){
            return false;
        } else {
            throw new IllegalStateException("SELinuxTest failed: exitCode=" + exitCode + "stdout='" + stdoutData + "'  stderr='" + stderrData + "'");
        }*/
    }
}
