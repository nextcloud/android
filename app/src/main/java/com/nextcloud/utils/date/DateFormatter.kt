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

object DateFormatter {

    /**
     * Converts a Unix timestamp (in milliseconds) into a formatted date string.
     * For example, input 1733309160885 with "MMM d" pattern outputs "Dec 4".
     */
    @Suppress("MagicNumber")
    fun timestampToDateRepresentation(timestamp: Long, formatPattern: DateFormatPattern): String {
        val date = Date(timestamp * 1000)
        val format = SimpleDateFormat(formatPattern.pattern, Locale.getDefault())
        return format.format(date)
    }
}
