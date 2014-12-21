package de.srlabs.msd;

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

	@Override
	public boolean onCreateOptionsMenu(Menu _menu)
	{
		super.onCreateOptionsMenu(_menu);

		// Disable map, recording icon and preferences
		menu.getItem(0).setVisible(false);
		menu.getItem(1).setVisible(false);

		return true;
	}
}
