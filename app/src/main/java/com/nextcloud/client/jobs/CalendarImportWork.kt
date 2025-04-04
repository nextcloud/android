/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.logger.Logger
import net.fortuna.ical4j.data.CalendarBuilder
import third_parties.sufficientlysecure.AndroidCalendar
import third_parties.sufficientlysecure.CalendarSource
import third_parties.sufficientlysecure.ProcessVEvent
import java.io.File

class CalendarImportWork(
    private val appContext: Context,
    params: WorkerParameters,
    private val logger: Logger,
    private val contentResolver: ContentResolver
) : Worker(appContext, params) {

    companion object {
        const val TAG = "CalendarImportWork"
    }

    override fun doWork(): Result {
        val calendars = inputData.keyValueMap as? Map<*, *>
        if (calendars == null) {
            logger.d(TAG, "CalendarImportWork cancelled due to null empty input data")
            return Result.failure()
        }

        val calendarBuilder = CalendarBuilder()

        for ((path, selectedCalendarIndex) in calendars) {
            // Check types
            if (path !is String || selectedCalendarIndex !is Int) {
                logger.d(TAG, "Skipping wrong calendar import input data: $path - $selectedCalendarIndex")
                continue
            }

            logger.d(TAG, "Import calendar from $path")

            val file = File(path)
            val calendarSource = CalendarSource(
                file.toURI().toURL().toString(),
                null,
                null,
                null,
                appContext
            )

            val calendarList = AndroidCalendar.loadAll(contentResolver)
            if (selectedCalendarIndex >= calendarList.size) {
                logger.d(TAG, "Skipping selectedCalendarIndex out of bound")
                return Result.failure()
            }

            val selectedCalendar = calendarList[selectedCalendarIndex]

            ProcessVEvent(
                appContext,
                calendarBuilder.build(calendarSource.stream),
                selectedCalendar,
                true
            ).run()
        }

        logger.d(TAG, "CalendarImportWork successfully completed")
        return Result.success()
    }
}
