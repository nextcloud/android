/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.errorhandling

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

class ErrorReportGenerator(
    private val context: Context,
    private val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
) {

    companion object {
        private const val TAG = "ErrorReportGenerator"

        /**
         * The report is sent to [ShowErrorActivity] as an Intent extra, which must stay well below
         * the 1 MB Binder transaction limit.
         */
        const val MAX_STACK_TRACE_LENGTH = 100_000
        const val TRUNCATION_MARKER = "... (stack trace truncated)"
    }

    fun generate(thread: Thread, exception: Throwable): String {
        val stackTrace = formatStackTrace(thread, exception)

        return """
            |### Cause of error
            |```java
            ${stackTrace.prependIndent("|")}
            |```
            |
            |### App information
            |* ID: `${BuildConfig.APPLICATION_ID}`
            |* Version: `${installedVersionCode()}${buildNumberSuffix()}`
            |* Version name: `${BuildConfig.VERSION_NAME}`
            |* Build flavor: `${BuildConfig.FLAVOR}`
            |
            |### Device information
            |* Brand: `${Build.BRAND}`
            |* Device: `${Build.DEVICE}`
            |* Model: `${Build.MODEL}`
            |* Id: `${Build.ID}`
            |* Product: `${Build.PRODUCT}`
            |* Supported ABIs: `${supportedAbis.joinToString()}`
            |
            |### Firmware
            |* SDK: `${Build.VERSION.SDK_INT}`
            |* Release: `${Build.VERSION.RELEASE}`
            |* Incremental: `${Build.VERSION.INCREMENTAL}`
        """.trimMargin("|")
    }

    private fun formatStackTrace(thread: Thread, exception: Throwable): String {
        val stackTrace = "Exception in thread \"${thread.name}\" ${exception.stackTraceToString()}".trimEnd()
        if (stackTrace.length <= MAX_STACK_TRACE_LENGTH) {
            return stackTrace
        }

        return stackTrace.take(MAX_STACK_TRACE_LENGTH) + "\n" + TRUNCATION_MARKER
    }

    private fun buildNumberSuffix(): String {
        val buildNumber = context.resources.getString(R.string.buildNumber)
        if (buildNumber.isEmpty()) {
            return ""
        }

        return " (build #$buildNumber)"
    }

    /**
     * F-Droid ships one APK per ABI, each with its own versionCode, while [BuildConfig.VERSION_CODE] is
     * shared by all of them. Read the installed APK's versionCode to report the right one.
     */
    private fun installedVersionCode(): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        Log_OC.e(TAG, "Could not read installed versionCode", e)
        BuildConfig.VERSION_CODE.toLong()
    }
}
