/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 <Ruba Mogalli> <rubamogalli@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.date

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateFormatPatternTest {

    // Fixed date, so the result never depends on today's date or timezone
    private val date = LocalDateTime.of(2024, 8, 3, 14, 38)

    private fun format(pattern: DateFormatPattern): String =
        DateTimeFormatter.ofPattern(pattern.pattern, Locale.ENGLISH).format(date)

    @Test
    fun fullDateWithHours_formatsDateAndTime() {
        assertEquals("03.08.2024 - 14:38", format(DateFormatPattern.FullDateWithHours))
    }

    @Test
    fun monthWithDate_formatsShortMonthAndDay() {
        assertEquals("Aug 3", format(DateFormatPattern.MonthWithDate))
    }

    @Test
    fun fullMonthWithYear_formatsFullMonthAndYearAndTime() {
        assertEquals("August 03, 2024 14:38", format(DateFormatPattern.MonthDayYearTime))
    }

    @Test
    fun monthWithYear_formatsShortMonthAndYear() {
        assertEquals("Aug 2024", format(DateFormatPattern.MonthWithYear))
    }

    @Test
    fun fullMonthWithYear_formatsFullMonthAndYear() {
        assertEquals("August 2024", format(DateFormatPattern.FullMonthWithYear))
    }

    @Test
    fun fullMonth_formatsFullMonthName() {
        assertEquals("August", format(DateFormatPattern.FullMonth))
    }

    @Test
    fun year_formatsFourDigitYear() {
        assertEquals("2024", format(DateFormatPattern.Year))
    }
}
