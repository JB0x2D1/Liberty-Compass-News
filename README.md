# Liberty-Compass-News
Android app and widget companion to Liberty Compass News (www.LibCompass.com)

MainActivity: open Liberty Compass News (www.LibCompass.com) in large webview and ads in small webview

MyWidgetProvider: display headlines from Liberty Compass in a textview.  Clicking once on widget displays next 
  headline.  Douple clicking on widget opens currently displayed headline (full story) in new default browser 
  window.  Clicking left side of widget displays previous headline.  Handles download from server of headlines, URLs, 
  and latest app version.  Initiates system notification if a new app version is available.
  
WidgetConfig: display use instructions, allow user selection of background color (from spinner) and text color (using 
  red, green, and blue slider bars or an edittext field for RGB hex value).  Sliders update edittext and vice versa.
  A preview of how the chosen colors will look is displayed.
  
DownloadUpdate: enqueue the latest version of the .apk with the system download manager

FetchData: download updated headlines, URLs to news stories, and latest app version information from the server in an Asynchronous Task
