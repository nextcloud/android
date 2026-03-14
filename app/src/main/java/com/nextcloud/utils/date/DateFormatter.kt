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
import java.util.regex.Pattern

object DateFormatter {

    private const val FIRST_DAY_OF_MONTH = 1
    private const val FIRST_MONTH = 1
    private const val YEAR_GROUP = 1
    private const val MONTH_GROUP = 2
    private const val DAY_GROUP = 3

    // Pattern to extract YYYY, YYYY/MM, or YYYY/MM/DD from file path (requires zero-padded month/day)
    private val FOLDER_DATE_PATTERN: Pattern = Pattern.compile("/(\\d{4})(?:/(\\d{2}))?(?:/(\\d{2}))?/")

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

    /**
     * Extract folder date from path (YYYY, YYYY/MM, or YYYY/MM/DD).
     * Uses LocalDate for calendar-aware validation (leap years, days per month).
     * Invalid month/day values fall back to defaults. Future dates are rejected.
     * @return timestamp or null if no folder date found or date is in the future
     */
    @Suppress("TooGenericExceptionCaught")
    fun extractFolderDate(path: String?): Long? {
        try {
            val matcher = path?.let { FOLDER_DATE_PATTERN.matcher(it) }
            val year = matcher?.takeIf { it.find() }?.group(YEAR_GROUP)?.toIntOrNull()

            return year?.let { y ->
                val rawMonth = matcher.group(MONTH_GROUP)?.toIntOrNull()
                val rawDay = matcher.group(DAY_GROUP)?.toIntOrNull()

                val month = rawMonth ?: FIRST_MONTH
                val day = rawDay ?: FIRST_DAY_OF_MONTH

                val localDate = tryCreateDate(y, month, day)
                    ?: tryCreateDate(y, month, FIRST_DAY_OF_MONTH)
                    ?: tryCreateDate(y, FIRST_MONTH, FIRST_DAY_OF_MONTH)

                localDate?.takeIf { !it.isAfter(java.time.LocalDate.now()) }
                    ?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun tryCreateDate(year: Int, month: Int, day: Int): java.time.LocalDate? = try {
        java.time.LocalDate.of(year, month, day)
    } catch (e: java.time.DateTimeException) {
        null
    }
}
