package com.libcompass.androidapp;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled") 
public class MainActivity extends Activity {

	/* Display www.libcompass.com in the main WebView and ads in the small
	 * WebView. */

	protected static final String version = "0.8.2";
	private static Activity act;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		act = this;
		MyFragment frag = new MyFragment();
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, frag).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
        if (id == R.id.menu_home) { // load the home page in the main webview
        	WebView myWebView = (WebView) findViewById(R.id.webview);
        	myWebView.loadUrl("http://www.libcompass.com");
            return true;
        }
        if (id == R.id.menu_about) { // display "About" dialog
        	new AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Liberty Compass News\nVersion: " + version + "\n\nReport Bugs: android@libcompass.com")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) { 
                }
             })
            .setIcon(android.R.drawable.ic_dialog_info)
             .show();
        }
        if (id == R.id.menu_check_update) { // check server for new version of this app, display dialog with result
        	if (MyWidgetProvider.newVersionAvailable(getBaseContext())) {
        		new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("There is a new version of Liberty Compass News available.\nCheck notifications to update.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                    }
                 })
                .setIcon(android.R.drawable.ic_dialog_info)
                 .show();
        	} else {
        		new AlertDialog.Builder(this)
                .setTitle("No Update Available")
                .setMessage("This version of Liberty Compass News is up to date.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { 
                    }
                 })
                .setIcon(android.R.drawable.ic_dialog_info)
                 .show();
        	}
        }
        return super.onOptionsItemSelected(item);
	}

	/**
	 * The view fragment of the main activity 
	 */
	public static class MyFragment extends Fragment {
		
		private static WebView wv, av;
		private Bundle webViewBundle;
		
		public MyFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.fragment_main, container,
					false);
			// restore state if possible
			if (webViewBundle != null) {
				wv.restoreState(webViewBundle); 
				av.restoreState(webViewBundle);
			}
			else { // initialize new state
				wv = (WebView) v.findViewById(R.id.webview);
				wv.getSettings().setJavaScriptEnabled(true);
				wv.setWebViewClient(new WebViewClient());
				av = (WebView) v.findViewById(R.id.adview);
				av.getSettings().setJavaScriptEnabled(true);
				av.setWebViewClient(new WebViewClient(){
		    	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
		    	        if (url != null) {
		    	            view.getContext().startActivity(
		    	                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		    	            return true;
		    	        } else {
		    	            return false;
		    	        }
		    	    }
		    	});
				wv.loadUrl("http://www.libcompass.com/");
				av.loadUrl("http://www.libcompass.com/ads.htm");
			}
			return v;
		}
		
		@Override
	    public void onSaveInstanceState(Bundle outState) {
	        super.onSaveInstanceState(outState);
	        wv.saveState(outState);
	        av.saveState(outState);
	    }

	    @Override
	    public void onActivityCreated(Bundle savedInstanceState) {
	        super.onActivityCreated(savedInstanceState);
	        wv.restoreState(savedInstanceState);
	        av.restoreState(savedInstanceState);
	    }
	    
	    public void onPause() {
	        super.onPause();
	        webViewBundle = new Bundle();
	        wv.saveState(webViewBundle);
	        av.saveState(webViewBundle);
	    }
	    
	    @Override
	    public void onViewCreated(View view, Bundle savedInstanceState) {
	        super.onViewCreated(view, savedInstanceState);
	        // set timertask to reload ad webview every 60 seconds
	        WebView adview = (WebView) view.findViewById(R.id.adview);
	        new ReloadWebView(act, 60, adview);
	    }
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView myWebView = (WebView) findViewById(R.id.webview);
		// Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
    
    protected static class ReloadWebView extends TimerTask { //does what it says
        Activity context;
        Timer timer;
        WebView wv;

        public ReloadWebView(Activity context, int seconds, WebView wv) {
            this.context = context;
            this.wv = wv;

            timer = new Timer();
            timer.schedule(this,
                    seconds * 1000,  // initial delay
                    seconds * 1000); // subsequent rate
        }

        @Override
        public void run() {
            if(context == null || context.isFinishing()) {
                // Activity killed
                this.cancel();
                return;
            }

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    wv.reload();
                }
            });
        }
    }
}
