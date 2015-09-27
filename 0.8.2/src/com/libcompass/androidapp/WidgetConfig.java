package com.libcompass.androidapp;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class WidgetConfig extends Activity {
	
	/* Choose background of widget and text color using slider bars, spinner,
	 * and edittext */
	
	private int mAppWidgetId = 0 ;
	private int textColor = Color.BLACK;
	private int widgetBg = R.drawable.bg_grey;
	 
    //initialize the view
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_layout); //so findViewById works
		final ImageView previewImgView = (ImageView) findViewById(R.id.previewbg);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.bg_colors_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(1);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				int[] arr = {R.drawable.bg_black, R.drawable.bg_grey, R.drawable.bg_white};
				previewImgView.setImageResource(arr[pos]);
				widgetBg = (arr[pos]);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
        final EditText editText = (EditText) findViewById(R.id.widget_text_color); //hex representation of text color
        editText.setText("000000"); 
        editText.setSelection(0, 6); //select it
        editText.setFocusableInTouchMode(true); //make it focusable
        final TextView textView = (TextView) findViewById(R.id.previewtext); //to show user how their chosen text color will look
        textView.setTextColor(Color.BLACK); //default is black
        final SeekBar redsb = (SeekBar) findViewById(R.id.seek1); //the red slider bar
        final SeekBar greensb = (SeekBar) findViewById(R.id.seek2); //the green slider bar
        final SeekBar bluesb = (SeekBar) findViewById(R.id.seek3); //the blue slider bar
        /* set listeners on the slider bars and update the preview text with 
         * the new color in real time as the user changes the sliders */
        //to prevent feedback loop, set flags to ignore events
        final boolean[] ignorered = {false};
		final boolean[] ignoregreen = {false};
		final boolean[] ignoreblue = {false};
		final boolean[] ignoreEdit = {false};
        redsb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if (!ignorered[0]) {
					ignoreEdit[0] = true;
					editText.setText(Convert.decToHex(redsb.getProgress()) + Convert.decToHex(greensb.getProgress()) + Convert.decToHex(bluesb.getProgress()));
					textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
				}
				else ignorered[0] = false;
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				if (!ignorered[0]) {
					ignoreEdit[0] = true;
					editText.setText(Convert.decToHex(redsb.getProgress()) + Convert.decToHex(greensb.getProgress()) + Convert.decToHex(bluesb.getProgress()));
					textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
				}
				else ignorered[0] = false;
			}
        });
        greensb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if (!ignoregreen[0]) {
					ignoreEdit[0] = true;
					editText.setText(Convert.decToHex(redsb.getProgress()) + Convert.decToHex(greensb.getProgress()) + Convert.decToHex(bluesb.getProgress()));
					textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
				}
				else ignoregreen[0] = false;
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				if (!ignoregreen[0]) {
					ignoreEdit[0] = true;
					editText.setText(Convert.decToHex(redsb.getProgress()) + Convert.decToHex(greensb.getProgress()) + Convert.decToHex(bluesb.getProgress()));
					textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
				}
				else ignoregreen[0] = false;
			}
        });
        bluesb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if (!ignoreblue[0]) {
					ignoreEdit[0] = true;
					editText.setText(Convert.decToHex(redsb.getProgress()) + Convert.decToHex(greensb.getProgress()) + Convert.decToHex(bluesb.getProgress()));
					textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
				}
				else ignoreblue[0] = false;
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				if (!ignoreblue[0]) {
					ignoreEdit[0] = true;
					editText.setText(Convert.decToHex(redsb.getProgress()) + Convert.decToHex(greensb.getProgress()) + Convert.decToHex(bluesb.getProgress()));
					textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
				}
				else ignoreblue[0] = false;
			}
        });
        /* set a listener on the EditText field containing the hex 
         * representation of the text color.  Update sliders to match typed
         * values if they are valid hex */
        editText.addTextChangedListener(new TextWatcher(){
        	@Override
			public void afterTextChanged(Editable s) {
        		if (ignoreEdit[0]) ignoreEdit[0] = false;
        		else {
					boolean redok = true, greenok = true, blueok = true;
					if (s.toString().length() >= 2) {
						int red = 0;
						try {
							red =  Integer.decode("#" + s.toString().substring(0, 2));
						} catch (NumberFormatException e) {
							redok = false;
						}
						if (redok && 0 <= red && red < 256) {
							ignorered[0] = true;
							redsb.setProgress(red);
						}
					}
					if (s.toString().length() >= 4) {
						int green = 0;
						try {
							green = Integer.decode("#" + s.toString().substring(2, 4));
						} catch (NumberFormatException e) {
							greenok = false;
						}
						if (greenok && 0 <= green && green < 256) {
							ignoregreen[0] = true;
							greensb.setProgress(green);
						}
					}
					if (s.toString().length() == 6) {
						int blue = 0;
						try {
							blue = Integer.decode("#" + s.toString().substring(4, 6));
						} catch (NumberFormatException e) {
							blueok = false;
						}
						if (blueok && 0 <= blue && blue < 256) {
							ignoreblue[0] = true;
							bluesb.setProgress(blue);
						}
					} else blueok = false;
					if (redok && greenok && blueok) textView.setTextColor(Color.parseColor("#" + editText.getText().toString()));
        		}
            }
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
        });
        /* set listener on the OK button to update widget and finish 
         * configuration activity when pressed */
        Button okButton = (Button) findViewById(R.id.btn_ok);
        OnClickListener okButtonClicked = new OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		boolean ok = true;
        		int c = 0;
        		try {
        			c = Color.parseColor("#" + editText.getText().toString());
        		} catch (IllegalArgumentException e) {
        			Toast.makeText(view.getContext(), "Invalid Color", Toast.LENGTH_SHORT).show();
        			ok = false;
        		}
        		if (ok) {
        			textColor = c;
        			sendFirstUpdate();
        		}
        	}
        };
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        okButton.setOnClickListener(okButtonClicked);
    }

    private void sendFirstUpdate() { //does what it says
    	final Context context = WidgetConfig.this;
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    	ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
    			WidgetConfig.class.getName());
    	Intent firstUpdate = new Intent(context, MyWidgetProvider.class);
    	int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
    	//Set widget color and background
    	Intent setColorIntent = new Intent(context, MyWidgetProvider.class);
    	setColorIntent.setAction(MyWidgetProvider.ACTION_UPDATE_COLOR);
    	setColorIntent.putExtra("color", textColor);
    	setColorIntent.putExtra("widgetbg", widgetBg);
    	setColorIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    	context.sendBroadcast(setColorIntent);
    	//call widget onUpdate and finish this activity 
    	firstUpdate.setAction("android.appwidget.action.APPWIDGET_UPDATE");
    	firstUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
    	context.sendBroadcast(firstUpdate);
    	Intent resultValue = new Intent();  
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);  
        setResult(RESULT_OK, resultValue);  
        finish();
    }
    private static class Convert { //converts decimal 0-255 to hex 00-FF
    	  private static final int sizeOfIntInHalfBytes = 2;
    	  private static final int numberOfBitsInAHalfByte = 4;
    	  private static final int halfByte = 0x0F;
    	  private static final char[] hexDigits = { 
    	    '0', '1', '2', '3', '4', '5', '6', '7', 
    	    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    	  };

    	  public static String decToHex(int dec) {
    	    StringBuilder hexBuilder = new StringBuilder(sizeOfIntInHalfBytes);
    	    hexBuilder.setLength(sizeOfIntInHalfBytes);
    	    for (int i = sizeOfIntInHalfBytes - 1; i >= 0; --i)
    	    {
    	      int j = dec & halfByte;
    	      hexBuilder.setCharAt(i, hexDigits[j]);
    	      dec >>= numberOfBitsInAHalfByte;
    	    }
    	    return hexBuilder.toString(); 
    	  }
    }
}