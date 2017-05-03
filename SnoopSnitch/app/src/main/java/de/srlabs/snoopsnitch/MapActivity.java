package de.srlabs.snoopsnitch;

import de.srlabs.snoopsnitch.R;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;

public class MapActivity extends BaseActivity 
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		try
		{
	        WebView webview = (WebView) findViewById(R.id.map_web_view);
	        webview.getSettings().setJavaScriptEnabled(true);
	        
	        // We have to set margin/padding to zero to avoid
	        // a white border around the web view
	        String customHtml = "<html><head><style>* {margin:0;padding:0;}</style></head><body><iframe src=\"https://gsmmap.org/?n=0\" width=\"100%\" height=\"100%\" scrolling=\"auto\" frameborder=\"0\" ></iframe></body></html>";
	        webview.loadData(customHtml, "text/html", "UTF-8");
		}
		catch (Exception e)
		{
			Log.e(getLocalClassName(), e.getMessage());
		}
	}
}
