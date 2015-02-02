package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;
import android.os.Bundle;
import android.view.Menu;

public class AboutActivity extends BaseActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
}
