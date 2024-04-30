/*
 * Nextcloud - Android Client
 *
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
        const val SELECTED_CALENDARS = "selected_contacts_indices"
    }

    override fun doWork(): Result {
        val calendarPaths = inputData.getStringArray(SELECTED_CALENDARS) ?: arrayOf<String>()
        val calendars = inputData.keyValueMap as Map<String, AndroidCalendar>

        val calendarBuilder = CalendarBuilder()

        for ((path, selectedCalendar) in calendars) {
            logger.d(TAG, "Import calendar from $path")

            val file = File(path)
            val calendarSource = CalendarSource(
                file.toURI().toURL().toString(),
                null,
                null,
                null,
                appContext
            )

            val calendars = AndroidCalendar.loadAll(contentResolver)[0]

            ProcessVEvent(
                appContext,
                calendarBuilder.build(calendarSource.stream),
                selectedCalendar,
                true
            ).run()
        }

        return Result.success()
    }
}
