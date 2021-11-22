/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
