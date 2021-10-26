package de.srlabs.snoopsnitch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;


public class ProminentDisclosureActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prominent_disclosure);
        getActionBar().hide();

        WebView webview = (WebView) this.findViewById(R.id.firstRunWebView);
        String content = getString(R.string.alert_first_app_start_message);
        webview.loadData(content, "text/html; charset=utf-8", "UTF-8");

        Button button = (Button) findViewById(R.id.agreeFirstRunButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = getIntent();
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    // @Override
    // public void onBackPressed() {
    //     moveTaskToBack(true);
    // }
}
