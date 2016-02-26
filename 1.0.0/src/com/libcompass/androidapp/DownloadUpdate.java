package com.libcompass.androidapp;

import android.app.Activity;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

public class DownloadUpdate extends Activity {

	/* enqueue new version of app for download in download manager, send a toast
	 * that download has started, dismiss notification. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(MyWidgetProvider.APK_URL));
		// This puts the download in the same Download dir the browser uses
	    String filename = "LibertyCompassNews.apk";
	    r.setDescription(filename);
		r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
		// Notify user when download is completed
		r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		// Start download
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		dm.enqueue(r);
		Toast.makeText(this, "Download in progress.  See notification area for progress.",
				   Toast.LENGTH_LONG).show();
		MyWidgetProvider.dismissNotification();
		finish();
	}

}
