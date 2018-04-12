package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;
import de.srlabs.snoopsnitch.qdmon.Operator;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

public class MapActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            WebView webview = (WebView) findViewById(R.id.map_web_view);
            webview.getSettings().setJavaScriptEnabled(true);

            Operator currentOperator = new Operator(this);
            String mcc = ""+currentOperator.getMcc();
            Log.i("SNSN: "+getLocalClassName(), "Showing gsmmap for MCC: "+mcc);
            webview.loadUrl("https://gsmmap.org/?n=" + mcc);
        } catch (Exception e) {
            Log.e(getLocalClassName(), e.getMessage());
        }
    }
}
