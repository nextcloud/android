/*
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

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = ExceptionHandler.class.getSimpleName();
    private static final String LINE_SEPARATOR = "\n";

    private final Activity context;

    public ExceptionHandler(Activity context) {
        this.context = context;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        Log.e(TAG, "ExceptionHandler caught UncaughtException", exception);
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        String errorReport = generateErrorReport(stackTrace.toString());

        Log.e(TAG, "An exception was thrown and handled by ExceptionHandler:", exception);

        Intent intent = new Intent(context, ShowErrorActivity.class);
        intent.putExtra(ShowErrorActivity.EXTRA_ERROR_TEXT, errorReport);
        context.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1000);
    }

    @NotNull
    private String generateErrorReport(String stackTrace) {
        return "************ CAUSE OF ERROR ************\n\n" +
            stackTrace +
            "\n************ DEVICE INFORMATION ***********" +
            LINE_SEPARATOR +
            "Brand: " +
            Build.BRAND +
            LINE_SEPARATOR +
            "Device: " +
            Build.DEVICE +
            LINE_SEPARATOR +
            "Model: " +
            Build.MODEL +
            LINE_SEPARATOR +
            "Id: " +
            Build.ID +
            LINE_SEPARATOR +
            "Product: " +
            Build.PRODUCT +
            LINE_SEPARATOR +
            "\n************ FIRMWARE ************" +
            LINE_SEPARATOR +
            "SDK: " +
            Build.VERSION.SDK_INT +
            LINE_SEPARATOR +
            "Release: " +
            Build.VERSION.RELEASE +
            LINE_SEPARATOR +
            "Incremental: " +
            Build.VERSION.INCREMENTAL +
            LINE_SEPARATOR;
    }
}
