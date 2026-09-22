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
        assertEquals("0 B", HumanReadableFormatter.bytesToHumanReadable(0))
        assertEquals("1 B", HumanReadableFormatter.bytesToHumanReadable(1))
        assertEquals("512 B", HumanReadableFormatter.bytesToHumanReadable(512))
    }

    @Test
    fun `a unit is kept as long as the value fits into it`() {
        assertEquals("1024 B", HumanReadableFormatter.bytesToHumanReadable(1024))
        assertEquals("1024 KB", HumanReadableFormatter.bytesToHumanReadable(1024 * 1024))
        assertEquals("1024.0 GB", HumanReadableFormatter.bytesToHumanReadable(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun `kilobytes are rounded to whole numbers`() {
        assertEquals("1 KB", HumanReadableFormatter.bytesToHumanReadable(1025))
        assertEquals("2 KB", HumanReadableFormatter.bytesToHumanReadable(1536))
    }

    @Test
    fun `megabytes up to terabytes keep one decimal`() {
        assertEquals("1.0 MB", HumanReadableFormatter.bytesToHumanReadable(1024L * 1024 + 1))
        assertEquals("1.5 GB", HumanReadableFormatter.bytesToHumanReadable(1610612736))
        assertEquals("5.0 GB", HumanReadableFormatter.bytesToHumanReadable(5L * 1024 * 1024 * 1024))
    }

    @Test
    fun `petabytes and above keep two decimals`() {
        assertEquals("1.00 PB", HumanReadableFormatter.bytesToHumanReadable(1024L * 1024 * 1024 * 1024 * 1024 + 1))
    }

    @Test
    fun `the largest possible size stays within the known units`() {
        assertEquals("8.00 EB", HumanReadableFormatter.bytesToHumanReadable(Long.MAX_VALUE))
    }

    @Test
    fun `a size is written with a decimal point in every locale`() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("1.5 GB", HumanReadableFormatter.bytesToHumanReadable(1610612736))

        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("1.5 GB", HumanReadableFormatter.bytesToHumanReadable(1610612736))
    }

    @Test
    fun `a negative size is pending`() {
        assertEquals(pendingLabel, HumanReadableFormatter.bytesToHumanReadable(-1))
        assertEquals(pendingLabel, HumanReadableFormatter.bytesToHumanReadable(Long.MIN_VALUE))
    }

    @Test
    fun `a timestamp keeps its date and time when read back`() {
        val timestamp = 1790000000000
        val formatted = HumanReadableFormatter.unixTimeToHumanReadable(timestamp)

        val parsed = DateFormat.getDateTimeInstance().parse(formatted)

        assertEquals(Date(timestamp - timestamp % 1000), parsed)
    }

    @Test
    fun `a timestamp is formatted for the current locale`() {
        val timestamp = 1790000000000

        val american = HumanReadableFormatter.unixTimeToHumanReadable(timestamp)
        Locale.setDefault(Locale.GERMANY)
        val german = HumanReadableFormatter.unixTimeToHumanReadable(timestamp)

        assertNotEquals(american, german)
    }
}
