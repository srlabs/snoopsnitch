package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class AboutActivity extends BaseActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		TextView aboutText = (TextView) findViewById (R.id.aboutText);
		
		String aboutContent =
				"SnoopSnitch " +
				this.getString(R.string.app_version) +
				"\n\n" +
				this.getString(R.string.about_text) +
				"\n\n" +
				this.getString(R.string.copyright_text);

		aboutText.setText(aboutContent);
	}
}
