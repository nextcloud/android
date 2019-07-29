/*
 *   Nextcloud Android client application
 *
 *   @author LukeOwncloud
 *   @author AndyScherzinger
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 LukeOwncloud
 *   Copyright (C) 2019 Andy Scherzinger
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
 */
package com.nextcloud.client.errorhandling

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class ExceptionHandler(private val context: Activity) : Thread.UncaughtExceptionHandler {
    companion object {
        private val TAG = ExceptionHandler::class.java.simpleName
        private val LINE_SEPARATOR = "\n"
        private val STATUS = 1000
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e(TAG, "ExceptionHandler caught UncaughtException", exception)
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))

        val errorReport = generateErrorReport(stackTrace.toString())

        Log.e(TAG, "An exception was thrown and handled by ExceptionHandler:", exception)

        val intent = Intent(context, ShowErrorActivity::class.java)
        intent.putExtra(ShowErrorActivity.EXTRA_ERROR_TEXT, errorReport)
        context.startActivity(intent)

        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(STATUS)
    }

    private fun generateErrorReport(stackTrace: String): String {
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
            LINE_SEPARATOR
    }
}
