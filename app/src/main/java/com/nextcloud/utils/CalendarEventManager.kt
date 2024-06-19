/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.nextcloud.utils.extensions.parseDateTimeRange
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface

class CalendarEventManager(private val context: Context) {

    fun openCalendarEvent(searchResult: SearchResultEntry, listInterface: UnifiedSearchListInterface) {
        val eventStartDate = searchResult.parseDateTimeRange()

        if (eventStartDate == null) {
            listInterface.onSearchResultClicked(searchResult)
            return
        }

        val eventId: Long? = getCalendarEventId(searchResult.title, eventStartDate)

        if (eventId == null) {
            listInterface.onSearchResultClicked(searchResult)
        } else {
            val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val intent = Intent(Intent.ACTION_VIEW).setData(uri)
            context.startActivity(intent)
        }
    }

    private fun getCalendarEventId(eventTitle: String, eventStartDate: Long): Long? {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
            val titleIndex = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            val startDateIndex = cursor.getColumnIndex(CalendarContract.Events.DTSTART)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex)
                val startDate = cursor.getLong(startDateIndex)

                if (eventTitle == title && startDate == eventStartDate) {
                    return cursor.getLong(idIndex)
                }
            }
        }

        return null
    }
}
