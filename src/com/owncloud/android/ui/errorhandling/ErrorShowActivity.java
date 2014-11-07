package com.owncloud.android.ui.errorhandling;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.owncloud.android.R;

public class ErrorShowActivity extends Activity {

	private static final String TAG = ErrorShowActivity.class.getName();

	TextView error;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "ErrorShowActivity was called. See above for StackTrace.");
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		setContentView(R.layout.errorhandling_showerror);
		error = (TextView) findViewById(R.id.errorTextView);
		error.setText(getIntent().getStringExtra("error"));

	}
}