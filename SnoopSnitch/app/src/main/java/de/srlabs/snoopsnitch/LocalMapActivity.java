package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class LocalMapActivity extends BaseActivity {
    private static final String mTAG = "LocalMapActivity";
    private final boolean DEBUG = true;
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
                    Log.d(mTAG,"Loaded map successfully!");
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(getApplicationContext(), "Error while loading map: " + description, Toast.LENGTH_SHORT).show();
                    Log.e(mTAG,"Error while loading map: "+errorCode+": "+description);
                }

                @Override
                public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse){
                    Log.e(mTAG,"HTTP error while loading map: request: "+request.toString()+" response:"+errorResponse);
                }

                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
                    Log.e(mTAG,"SSL error while loading map: error:"+error.toString());
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon){
                    if(DEBUG)
                        Log.d(mTAG,"onPageStarted: "+url);
                }

                @Override
                public void onLoadResource(WebView view, String url){
                    if(DEBUG)
                        Log.d(mTAG,"onLoadResource: "+url);
                }
            });
            webview.loadData(customHtml, "text/html", "UTF-8");
            Toast.makeText(getApplicationContext(), "Loading map...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(getLocalClassName(), e.getMessage());
        }
    }
}
