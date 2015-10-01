package com.libcompass.androidapp;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

import com.libcompass.androidapp.MainActivity.OnTaskCompleted;

//perform networking operations in an asynchronous task
public class FetchData  extends AsyncTask<String, Void, String> {
	private final HttpClient Client = new DefaultHttpClient();
	private String Content;
	private String Error = null;
	public OnTaskCompleted otc = null;
	private String[] URLSarray, LINKSarray;
	private String latestVersion, APK_URL;

	public FetchData(OnTaskCompleted otc) {
		this.otc = otc;
	}
	
	public FetchData() {}
	
	protected void onPreExecute() {}

	protected String doInBackground(String... urls) {
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
			} //save the URLs and headlines
			if (Error == null) {
				if (c == 0) URLSarray = Content.split(System.getProperty("line.separator"));
				if (c == 1) LINKSarray = Content.split(System.getProperty("line.separator"));
				if (c == 2) {
					latestVersion = Content.split(System.getProperty("line.separator"))[0];
					APK_URL = Content.split(System.getProperty("line.separator"))[1];
				}
			}
		}
		return Error;
	}
	@Override
	protected void onPostExecute(String result) {
		MyWidgetProvider.setDownloadedValues(URLSarray, LINKSarray, latestVersion, APK_URL);
		if (otc != null) otc.onTaskCompleted();
	}
}

