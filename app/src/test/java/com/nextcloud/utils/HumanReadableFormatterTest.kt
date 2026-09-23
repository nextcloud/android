/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.MainApp
import com.owncloud.android.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HumanReadableFormatterTest {

    private val pendingLabel = "Pending"

    private lateinit var mainApp: MockedStatic<MainApp>
    private lateinit var defaultLocale: Locale
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        mainApp = Mockito.mockStatic(MainApp::class.java)
        mainApp.`when`<String> { MainApp.string(R.string.common_pending) }.thenReturn(pendingLabel)

        defaultLocale = Locale.getDefault()
        defaultTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        mainApp.close()
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `sizes below one kilobyte are shown as whole bytes`() {
        assertEquals("0 B", HumanReadableFormatter.formatBytes(0))
        assertEquals("1 B", HumanReadableFormatter.formatBytes(1))
        assertEquals("512 B", HumanReadableFormatter.formatBytes(512))
        assertEquals("1023 B", HumanReadableFormatter.formatBytes(1023))
    }

    @Test
    fun `each unit starts at exactly 1024 of the previous one`() {
        assertEquals("1 KB", HumanReadableFormatter.formatBytes(1L shl 10))
        assertEquals("1.0 MB", HumanReadableFormatter.formatBytes(1L shl 20))
        assertEquals("1.0 GB", HumanReadableFormatter.formatBytes(1L shl 30))
        assertEquals("1.0 TB", HumanReadableFormatter.formatBytes(1L shl 40))
        assertEquals("1.00 PB", HumanReadableFormatter.formatBytes(1L shl 50))
        assertEquals("1.00 EB", HumanReadableFormatter.formatBytes(1L shl 60))
    }

    @Test
    fun `kilobytes are rounded to whole numbers`() {
        assertEquals("1 KB", HumanReadableFormatter.formatBytes(1025))
        assertEquals("2 KB", HumanReadableFormatter.formatBytes(1536))
    }

    @Test
    fun `megabytes up to terabytes keep one decimal`() {
        assertEquals("1.0 MB", HumanReadableFormatter.formatBytes(1024L * 1024 + 1))
        assertEquals("1.5 GB", HumanReadableFormatter.formatBytes(1610612736))
        assertEquals("5.0 GB", HumanReadableFormatter.formatBytes(5L * 1024 * 1024 * 1024))
    }

    @Test
    fun `petabytes and above keep two decimals`() {
        assertEquals("1.00 PB", HumanReadableFormatter.formatBytes((1L shl 50) + 1))
    }

    @Test
    fun `the largest possible size stays within the known units`() {
        assertEquals("8.00 EB", HumanReadableFormatter.formatBytes(Long.MAX_VALUE))
    }

    @Test
    fun `a size is written with a decimal point in every locale`() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("1.5 GB", HumanReadableFormatter.formatBytes(1610612736))

        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("1.5 GB", HumanReadableFormatter.formatBytes(1610612736))
    }

    @Test
    fun `a negative size is pending`() {
        assertEquals(pendingLabel, HumanReadableFormatter.formatBytes(-1))
        assertEquals(pendingLabel, HumanReadableFormatter.formatBytes(Long.MIN_VALUE))
    }

    @Test
    fun `a timestamp keeps its date and time when read back`() {
        val timestamp = 1790000000000
        val formatted = HumanReadableFormatter.formatDateTime(timestamp)

        val parsed = DateFormat.getDateTimeInstance().parse(formatted)

        assertEquals(Date(timestamp), parsed)
    }

    @Test
    fun `a timestamp is formatted for the current locale`() {
        val timestamp = 1790000000000

        val american = HumanReadableFormatter.formatDateTime(timestamp)
        Locale.setDefault(Locale.GERMANY)
        val german = HumanReadableFormatter.formatDateTime(timestamp)

        assertNotEquals(american, german)
    }
}
