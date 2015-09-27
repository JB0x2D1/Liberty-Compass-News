package com.libcompass.androidapp;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.RemoteViews;

public class MyWidgetProvider extends AppWidgetProvider {

//DONE: Added clear background
//DONE: fix bug: update available when at latest version
//TODO: menu button to check for updates

	/* Display a TextView that retrieves a list of headlines and URLs 
	 * corresponding to those headlines cycles headlines.  Headlines are
	 * displayed in the TextView.  When the TextView is clicked, the next
	 * headline is displayed.  When the TextView is double clicked, the default
	 * browser is launched with a URL to the displayed headline. 
	 */
	private static final String ACTION_CLICK = "com.libcompass.androidapp.ACTION_CLICK";
	public static final String ACTION_UPDATE_COLOR = "com.libcompass.androidapp.ACTION_UPDATE_COLOR";
	private static final int NEW_VERSION = 0;
	private static String[] URLSarray = null, LINKSarray = null; //these hold the headlines and the URLs to the story
	private static long updated = 0;//milliseconds from 1/1/1970 to time widget was last updated
	private static int index = -1;//used to keep place in LINKSarray and URLSarray
	private static final int DOUBLE_CLICK_DELAY = 300;//time to wait for a second click in milliseconds
	private static int textColor = Color.BLACK;//default text color to display on widget
	private static int widgetBackground = R.drawable.bg_grey;//default widget background
	private static boolean dontIncrementIndex = false;//true if only changing color and not displaying next headline 
	protected static String latestVersion = "";
	protected static String APK_URL = "";
	private static NotificationManager mNotificationManager = null;
	//private static Context myContext;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//if local textColor or widget background are different from sharedpefs, sharedprefs value is saved locally and used
		textColor = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("textcolor", Color.BLACK);
		widgetBackground = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("widgetbg", R.drawable.bg_grey);
		index = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("index", 0);
		if (!atLatestVersion()) {
//			Toast.makeText(context, "Not at latest version", Toast.LENGTH_LONG).show();
			notifyNewVersion(context);
			latestVersion = "";
		}
		//get URLS and LINKS from sharedprefs
		LINKSarray = loadArray("LINKSarray", context);
		URLSarray = loadArray("URLSarray", context);
		if (URLSarray.length == 0) URLSarray = null;
		//set pending intent on widget textview to listen for clicks
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
	    Intent intent = new Intent(context, getClass());
	    intent.setAction(ACTION_CLICK);
	    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
	    views.setOnClickPendingIntent(R.id.update, pendingIntent);
	    
		// Get the data from the server if not present or > 1 hour elapsed
		Date now = new Date();
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
			if (updated == 0 || now.getTime() - updated > 3600000 || URLSarray == null) {
				String[] serverURLs = new String[3];
				serverURLs[0] = "http://www.libcompass.com/appdata/urls.txt";
				serverURLs[1] = "http://www.libcompass.com/appdata/links.txt";
				serverURLs[2] = "http://www.libcompass.com/appdata/version.txt";
				AsyncTask<String, Void, Void> task = new LongOperation().execute(serverURLs);
				try {
					task.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		} else {
			LINKSarray = new String[1]; 
			LINKSarray[0] = "No Network Connection.  Tap To Retry.";
		}
		//if only changing color index should be same as last update so decrement by one and reset flag
		if (dontIncrementIndex && index > 0) {
			--index;
			dontIncrementIndex = false;
		}
		//set widget background to user preference (or default)
		views.setInt(R.id.widget_linear_layout, "setBackgroundResource", widgetBackground);
		//write the headline to the widget TextView in the user chosen color
		views.setTextViewText(R.id.update, LINKSarray[index++]);
		views.setTextColor(R.id.update, textColor);
	    appWidgetManager.updateAppWidget(appWidgetIds, views);
	    context.getSharedPreferences("widget", 0).edit().putInt("clicks", 0).commit();
	    //correct for array out of bounds
	    if (index >= LINKSarray.length) index = 0;
	    context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("index", index).commit();
	}
	
	@SuppressLint("HandlerLeak") @Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(ACTION_CLICK)) { //user clicked on widget
	        int clickCount = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("clicks", 0);
	        context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks", ++clickCount).commit();

	        final Handler handler = new Handler() {
	            public void handleMessage(Message msg) {
	                int clickCount = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("clicks", 0);
	                if (clickCount > 1 && URLSarray != null) {
	                //Double Click - Launch default web browser with URL to story being displayed
	          		  	int ind;
	          		  	if (index == 0) ind = URLSarray.length - 1;
	          		  	else ind = index - 1;
	          		  	Intent newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URLSarray[ind]));
	          		  	newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	          		  	context.startActivity(newIntent);
	                }
	                else {
	                //Single Click - display next story in widget by calling widget onUpdate
	                	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	                	ComponentName thisAppWidget = new ComponentName(context.getPackageName(), MyWidgetProvider.class.getName());
	                	int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
	                	onUpdate(context, appWidgetManager, appWidgetIds);
	                }
	                context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks", 0).commit();
	            }
	        };
	        // wait to see if there is a second click coming
	        if (clickCount == 1) new Thread() {
	            @Override
	            public void run(){
	                try {
	                    synchronized(this) { wait(DOUBLE_CLICK_DELAY); }
	                    handler.sendEmptyMessage(0);
	                } catch(InterruptedException ex) {}
	            }
	        }.start();
	    } else if (intent.getAction().equals(ACTION_UPDATE_COLOR)) { //user picked color and/or background in configuration activity
			//save the new color and background internally and to shared preferences
			textColor = intent.getIntExtra("color", Color.BLACK);
			context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("textcolor", textColor).commit();
			widgetBackground = intent.getIntExtra("widgetbg", R.drawable.bg_grey);
			context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("widgetbg", widgetBackground).commit();
			//call onUpdate to redraw widget
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        	ComponentName thisAppWidget = new ComponentName(context.getPackageName(), MyWidgetProvider.class.getName());
        	int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        	dontIncrementIndex = true;
        	onUpdate(context, appWidgetManager, appWidgetIds);
		} else if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) { //update widget on network state change
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        	ComponentName thisAppWidget = new ComponentName(context.getPackageName(), MyWidgetProvider.class.getName());
        	int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        	if (context!=null && appWidgetManager != null && appWidgetIds != null) onUpdate(context, appWidgetManager, appWidgetIds);
		}
	    super.onReceive(context, intent);
	}
	
	//perform networking operations in an asynchronous task
	private class LongOperation  extends AsyncTask<String, Void, Void> {
		private final HttpClient Client = new DefaultHttpClient();
		private String Content;
		private String Error = null;
      
		protected void onPreExecute() {}

		protected Void doInBackground(String... urls) {
			HttpGet httpget;
			for (int c = 0;c < 3; c++) {
				try {
					// Server URL call by GET method
					httpget = new HttpGet(urls[c]);
					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					Content = Client.execute(httpget, responseHandler);
				} catch (ClientProtocolException e) {
					Error = e.getMessage();
					cancel(true);
				} catch (IOException e) {
					Error = e.getMessage();
					cancel(true);
				} //save the URLs and headlines internally
				if (c == 0) URLSarray = Content.split(System.getProperty("line.separator"));
				if (c == 1) LINKSarray = Content.split(System.getProperty("line.separator"));
				if (c == 2) {
					latestVersion = Content.split(System.getProperty("line.separator"))[0];
					APK_URL = Content.split(System.getProperty("line.separator"))[1];
				}
			}
			return null;
		}
       
		@SuppressWarnings("unused")
		protected void onPostExecute(Context context) {
			//if there was an error, display it in the widget TextView
			if (Error != null) {
				RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
				remoteViews.setTextViewText(R.id.update, Error);
			}
			if (saveArray(LINKSarray, "LINKSarray", context) && saveArray(URLSarray, "URLSarray", context))
				updated = new Date().getTime();
		}
	}
	public boolean saveArray(String[] array, String arrayName, Context mContext) { 
		SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(arrayName +"_size", array.length);
		for(int i=0;i<array.length;i++) editor.putString(arrayName + "_" + i, array[i]);
		return editor.commit();
	}
	public String[] loadArray(String arrayName, Context mContext) {
		SharedPreferences prefs = mContext.getSharedPreferences("preferencename", 0);
		int size = prefs.getInt(arrayName + "_size", 0);
		String array[] = new String[size];
		for(int i=0;i<size;i++) array[i] = prefs.getString(arrayName + "_" + i, null);
		return array;
	}
	
	private static void notifyNewVersion(Context context) {
		Notification.Builder mBuilder =
		        new Notification.Builder(context)
				.setAutoCancel(true)
		        .setSmallIcon(R.drawable.ic_compass)
		        .setContentTitle("Liberty Compass News")
		        .setContentText("New Version "+latestVersion+" Available for Download.");
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(context, Update.class);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(Update.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		mNotificationManager =
		    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(NEW_VERSION, mBuilder.build());
	}
	protected static void dismissNotification() {
		if (mNotificationManager != null) mNotificationManager.cancel(NEW_VERSION);
	}
	
	private static boolean atLatestVersion() {
		if (latestVersion == "") return true;
		return Integer.valueOf((latestVersion.split("\\.")[0]+latestVersion.split("\\.")[1]+latestVersion.split("\\.")[2])) <= 
		Integer.valueOf(MainActivity.version.split("\\.")[0]+MainActivity.version.split("\\.")[1]+MainActivity.version.split("\\.")[2]);
	}
	
	protected static boolean newVersionAvailable (Context context) {
		if (atLatestVersion()) return false;
		notifyNewVersion(context);
		latestVersion = "";
		return true;
	}
}