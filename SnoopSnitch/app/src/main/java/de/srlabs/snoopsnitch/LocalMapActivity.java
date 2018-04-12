package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class LocalMapActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            final WebView webview = (WebView) findViewById(R.id.local_map_web_view);
            webview.getSettings().setJavaScriptEnabled(true);
            String mcc = String.valueOf(msdServiceHelperCreator.getMsdServiceHelper().getData().getScores().getMcc());
            Log.i(getLocalClassName(), "Showing gsmmap for MCC: "+mcc);

            webview.loadUrl("https://gsmmap.org/?n=" + mcc);
        } catch (Exception e) {
            Log.e(getLocalClassName(), e.getMessage());
        }
    }
}
