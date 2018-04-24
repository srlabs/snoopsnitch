package de.srlabs.snoopsnitch;

import android.app.Application;
import android.util.Log;

import de.srlabs.patchalyzer_module.AppFlavor;
import de.srlabs.patchalyzer_module.Constants;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Log.d(Constants.LOG_TAG,"app run...");
        AppFlavor.setAppFlavor(new PAAppFlavorSNSN());
    }
}