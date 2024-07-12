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

// FIXME
@SuppressLint("SimpleDateFormat")
fun SearchResultEntry.parseDateTimeRange(): Long? {
    // Define the input and output date formats
    val inputFormat = SimpleDateFormat("MMM d, yyyy h:mm a")
    val outputFormat = SimpleDateFormat("MMM d, yyyy HH:mm a")

    // Parse the input date string
    val startDateTime = inputFormat.parse(subline.split(" - ")[0])

    // Format the date to the desired output format
    val result = outputFormat.format(startDateTime)


    val formatter = SimpleDateFormat("MMM dd, yyyy HH:MM")
    try {
        val date = formatter.parse(result)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return date?.time
    } catch (e: ParseException) {
       return null
    }
}