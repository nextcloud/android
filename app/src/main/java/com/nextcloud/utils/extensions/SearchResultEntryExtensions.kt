/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.common.SearchResultEntry
import java.text.SimpleDateFormat
import java.util.Locale

fun SearchResultEntry.parseDateTimeToMillis(): Long? {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val date = sdf.parse(subline)
    return date?.time
}

fun SearchResultEntry.parseDateTimeRange(): Long? {
    val regex = Regex("""(\w+ \d{1,2}, \d{4} \d{1,2}:\d{2} [AP]M) - (\d{1,2}:\d{2} [AP]M)""")
    val matchResult = regex.find(subline)

    if (matchResult != null) {
        val (startDateTimeString, endTimeString) = matchResult.destructured
        val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return dateFormat.parse(startDateTimeString)?.time
    }

    return null
}