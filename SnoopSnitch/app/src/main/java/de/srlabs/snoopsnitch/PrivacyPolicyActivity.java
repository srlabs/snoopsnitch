package de.srlabs.snoopsnitch;

import android.os.Bundle;
import android.webkit.WebView;


public class PrivacyPolicyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_privacy_policy);

        WebView webview = (WebView)this.findViewById(R.id.privacyPolicyWebView);
        String content = getString(R.string.privacy_policy);
        webview.loadData(content, "text/html; charset=utf-8", "UTF-8");
    }
}
