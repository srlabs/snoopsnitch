package de.srlabs.msd;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;

public class LocalMapActivity extends BaseActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_local_map);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		try
		{
	        WebView webview = (WebView) findViewById(R.id.local_map_web_view);
	        webview.getSettings().setJavaScriptEnabled(true);
	        
	        // We have to set margin/padding to zero to avoid
	        // a white border around the web view
	        String mcc = String.valueOf(msdServiceHelperCreator.getMsdServiceHelper().getData().getScores().getMcc());
	        String customHtml = "<html><head><style>* {margin:0;padding:0;}</style></head><body><iframe src=\"http://gsmmap.org/?n=" 
	        		+ mcc + "\" width=\"100%\" height=\"100%\" scrolling=\"auto\" frameborder=\"0\" ></iframe></body></html>";
	        webview.loadData(customHtml, "text/html", "UTF-8");
		}
		catch (Exception e)
		{
			Log.e(getLocalClassName(), e.getMessage());
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu _menu) 
	{
		super.onCreateOptionsMenu(_menu);
		
		// Disable map and recording icon
		menu.getItem(0).setVisible(false);
		menu.getItem(1).setVisible(false);
		
		return true;
	}
}
