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

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
	private final Activity mContext;
	private final String LINE_SEPARATOR = "\n";

	private static final String TAG = ExceptionHandler.class.getSimpleName();

	public ExceptionHandler(Activity context) {
		mContext = context;
	}

	public void uncaughtException(Thread thread, Throwable exception) {
	    Log.e(TAG, "ExceptionHandler caught UncaughtException", exception);
		StringWriter stackTrace = new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));
		StringBuilder errorReport = new StringBuilder();
		errorReport.append("************ CAUSE OF ERROR ************\n\n");
		errorReport.append(stackTrace.toString());

		errorReport.append("\n************ DEVICE INFORMATION ***********\n");
		errorReport.append("Brand: ");
		errorReport.append(Build.BRAND);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("Device: ");
		errorReport.append(Build.DEVICE);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("Model: ");
		errorReport.append(Build.MODEL);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("Id: ");
		errorReport.append(Build.ID);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("Product: ");
		errorReport.append(Build.PRODUCT);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("\n************ FIRMWARE ************\n");
		errorReport.append("SDK: ");
		errorReport.append(Build.VERSION.SDK_INT);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("Release: ");
		errorReport.append(Build.VERSION.RELEASE);
		errorReport.append(LINE_SEPARATOR);
		errorReport.append("Incremental: ");
		errorReport.append(Build.VERSION.INCREMENTAL);
		errorReport.append(LINE_SEPARATOR);

		Log.e(TAG, "An exception was thrown and handled by ExceptionHandler:", exception);

		Intent intent = new Intent(mContext, ErrorShowActivity.class);
		intent.putExtra("error", errorReport.toString());
		mContext.startActivity(intent);

		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(1000);
	}

}