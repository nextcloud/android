/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.annotation.SuppressLint
import com.nextcloud.model.SearchResultEntryType
import com.owncloud.android.lib.common.SearchResultEntry
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

fun SearchResultEntry.getType(): SearchResultEntryType {
    return if (icon == "icon-folder") {
        SearchResultEntryType.Folder
    } else if (icon.startsWith("icon-note")) {
        SearchResultEntryType.Note
    } else if (icon.startsWith("icon-contacts")) {
        SearchResultEntryType.Contact
    } else if (icon.startsWith("icon-calendar")) {
        SearchResultEntryType.CalendarEvent
    } else if (icon.startsWith("icon-deck")) {
        SearchResultEntryType.Deck
    } else {
        SearchResultEntryType.Unknown
    }
}

@SuppressLint("SimpleDateFormat")
fun SearchResultEntry.parseDateTimeRange(): Long? {
    val cleanedSubline: String = subline.replace('\u202F', ' ')
    val formatter = SimpleDateFormat("MMM d, yyyy h:mm a")
    val startDate = cleanedSubline.substringBefore(" -")

    try {
        val date: Date? = formatter.parse(startDate)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return date?.time
    } catch (e: ParseException) {
        return null
    }
}
