package com.libcompass.androidapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.libcompass.androidapp.MainActivity.OnTaskCompleted;

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
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MyWidgetProvider extends AppWidgetProvider {

	/* Display a TextView that retrieves a list of headlines.  
	 * Maintain URLs corresponding to those headlines.  Headlines are
	 * displayed in the TextView.  When the TextView is clicked, the next
	 * headline is displayed.  When the TextView is double clicked, the default
	 * browser is launched with a URL to the displayed headline. 
	 */
	
	//TODO: is it possible to only update the new widget from the configuration activity?
	
	//DONE: save individual widget preferences in sharedprefs as in "<widget-id>bgcolor" "bg_black"
	//DONE: 	background, text color, index position

	//DONE: in ondeleted delete the sharedprefs of the individual deleted widget(s)
	
	//DONE: in ondisabled (the last widget has been removed) delete any/all sharedprefs (globals)
	//DONE: 	updated, updateFrequency, URLSarray, LINKSarray, etc

	//DONE: append widgetid to pending click intent actions

	//DONE: extract widget id in onreceive in order to call appWidgetManager.updateAppWidget(widgetid, remoteviews)
	
	//DONE: add time updated to widget view
	//DONE: don't do anything in onUpdate if device screen is off, listen for
	//	lock screen unlock and call onUpdate at that time. Goal: save battery & data.
	//DONE: added timeout on server downloads by adding method downloadAndSave
	
	//CONSTANTS
	private static final String ACTION_BACK = "com.libcompass.androidapp.ACTION_BACK";
	private static final String ACTION_CLICK = "com.libcompass.androidapp.ACTION_CLICK";
	private static final String ACTION_DOWNLOAD = "com.libcompass.androidapp.ACTION_DOWNLOAD";
	private static final String ACTION_UPDATE_ALL = "com.libcompass.androidapp.ACTION_UPDATE_ALL";
	protected static final String ACTION_UPDATE_COLOR = "com.libcompass.androidapp.ACTION_UPDATE_COLOR";
	protected static final String ACTION_NEXT_HEADLINE = "com.libcompass.androidapp.ACTION_MY_UPDATE";
	private static final int NEW_VERSION_NOTIFICATION = 8675309;
	
	private static RemoteViews views = null; //declared here so AsyncTask can access it
	private static String[] URLSarray = null, LINKSarray = null; //these hold the headlines of, and the URLs to, the news stories
	private static long updated = 0;//milliseconds from 1/1/1970 to time widget was last updated
	private static int index = 0;//used to keep place in LINKSarray and URLSarray
	private static final int DOUBLE_CLICK_DELAY = 200;//time to wait for a second click in milliseconds
	private static int textColor = Color.BLACK;//default text color to display on widget
	private static int widgetBackground = R.drawable.bg_grey;//default widget background
	protected static String latestVersion = "";//latest version of app according to the server
	protected static String APK_URL = "";//URL of latest version of app .apk according to the server
	private static NotificationManager mNotificationManager = null;//declared here so notification methods can access it
	protected static long updateFrequency = 14400000;//how often the server is queried for new data in milliseconds
	protected static String[] serverURLs = {"http://www.libcompass.com/appdata/urls.txt", "http://www.libcompass.com/appdata/links.txt", 
		"http://www.libcompass.com/appdata/version.txt"}; //URLs of data to be fetched from server: URLSarray, LINKSarray, latestVersion
	private static String downloadSaveError = null;
	private static int widgetId;
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int id : appWidgetIds) {
			SharedPreferences prefs = context.getSharedPreferences("widget", 0);
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove("index"+String.valueOf(id)).commit();
			editor.remove("textcolor"+String.valueOf(id)).commit();
			editor.remove("widgetbg"+String.valueOf(id)).commit();
		}
	}
	
	@Override
	public void onDisabled(Context context) {
		SharedPreferences prefs = context.getSharedPreferences("widget", 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.clear().commit();
	}

	/*
	 * 		onUpdate
	 */

	@Override
	public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int id : appWidgetIds) {
			updateWidget(context, id);
		}
	}//onUpdate
	
	/*************************************************\
	 * 		onReceive
	\*************************************************/
	
	@SuppressLint("HandlerLeak") @Override
	public void onReceive(final Context context, final Intent intent) {
	    super.onReceive(context, intent);//pass the broadcast up the heirarchy
		//if device is sleeping, do nothing
		if(!((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn()) return;
	    String action = intent.getAction();
    	final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
// change in connectivity state, device unlocked, regularly scheduled system widget update call or ACTION_UPDATE_ALL
	    // cycle all widgets' headlines by one
	    if ((action.equals(ConnectivityManager.CONNECTIVITY_ACTION) && MainActivity.networkAvailable(context)) ||
	    		action.equals(Intent.ACTION_USER_PRESENT) ||
	    		action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) ||
	    		action.equals(ACTION_UPDATE_ALL)) {
	    	ComponentName widgetComponent = new ComponentName(context, MyWidgetProvider.class);
			int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
			for (int id : widgetIds) {
				updateWidget(context, id);
			}
	    } else
// user click
	    // cycle the clicked widget's headline by one
		if (action.startsWith(ACTION_NEXT_HEADLINE)) {
			widgetId = Integer.parseInt(action.substring(ACTION_NEXT_HEADLINE.length()));
			updateWidget(context, widgetId);
		} else
// right side of widget was clicked
		// query server for new headlines 
		if (action.startsWith(ACTION_DOWNLOAD)) {
			widgetId = Integer.parseInt(action.substring(ACTION_DOWNLOAD.length()));
			//use saved value of updated
			updated = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getLong("updated", 0);
			//don't allow server downloads more frequently than every 10 minutes
			Date now = new Date();
			long interval = now.getTime() - updated;//interval == length of time since last update (milliseconds)
			if (interval > 600000) {//600000 == 10 minutes
				if (!MainActivity.networkAvailable(context)) {
					Toast.makeText(context, "No network available.", Toast.LENGTH_LONG).show();
					return;
				}
	        	Toast.makeText(context, "Downloading news from server.", Toast.LENGTH_LONG).show();
	        	downloadAndSave(context, true, new OnTaskCompleted() {
					@Override
					public void onTaskCompleted() {
						Intent intent = new Intent(context, MyWidgetProvider.class);
					    intent.setAction(ACTION_NEXT_HEADLINE+String.valueOf(widgetId));
					    context.sendBroadcast(intent);
					}
	        	});
	        } else { //less than 10 minutes since last download
				interval = 600000 - interval;
				interval /= 1000;
				int sec = (int) interval % 60;
				interval /= 60;
				int min = (int) interval;
				Toast.makeText(context, "Just Downloaded News.  Try again in "+String.valueOf(min)+" min "+String.valueOf(sec)+" sec.", Toast.LENGTH_LONG).show();
			}
		} else
// left side of widget was clicked
	    // adjust index to display previous story
		if (action.startsWith(ACTION_BACK)) {
			widgetId = Integer.parseInt(action.substring(ACTION_BACK.length()));
			index = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("index" + String.valueOf(widgetId), 0);
			//0 index means index arrayLength - 1 is displaying. set index to arrayLength - 2
			if (index == 0) index = LINKSarray.length - 2;
			//1 index means index 0 is displaying. set index to arrayLength - 1
			else if (index == 1) index = LINKSarray.length - 1;
			//in all other cases, decrement by 2
			else index -= 2;
			//save index to sharedprefs
			context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("index" + String.valueOf(widgetId), index).commit();
			//update the widget
			updateWidget(context, widgetId);
		} else
//user clicked on widget
		if (action.startsWith(ACTION_CLICK)) {
			widgetId = Integer.parseInt(action.substring(ACTION_CLICK.length()));
			//get current click count
	        int clickCount = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("clicks"+String.valueOf(widgetId), 0);
	        //increment and save current click count
	        context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks"+String.valueOf(widgetId), ++clickCount).commit();
	        final Handler handler = new Handler() {
	            public void handleMessage(Message msg) {
	//get values from sharedprefs and don't move on until it is done
	        		Thread t = new Thread() {
	        			public void run() {
	        				try { loadEverything(context, widgetId); }
	        				catch (Exception e) { e.printStackTrace(); }
	        			}
	        		};
	        		t.start();
	        		try {
	        			t.join();
	        		} catch (InterruptedException e1) {
	        			e1.printStackTrace();
	        		}
	                int clickCount = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("clicks"+String.valueOf(widgetId), 0);
	                if (clickCount > 1 && URLSarray != null) {
	         //Double Click - Launch default web browser with URL to story being displayed
	                	if (!MainActivity.networkAvailable(context)) { //don't try to launch browser if no network
	                		Toast.makeText(context, "No network available.", Toast.LENGTH_LONG).show();
	                		//reset saved click count to 0
	    	                context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks"+String.valueOf(widgetId), 0).commit();
	                		return;
	                	} else {
		                	//index is incremented after widget is updated so open URL to story being displayed (which is not current value of index)
		          		  	int ind;
		          		  	if (index == 0) ind = URLSarray.length - 1;
		          		  	else ind = index - 1;
		          		  	//create and fire the intent to open the URL in a new default web browser
		          		  	Intent newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URLSarray[ind]));
		          		  	newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		          		  	context.startActivity(newIntent);
	                	}
	                }
	                else {
	        //Single Click - display next story in widget by calling widget onUpdate
	                	updateWidget(context, widgetId);
	                }//reset saved click count to 0
	                context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("clicks"+String.valueOf(widgetId), 0).commit();
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
	    } else
//save user-selected options from configuration activity
    	if (action.startsWith(ACTION_UPDATE_COLOR)) {
    		widgetId = Integer.parseInt(action.substring(ACTION_UPDATE_COLOR.length()));
			textColor = intent.getIntExtra("color", Color.BLACK);
			context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("textcolor"+String.valueOf(widgetId), textColor).commit();
			widgetBackground = intent.getIntExtra("widgetbg", R.drawable.bg_grey);
			context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("widgetbg"+String.valueOf(widgetId), widgetBackground).commit();
			updateFrequency = intent.getLongExtra("updatefrequency", 14400000);
			context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putLong("updatefrequency", updateFrequency).commit();
//			views = updateWidget(context, widgetId);
//			appWidgetManager.updateAppWidget(widgetId, views);
		} 
	}//onReceive
	
	//save string array to sharedprefs
	public static boolean saveArray(String[] array, String arrayName, Context mContext) { 
		SharedPreferences prefs = mContext.getSharedPreferences("widget", 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(arrayName +"_size", array.length);
		for(int i=0;i<array.length;i++) editor.putString(arrayName + "_" + i, array[i]);
		return editor.commit();
	}
	//load string array from sharedprefs
	public String[] loadArray(String arrayName, Context mContext) {
		SharedPreferences prefs = mContext.getSharedPreferences("widget", 0);
		int size = prefs.getInt(arrayName + "_size", 0);
		String[] array = new String[size];
		for(int i=0;i<size;i++) array[i] = prefs.getString(arrayName + "_" + i, null);
		if (array.length == 0) return null;
		else return array;
	}
	//send system notification if a new version is available for download
	private static void notifyNewVersion(Context context) {
		Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Notification.Builder mBuilder =
		        new Notification.Builder(context)
				.setAutoCancel(true)
		        .setSmallIcon(R.drawable.ic_compass)
		        .setSound(uri)
		        .setContentTitle("Liberty Compass News")
		        .setContentText("New Version "+latestVersion+" Available for Download.");
		// Creates an explicit intent for Update Activity
		Intent resultIntent = new Intent(context, DownloadUpdate.class);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(DownloadUpdate.class);
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
		// mId (NEW_VERSION) allows you to update the notification later on.
		mNotificationManager.notify(NEW_VERSION_NOTIFICATION, mBuilder.build());
	}
	protected static void dismissNotification() {
		if (mNotificationManager != null) mNotificationManager.cancel(NEW_VERSION_NOTIFICATION);
	}
	//only check if data downloaded from server (if available) indicates that this app is up to date
	private static boolean atLatestVersion() {
		if (latestVersion == "") return true;
		return Integer.valueOf((latestVersion.split("\\.")[0]+latestVersion.split("\\.")[1]+latestVersion.split("\\.")[2])) <= 
		Integer.valueOf(MainActivity.version.split("\\.")[0]+MainActivity.version.split("\\.")[1]+MainActivity.version.split("\\.")[2]);
	}
	//check if new version is available AND send notification if a new version is available
	protected static boolean newVersionAvailable (Context context) {
		if (atLatestVersion()) return false;
		notifyNewVersion(context);
		latestVersion = "";
		context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putString("latestVersion", latestVersion).commit();
		return true;
	}
	private static void saveEverything(Context context) {
		//save the values that change often.  
		//textColor, widgetBackground, and updateFrequency are omitted
		Date now = new Date();
		context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putLong("index"+String.valueOf(widgetId), index).commit();
		context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putLong("updated", now.getTime()).commit();
		context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putString("latestVersion", latestVersion).commit();
		if (URLSarray != null) saveArray(URLSarray, "URLSarray", context);
		if (LINKSarray != null) saveArray(LINKSarray, "LINKSarray", context);
	}
	private void loadEverything(Context context, int widgetId) {
		//load all saved values from sharedprefs
		//widgetId specific values
		index = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("index"+String.valueOf(widgetId), (int)0);
		textColor = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("textcolor"+String.valueOf(widgetId), Color.BLACK);
		widgetBackground = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getInt("widgetbg"+String.valueOf(widgetId), R.drawable.bg_grey);
		//widgetId agnostic values 
		updateFrequency = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getLong("updateFrequency", (long)14400000);
		updated = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getLong("updated", (long)0);
		latestVersion = context.getSharedPreferences("widget", Context.MODE_PRIVATE).getString("latestVersion", "");
		LINKSarray = loadArray("LINKSarray", context);
		URLSarray = loadArray("URLSarray", context);
	}
	protected static void downloadAndSave(Context context, boolean toast) {
		downloadAndSave(context, toast, null);
	}
	protected static void downloadAndSave(final Context context, final boolean toast, final OnTaskCompleted otc) {
		/* Download data from server silently if toast == false,
		 * or with a success/error Toast if toast == true.
		 * Spins off a new thread to wait 30 seconds for the download operation 
		 * to complete.
		 * 
		 * When the download operation is complete, a thread is created and
		 * joined to save everything to sharedprefs.  Once this operation
		 * is completed, the OnTaskCompleted passed to downloadAndSave
		 * is called, if not null.
		 */
		downloadSaveError = null; //initialize error String to null

		// OnTaskCompleted method to be executed following download oepration
		final OnTaskCompleted downloadComplete = new OnTaskCompleted() {
			public void onTaskCompleted() {
				if(downloadSaveError == null) {
					Toast.makeText(context, "Download Successful", Toast.LENGTH_LONG).show();
					updated = new Date().getTime();
				}
				else Toast.makeText(context, "Download Error: " + downloadSaveError, Toast.LENGTH_LONG).show();
				Thread t = new Thread() {
		    		public void run() {
		    			if (downloadSaveError == null) {
							try { saveEverything(context); }
							catch (Exception e) {
								downloadSaveError = e.getMessage();
								e.printStackTrace();
							}
						}
		    		}
				};
				t.start();
				// not worried about this taking too long so join() the thread
				try { t.join(); }
				catch (InterruptedException e) { e.printStackTrace(); }
				// call OnTaskCompleted passed to downloadAndSave, if not null
				if (otc != null) otc.onTaskCompleted();
			}
		};

		// Thread that waits up to 30 seconds for the download to complete
		new Thread() {
    		public void run() {
				AsyncTask<String, Void, String> task = new FetchData(downloadComplete).execute(serverURLs);
				try {
					downloadSaveError = task.get(30, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					downloadSaveError = e.getMessage();
					e.printStackTrace();
				} catch (ExecutionException e) {
					downloadSaveError = e.getMessage();
					e.printStackTrace();
				} catch (TimeoutException e) {
					downloadSaveError = e.getMessage();
					e.printStackTrace();
				}
    		}
    	}.start();
	}
	
	protected static void setDownloadedValues(String[] URLS, String[] LINKS, String latest, String APK) {
		URLSarray = URLS;
		LINKSarray = LINKS;
		latestVersion = latest;
		APK_URL = APK;
	}
	
	//return a RemoteViews representing the widget corresponding to widgetId
	private void updateWidget(final Context context, final int widgetId) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
//get values from sharedprefs and don't move on until it is done
		Thread t = new Thread() {
			public void run() {
				try { loadEverything(context, widgetId); }
				catch (Exception e) { e.printStackTrace(); }
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

// Get the data from the server if not present or > updateFrequency elapsed
		Date now = new Date();//to determine time since last download
		boolean gotConnection = MainActivity.networkAvailable(context);
		if (gotConnection) {//don't call the server unless there is network connection
			if ((now.getTime() - updated) > updateFrequency || URLSarray == null) {
				downloadAndSave(context, false);
			}
		}
		// if we have connectivity and URLSarray or LINKSarray are null then  
		// assume download is not complete. wait up to 4 seconds for download to complete
		if ((gotConnection && URLSarray == null) || (gotConnection && LINKSarray == null)) {
			int elapsedTime = 0;
			int timeToWait = 4000;
			while (elapsedTime < timeToWait) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (URLSarray != null && LINKSarray != null) timeToWait = -1;//download is complete, stop waiting
				else elapsedTime += 100;//still waiting, update elapsedTime
			}
		}
		//if URLS array is still null after waiting, assume a connection problem, display message in widget
		if (URLSarray == null || LINKSarray == null) {
			LINKSarray = new String[1]; 
			LINKSarray[0] = "No Network Connection.  Tap To Retry.";
			URLSarray = new String[1];
			URLSarray[0] = "file:///android_asset/nonetwork.htm";
			updated = 0; //so next iteration will try to download data
			//save updated to sharedprefs
			context.getSharedPreferences("widget", 0).edit().putInt("updated", 0).commit();
		}
		// check if this version is current based on latest server download
		newVersionAvailable(context);
		
//set onclick listeners

		//set pending intent on widget textview to capture clicks
		views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
	    Intent intent = new Intent(context, getClass());
	    intent.setAction(ACTION_CLICK + String.valueOf(widgetId));
	    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
	    views.setOnClickPendingIntent(R.id.update, pendingIntent);
	    //set pending intent on clear imageview to capture clicks on left side of widget
	    Intent backIntent = new Intent(context, getClass());
	    backIntent.setAction(ACTION_BACK + String.valueOf(widgetId));
	    PendingIntent pendingBackIntent = PendingIntent.getBroadcast(context, 0, backIntent, 0);
	    views.setOnClickPendingIntent(R.id.clear, pendingBackIntent);
	    //set pending intent on clear imageview to capture clicks on right side of widget
	    Intent updateIntent = new Intent(context, getClass());
	    updateIntent.setAction(ACTION_DOWNLOAD + String.valueOf(widgetId));
	    PendingIntent pendingUpdateIntent = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
	    views.setOnClickPendingIntent(R.id.updatedata, pendingUpdateIntent);
		//check for array index out of bounds before using LINKSarray
		if (index >= LINKSarray.length - 1) index = 0;
// Draw the view
		//set widget background to user preference (or default)
		views.setInt(R.id.widget_linear_layout, "setBackgroundResource", widgetBackground);
		views.setTextColor(R.id.update, textColor);
		//write the headline to the widget TextView in the user chosen color
		views.setTextViewText(R.id.update, LINKSarray[index++]);
		//write the time of last download to the updatedtime textview
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
		sdf.setTimeZone(TimeZone.getDefault());
		views.setTextViewText(R.id.updatetime, (updated != 0) ? sdf.format(new Date(updated)) : "Updating");
	    //reset click count to 0
	    context.getSharedPreferences("widget", 0).edit().putInt("clicks", 0).commit();
	    //correct for array out of bounds after incrementing
	    if (index >= LINKSarray.length) index = 0;
	    // save position in headlines array to sharedprefs
	    context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("index" + String.valueOf(widgetId), index).commit();

	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	    appWidgetManager.updateAppWidget(widgetId, views);
	}
}