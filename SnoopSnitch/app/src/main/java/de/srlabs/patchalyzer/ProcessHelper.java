package de.srlabs.patchalyzer;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import de.srlabs.patchalyzer.Constants;
import de.srlabs.patchalyzer.TestUtils;

/**This class contains all the helper methods for calling subprocess like e.g. objdump
 * Created by jonas on 15.12.17.
 */

public class ProcessHelper {


    public static final String BIN_PATH="/data/data/de.srlabs.patchalyzer/lib/";
    public static final String OBJDUMP_PATH = BIN_PATH+"libobjdump.so";

    public static Vector<String> execProcessAndGetStdout(String[] cmd) throws Exception {
        Log.d(Constants.LOG_TAG,"ProcessHelper: running command: "+ Arrays.toString(cmd));
        return runCommand(cmd);
    }

    public static int execProcessAndGetExitValue(String name, String[] cmd) throws Exception{
        Log.i(Constants.LOG_TAG, "ProcessHelper: "+name+" running command: " + cmd.toString());
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();
        return p.exitValue();
    }

    public static List<String> getObjDumptTOutput(String filePath) throws Exception{
        return execProcessAndGetStdout(new String[]{OBJDUMP_PATH,"-tT", filePath});
    }

    public static List<String> getObjDumpHW(String filepath) throws Exception{
        return runCommand(new String[]{OBJDUMP_PATH,"-h","-w",filepath});
        //return execProcessAndGetStdout("objdump",new String[]{OBJDUMP_PATH,"-h","-w",filepath});
    }

    public static List<String> getObjDumpHWwithCheck(String filePath) throws Exception{
        return getObjDumpHW(filePath);
    }

    public static String getFileArchitecture(String filepath) throws Exception{
        Vector<String> lines = execProcessAndGetStdout(new String[]{"file",filepath});
        if(lines != null && lines.size()>0){
            return lines.get(0);
        }
        return null;
    }

    public static boolean stripSymbolsFromObjFile(String filepath, String tempFilePath) throws Exception{
        int exitValue = execProcessAndGetExitValue("strip",new String[]{"strip","-o",tempFilePath,filepath});
        return (exitValue == 0);
    }

    public static String sha256(byte[] base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base);
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public static Vector<String> runObjdumpCommand(String... params) throws IOException, InterruptedException {
        String[] cmd = new String[params.length + 1];
        cmd[0] = "/data/data/de.srlabs.patchalyzer/lib/libobjdump.so";
        for (int i = 0; i < params.length; i++) {
            cmd[i + 1] = params[i];
        }
        String cmdStr = "";
        for(String x:cmd){
            cmdStr += cmd + " ";
        }
        //Log.i(Constants.LOG_TAG, "runObjdumpCommand: " + cmdStr);
        return runCommand(cmd);
    }
    public static Vector<String> runCommand(String[] cmd) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        final Vector<String> stderrLines = new Vector<>();
        Thread stderrThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    String line = null;
                    try {
                        line = stderr.readLine();
                    } catch (IOException e) {
                        return;
                    }
                    if (line == null)
                        return;
                    stderrLines.add(line);
                }
            }
        };
        stderrThread.start();
        Vector<String> result = new Vector<>();
        while (true) {
            String line = stdout.readLine();
            if (line == null)
                break;
            result.add(line);
        }
        p.waitFor();
        stderrThread.join();
        int exitValue = p.exitValue();
        // On some phones the linker prints an error message ("unused DT entry") to stderr. This message can be safely ignored while other messages on stderr should be handled as an error.
        boolean realErrorOnStderr = false;
        for(String line:stderrLines){
            if(line.contains("unused DT entry"))
                continue;
            realErrorOnStderr = true;
        }
        if (exitValue == 0 && !realErrorOnStderr) {
            return result;
        } else {
            throw new IllegalStateException("objdump terminated with exit code " + exitValue + " STDERR: \n" + TextUtils.join("\n", stderrLines));
        }
    }

    public static JSONObject getSymbolTableEntry(String filename, String symbol) throws JSONException, IOException, InterruptedException {
        Vector<String> objdumpLines = runObjdumpCommand("-tT", filename);
        for(String line:objdumpLines){
            if(!line.contains(symbol))
                continue;
            line = line.trim();
            String[] components = line.split("\\s+");
            if(components[components.length-1].equals(symbol)){
                String addrHex = components[0];
                String lenHex;
                if(components[components.length-2].equals("Base")){
                    lenHex = components[components.length - 3];
                } else{
                    lenHex = components[components.length - 2];
                }
                long addr = Long.parseLong(addrHex, 16);
                long len = Long.parseLong(lenHex, 16);
                JSONObject entry = new JSONObject();
                entry.put("addr", addr);
                entry.put("len", len);
                return entry;
            }
        }
        return null;
    }
}
