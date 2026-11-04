/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 <Ruba Mogalli> <rubamogalli@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
    fun `full date with hours formats`() {
        assertEquals("03.08.2024 - 14:38", format(DateFormatPattern.FullDateWithHours))
    }

    @Test
    fun `abbreviated month with day formats`() {
        assertEquals("Aug 3", format(DateFormatPattern.MonthWithDate))
    }

    @Test
    fun `full month with year and time formats`() {
        assertEquals("August 03, 2024 14:38", format(DateFormatPattern.MonthDayYearTime))
    }

    @Test
    fun `abbreviated month with year formats`() {
        assertEquals("Aug 2024", format(DateFormatPattern.MonthWithYear))
    }

    @Test
    fun `full month with year formats`() {
        assertEquals("August 2024", format(DateFormatPattern.FullMonthWithYear))
    }

    @Test
    fun `full month formats`() {
        assertEquals("August", format(DateFormatPattern.FullMonth))
    }

    @Test
    fun `year formats`() {
        assertEquals("2024", format(DateFormatPattern.Year))
    }
}
