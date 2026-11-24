/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2014 Luke Owncloud <owncloud@ohrt.org>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.nextcloud.client.errorhandling

import android.content.Context

class ExceptionHandler(
    context: Context,
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler,
    private val reportGenerator: ErrorReportGenerator = ErrorReportGenerator(context),
    private val showReport: (String) -> Unit = { report ->
        context.startActivity(ShowErrorActivity.createIntent(context, report))
    }
) : Thread.UncaughtExceptionHandler {

    @Suppress("TooGenericExceptionCaught")
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            showReport(reportGenerator.generate(thread, exception))
        } catch (reportFailure: Throwable) {
            // Keep the original crash as the one reported to the OS, so logcat and Play Console show the real cause.
            addSuppressedSafely(exception, reportFailure)
        }

        // Pass exception to OS for graceful handling - OS will report it via ADB
        // and close all activities and services.
        defaultExceptionHandler.uncaughtException(thread, exception)
    }

    private fun addSuppressedSafely(exception: Throwable, reportFailure: Throwable) {
        if (reportFailure === exception) {
            return
        }

        exception.addSuppressed(reportFailure)
    }
}
