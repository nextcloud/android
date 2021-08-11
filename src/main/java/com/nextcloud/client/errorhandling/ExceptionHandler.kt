/*
 *   Nextcloud Android client application
 *
 *   @author LukeOwncloud
 *   @author AndyScherzinger
 *   @author Tobias Kaminsky
 *   @author Chris Narkiewicz
 *
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 LukeOwncloud
 *   Copyright (C) 2019 Andy Scherzinger
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.content.Context
import android.content.Intent
import android.os.Build
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import java.io.PrintWriter
import java.io.StringWriter

class ExceptionHandler(
    private val context: Context,
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val LINE_SEPARATOR = "\n"
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {

        @Suppress("TooGenericExceptionCaught") // this is exactly what we want here
        try {
            val stackTrace = StringWriter()
            exception.printStackTrace(PrintWriter(stackTrace))
            val errorReport = generateErrorReport(stackTrace.toString())
            val intent = Intent(context, ShowErrorActivity::class.java)
            intent.putExtra(ShowErrorActivity.EXTRA_ERROR_TEXT, errorReport)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            // Pass exception to OS for graceful handling - OS will report it via ADB
            // and close all activities and services.
            defaultExceptionHandler.uncaughtException(thread, exception)
        } catch (fatalException: Exception) {
            // do not recurse into custom handler if exception is thrown during
            // exception handling. Pass this ultimate fatal exception to OS
            defaultExceptionHandler.uncaughtException(thread, fatalException)
        }
    }

    private fun generateErrorReport(stackTrace: String): String {
        val buildNumber = context.resources.getString(R.string.buildNumber)

        var buildNumberString = ""
        if (buildNumber.isNotEmpty()) {
            buildNumberString = " (build #$buildNumber)"
        }

        return "************ CAUSE OF ERROR ************\n\n" +
            stackTrace +
            "\n************ APP INFORMATION ************" +
            LINE_SEPARATOR +
            "ID: " +
            BuildConfig.APPLICATION_ID +
            LINE_SEPARATOR +
            "Version: " +
            BuildConfig.VERSION_CODE +
            buildNumberString +
            LINE_SEPARATOR +
            "Build flavor: " +
            BuildConfig.FLAVOR +
            LINE_SEPARATOR +
            "\n************ DEVICE INFORMATION ************" +
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
