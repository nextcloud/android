/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.errorhandling;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
	private final Activity mContext;

	private static final String TAG = ExceptionHandler.class.getSimpleName();

	public ExceptionHandler(Activity context) {
		mContext = context;
	}

	public void uncaughtException(Thread thread, Throwable exception) {
	    Log.e(TAG, "ExceptionHandler caught UncaughtException", exception);
		StringWriter stackTrace = new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));
		final StringBuilder errorReport = new StringBuilder(192);
		final String LINE_SEPARATOR = "\n";
		errorReport.append("************ CAUSE OF ERROR ************\n\n")
				.append(stackTrace.toString())
				.append("\n************ DEVICE INFORMATION ***********\nBrand: ")
				.append(Build.BRAND)
				.append(LINE_SEPARATOR)
				.append("Device: ")
				.append(Build.DEVICE)
				.append(LINE_SEPARATOR)
				.append("Model: ")
				.append(Build.MODEL)
				.append(LINE_SEPARATOR)
				.append("Id: ")
				.append(Build.ID)
				.append(LINE_SEPARATOR)
				.append("Product: ")
				.append(Build.PRODUCT)
				.append(LINE_SEPARATOR)
				.append("\n************ FIRMWARE ************\nSDK: ")
				.append(Build.VERSION.SDK_INT)
				.append(LINE_SEPARATOR)
				.append("Release: ")
				.append(Build.VERSION.RELEASE)
				.append(LINE_SEPARATOR)
				.append("Incremental: ")
				.append(Build.VERSION.INCREMENTAL)
				.append(LINE_SEPARATOR);

		Log.e(TAG, "An exception was thrown and handled by ExceptionHandler:", exception);

		Intent intent = new Intent(mContext, ErrorShowActivity.class);
		intent.putExtra("error", errorReport.toString());
		mContext.startActivity(intent);

		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(1000);
	}

}