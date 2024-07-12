/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission

class CalendarEventManager(private val context: Context) {

    fun openCalendarEvent(searchResult: SearchResultEntry, listInterface: UnifiedSearchListInterface) {
        val createdAt = searchResult.attributes["createdAt"]?.toLongOrNull()
        val haveReadCalendarPermission = checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        val eventId: Long? = if (haveReadCalendarPermission && createdAt != null) {
            getCalendarEventId(searchResult.title, createdAt)
        } else {
            null
        }

        if (eventId == null) {
            val messageId = if (haveReadCalendarPermission) {
                R.string.unified_search_fragment_calendar_event_cannot_be_found_on_device
            } else {
                R.string.unified_search_fragment_calendar_permission_needed_redirecting_web
            }
            context.showToast(messageId)
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

        val selection = "${CalendarContract.Events.TITLE} = ? AND ${CalendarContract.Events.DTSTART} = ?"
        val selectionArgs = arrayOf(eventTitle, eventStartDate.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                return cursor.getLong(idIndex)
            }
        }

        return null
    }
}
