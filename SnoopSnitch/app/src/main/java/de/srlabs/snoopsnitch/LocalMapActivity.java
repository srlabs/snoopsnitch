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

            // We have to set margin/padding to zero to avoid
            // a white border around the web view
            String mcc = String.valueOf(msdServiceHelperCreator.getMsdServiceHelper().getData().getScores().getMcc());
            Log.i(getLocalClassName(), "Showing gsmmap for MCC: "+mcc);

            String customHtml = "<html><head><style>* {margin:0;padding:0;}</style></head><body><iframe src=\"https://gsmmap.org/?n="
                    + mcc + "\" width=\"100%\" height=\"100%\" scrolling=\"auto\" frameborder=\"0\" ></iframe></body></html>";

            webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(webview, url);
                    Toast.makeText(getApplicationContext(), "Done!", Toast.LENGTH_SHORT).show();
                    Log.d("LocalMapActivity","Loaded map successfully!");
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(getApplicationContext(), "Error while loading map: " + description, Toast.LENGTH_SHORT).show();
                    Log.e("LocalMapActivity","Error while loading map: "+errorCode+": "+description);
                }
            });
            webview.loadData(customHtml, "text/html", "UTF-8");
            Toast.makeText(getApplicationContext(), "Loading map...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(getLocalClassName(), e.getMessage());
        }
    }
}
