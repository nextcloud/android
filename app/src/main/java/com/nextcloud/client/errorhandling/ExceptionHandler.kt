/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2014 Luke Owncloud <owncloud@ohrt.org>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.nextcloud.client.errorhandling

import android.content.Context
import android.content.Intent
import android.os.Build
import com.owncloud.android.BuildConfig
import com.owncloud.android.R

class ExceptionHandler(
    private val context: Context,
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val LINE_SEPARATOR = "\n"
        private const val EXCEPTION_FORMAT_MAX_RECURSIVITY = 10
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        @Suppress("TooGenericExceptionCaught") // this is exactly what we want here
        try {
            val errorReport = generateErrorReport(formatException(thread, exception))
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

    private fun formatException(thread: Thread, exception: Throwable): String {
        fun formatExceptionRecursive(thread: Thread, exception: Throwable, count: Int = 0): String {
            if (count > EXCEPTION_FORMAT_MAX_RECURSIVITY) {
                return "Max number of recursive exception causes exceeded!"
            }
            // print exception
            val stringBuilder = StringBuilder()
            val stackTrace = exception.stackTrace
            stringBuilder.appendLine("Exception in thread \"${thread.name}\" $exception")
            // print available stacktrace
            for (element in stackTrace) {
                stringBuilder.appendLine("    at $element")
            }
            // print cause recursively
            exception.cause?.let {
                stringBuilder.append("Caused by: ")
                stringBuilder.append(formatExceptionRecursive(thread, it, count + 1))
            }
            return stringBuilder.toString()
        }

        return formatExceptionRecursive(thread, exception, 0)
    }

    private fun generateErrorReport(stackTrace: String): String {
        val buildNumber = context.resources.getString(R.string.buildNumber)

        val buildNumberString = when {
            buildNumber.isNotEmpty() -> " (build #$buildNumber)"
            else -> ""
        }

        return """
            |### Cause of error
            |```java
            ${stackTrace.prependIndent("|")}
            |```
            |
            |### App information
            |* ID: `${BuildConfig.APPLICATION_ID}`
            |* Version: `${BuildConfig.VERSION_CODE}$buildNumberString`
            |* Build flavor: `${BuildConfig.FLAVOR}`
            |
            |### Device information
            |* Brand: `${Build.BRAND}`
            |* Device: `${Build.DEVICE}`
            |* Model: `${Build.MODEL}`
            |* Id: `${Build.ID}`
            |* Product: `${Build.PRODUCT}`
            |
            |### Firmware
            |* SDK: `${Build.VERSION.SDK_INT}`
            |* Release: `${Build.VERSION.RELEASE}`
            |* Incremental: `${Build.VERSION.INCREMENTAL}`
        """.trimMargin("|")
    }
}
