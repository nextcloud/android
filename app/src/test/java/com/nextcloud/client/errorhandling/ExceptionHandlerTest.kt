/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.errorhandling

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExceptionHandlerTest {

    companion object {
        private const val REPORT = "report"
    }

    private val thread = Thread("main")
    private val crash = IllegalStateException("crash")

    private val defaultExceptionHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
    private val reportGenerator = mockk<ErrorReportGenerator>()
    private val shownReports = mutableListOf<String>()

    private var showReport: (String) -> Unit = { shownReports.add(it) }

    @Before
    fun setUp() {
        every { reportGenerator.generate(thread, crash) } returns REPORT
    }

    private fun handleCrash() {
        val handler = ExceptionHandler(mockk<Context>(), defaultExceptionHandler, reportGenerator) { showReport(it) }

        handler.uncaughtException(thread, crash)
    }

    @Test
    fun `report is shown and original crash is passed to the platform handler`() {
        handleCrash()

        assertEquals(listOf(REPORT), shownReports)
        verify(exactly = 1) { defaultExceptionHandler.uncaughtException(thread, crash) }
    }

    @Test
    fun `original crash is passed to the platform handler when report generation fails`() {
        val reportFailure = IllegalArgumentException("report failed")
        every { reportGenerator.generate(thread, crash) } throws reportFailure

        handleCrash()

        verify(exactly = 1) { defaultExceptionHandler.uncaughtException(thread, crash) }
        assertTrue(crash.suppressed.contains(reportFailure))
    }

    @Test
    fun `original crash is passed to the platform handler when showing the report throws an error`() {
        val reportFailure = OutOfMemoryError("no memory for report")
        showReport = { throw reportFailure }

        handleCrash()

        verify(exactly = 1) { defaultExceptionHandler.uncaughtException(thread, crash) }
        assertTrue(crash.suppressed.contains(reportFailure))
    }

    @Test
    fun `crash is not added to itself when report generation rethrows it`() {
        every { reportGenerator.generate(thread, crash) } throws crash

        handleCrash()

        verify(exactly = 1) { defaultExceptionHandler.uncaughtException(thread, crash) }
        assertTrue(crash.suppressed.isEmpty())
    }
}
