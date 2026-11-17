/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.errorhandling

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ErrorReportGeneratorTest {

    companion object {
        private const val PACKAGE_NAME = "com.nextcloud.client"
        private const val INSTALLED_VERSION_CODE = 350200902L
        private const val THREAD_NAME = "main"
        private val SUPPORTED_ABIS = listOf("arm64-v8a", "armeabi-v7a")
    }

    private val packageInfo = mockk<PackageInfo>()
    private val packageManager = mockk<PackageManager>()
    private val context = mockk<Context>()

    private lateinit var generator: ErrorReportGenerator

    @Before
    fun setUp() {
        every { context.packageName } returns PACKAGE_NAME
        every { context.packageManager } returns packageManager
        every { context.resources.getString(R.string.buildNumber) } returns ""
        every { packageManager.getPackageInfo(PACKAGE_NAME, 0) } returns packageInfo
        every { packageInfo.longVersionCode } returns INSTALLED_VERSION_CODE

        generator = ErrorReportGenerator(context, SUPPORTED_ABIS)
    }

    private fun generate(exception: Throwable): String = generator.generate(Thread(THREAD_NAME), exception)

    @Test
    fun `print full report to test output`() {
        val exception = IllegalStateException("outer", IllegalArgumentException("inner")).apply {
            addSuppressed(RuntimeException("suppressed"))
        }

        val report = generate(exception)

        println(report)
        assertTrue(report.isNotEmpty())
    }

    @Test
    fun `version is the installed APK versionCode`() {
        val report = generate(IllegalStateException("boom"))

        assertTrue(report.contains("* Version: `$INSTALLED_VERSION_CODE`"))
    }

    @Test
    fun `version falls back to BuildConfig versionCode when package info is unavailable`() {
        every { packageManager.getPackageInfo(PACKAGE_NAME, 0) } throws PackageManager.NameNotFoundException()

        val report = generate(IllegalStateException("boom"))

        assertTrue(report.contains("* Version: `${BuildConfig.VERSION_CODE}`"))
    }

    @Test
    fun `version contains build number when it is set`() {
        every { context.resources.getString(R.string.buildNumber) } returns "123"

        val report = generate(IllegalStateException("boom"))

        assertTrue(report.contains("* Version: `$INSTALLED_VERSION_CODE (build #123)`"))
    }

    @Test
    fun `report contains version name and supported ABIs`() {
        val report = generate(IllegalStateException("boom"))

        assertTrue(report.contains("* Version name: `${BuildConfig.VERSION_NAME}`"))
        assertTrue(report.contains("* Supported ABIs: `arm64-v8a, armeabi-v7a`"))
    }

    @Test
    fun `stack trace contains thread name, exception and cause`() {
        val exception = IllegalStateException("outer", IllegalArgumentException("inner"))

        val report = generate(exception)

        assertTrue(report.contains("Exception in thread \"$THREAD_NAME\" java.lang.IllegalStateException: outer"))
        assertTrue(report.contains("Caused by: java.lang.IllegalArgumentException: inner"))
    }

    @Test
    fun `stack trace contains suppressed exceptions`() {
        val exception = IllegalStateException("outer").apply {
            addSuppressed(IllegalArgumentException("suppressed"))
        }

        val report = generate(exception)

        assertTrue(report.contains("Suppressed: java.lang.IllegalArgumentException: suppressed"))
    }

    @Test
    fun `short stack trace is not truncated`() {
        val report = generate(IllegalStateException("boom"))

        assertFalse(report.contains(ErrorReportGenerator.TRUNCATION_MARKER))
    }

    @Test
    fun `long stack trace is truncated to stay below the Intent size limit`() {
        val longMessage = "x".repeat(ErrorReportGenerator.MAX_STACK_TRACE_LENGTH * 2)

        val report = generate(IllegalStateException(longMessage))

        assertTrue(report.contains(ErrorReportGenerator.TRUNCATION_MARKER))
        assertTrue(report.length < ErrorReportGenerator.MAX_STACK_TRACE_LENGTH * 2)
    }

    @Test
    fun `report is still created when the cause chain is circular`() {
        val first = IllegalStateException("first")
        val second = IllegalArgumentException("second", first)
        first.initCause(second)

        val report = generate(first)

        assertTrue(report.contains("CIRCULAR REFERENCE"))
    }
}
