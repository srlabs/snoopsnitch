package de.srlabs.patchalyzer.analysis.java_basic_tests;


import android.content.Context;
import java.util.HashMap;
import de.srlabs.patchalyzer.analysis.TestUtils;

public class AslrTest implements JavaBasicTest {
    @Override
    public Boolean runTest(Context c) throws Exception {
        String mapsLines1[] = TestUtils.readProcSelfMaps().split("\n");
        String mapsLines2[] = TestUtils.readProcSelfMaps().split("\n");
        HashMap<String, String> map1 = new HashMap<String, String>();
        for(String line: mapsLines1){
            line = line.trim();
            if(!line.endsWith(".so") || !line.contains("/system/")){
                continue;
            }
            String startAddr = line.split("-")[0];
            String soname = line.substring(line.indexOf("/system/"));
            map1.put(startAddr, soname);
        }
        int countEqual = 0;
        int countNotMapped = 0;
        int countDifferentMapping = 0;
        for(String line: mapsLines2){
            line = line.trim();
            if(!line.endsWith(".so") || !line.contains("/system/")){
                continue;
            }
            String startAddr = line.split("-")[0];
            String soname = line.substring(line.indexOf("/system/"));
            if(map1.containsKey(startAddr) && soname.equals(map1.get(startAddr))){
                countEqual++;
            } else if(map1.containsKey(startAddr)){
                countDifferentMapping++;
            } else{
                countNotMapped++;
            }
        }
        int countTotal = countEqual + countNotMapped + countDifferentMapping;
        if(countTotal == 0) {
            throw new IllegalStateException("Could not find /system/.../*.so mappings in /proc/self/maps!");
        }
        if(countNotMapped >= 1 && (countNotMapped + countDifferentMapping) > 0.5*countTotal) {
            return true;
        } else if(countEqual > 0.5*countTotal){
            return false;
        } else{
            throw new IllegalStateException("Inconsistant results for ASLR test: countTotal=" + countTotal + "  countNotMapped=" + countNotMapped + "  countEqual=" + countEqual + "  countDifferentMapping=" + countDifferentMapping);
        }
    }
}
