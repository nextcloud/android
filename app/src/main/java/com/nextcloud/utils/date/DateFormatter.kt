/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.date

import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nextcloud.utils.AppConstants.MILLIS_PER_SECOND

object DateFormatter {

    /**
     * Converts a Unix timestamp (in milliseconds) into a formatted date string.
     * For example, input 1733309160885 with "MMM d" pattern outputs "Dec 4".
     */
    fun timestampToDateRepresentation(timestamp: Long, formatPattern: DateFormatPattern): String {
        val date = Date(timestamp * MILLIS_PER_SECOND)
        val format = SimpleDateFormat(formatPattern.pattern, Locale.getDefault())
        return format.format(date)
    }
}
