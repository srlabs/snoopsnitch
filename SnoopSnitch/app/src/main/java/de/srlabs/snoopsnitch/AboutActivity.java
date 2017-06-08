package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;

import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import de.srlabs.snoopsnitch.BuildConfig;
import de.srlabs.snoopsnitch.util.MsdLog;


public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        TextView aboutText = (TextView) findViewById(R.id.aboutText);

        String aboutContent =
                "SnoopSnitch " +
                        BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")" +
                        "\n------------------------------------------------------------" +
                        "\n" +
                        MsdLog.getDeviceProps() +
                        "------------------------------------------------------------" +
                        "\n\n" +
                        this.getString(R.string.about_text) +
                        "\n\n" +
                        this.getString(R.string.copyright_text);

        aboutText.setText(aboutContent);
    }
}
