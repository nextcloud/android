/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.utils

import android.content.Context
import android.text.format.DateUtils
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.database.entity.toUploadEntity
import com.owncloud.android.utils.DisplayUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import com.owncloud.android.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadConversionTest {

    companion object {
        private const val JANUARY_27_2026 = 1769505718000
        private const val ONE_YEAR = 365L * 24 * 60 * 60 * 1000
        private const val ONE_MONTH = 30L * 24 * 60 * 60 * 1000
        private const val ONE_WEEK = 7 * 24 * 60 * 60 * 1000
        private const val TWO_HOURS = 2 * 60 * 60 * 1000
        private const val ONE_MINUTE = 60_000
        private const val THIRTY_SECONDS = 30_000

        private const val DATE_FORMATTER_PATTERN = "MMM dd, yyyy, hh:mm:ss a"
    }

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockkStatic(DateUtils::class)
    }

    @Test
    fun `UploadEntity converts to OCUpload and back correctly`() {
        val entity = UploadEntity(
            id = 123,
            localPath = "/local/file.txt",
            remotePath = "/remote/file.txt",
            accountName = "test@example.com",
            fileSize = 1024L,
            status = 2,
            localBehaviour = 1,
            uploadTime = null,
            nameCollisionPolicy = 0,
            isCreateRemoteFolder = 1,
            uploadEndTimestamp = 0,
            uploadEndTimestampLong = 1_650_000_000_000,
            lastResult = 0,
            isWhileChargingOnly = 1,
            isWifiOnly = 1,
            createdBy = 5,
            folderUnlockToken = "token123"
        )

        val upload = entity.toOCUpload()
        assertNotNull(upload)
        assertEquals(entity.localPath, upload?.localPath)
        assertEquals(entity.remotePath, upload?.remotePath)
        assertEquals(entity.uploadEndTimestampLong, upload?.uploadEndTimestamp)

        val convertedEntity = upload!!.toUploadEntity()
        assertEquals(entity.localPath, convertedEntity.localPath)
        assertEquals(entity.remotePath, convertedEntity.remotePath)
        assertEquals(entity.uploadEndTimestampLong, convertedEntity.uploadEndTimestampLong)
        assertEquals(entity.isCreateRemoteFolder, convertedEntity.isCreateRemoteFolder)
        assertEquals(entity.isWifiOnly, convertedEntity.isWifiOnly)
        assertEquals(entity.isWhileChargingOnly, convertedEntity.isWhileChargingOnly)
    }

    @Test
    fun `getRelativeDateTimeString returns seconds ago for recent past`() {
        val expectedResult = "seconds ago"

        every { context.getString(R.string.file_list_seconds_ago) } returns "seconds ago"

        every {
            DateUtils.getRelativeDateTimeString(
                any<Context>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { expectedResult }

        val time = JANUARY_27_2026 - THIRTY_SECONDS

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        assertEquals(expectedResult, result)
    }

    @Test
    fun `getRelativeDateTimeString returns future as human readable when showFuture is false`() {
        val formatter = SimpleDateFormat(DATE_FORMATTER_PATTERN, Locale.US)

        val time = JANUARY_27_2026 + ONE_MINUTE
        val timeAsDate = Date(time)
        val expectedString = formatter.format(timeAsDate)

        every {
            DateUtils.getRelativeDateTimeString(
                any<Context>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { expectedString }

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        val normalizedResult = result.toString().normalizeResult()
        val normalizedExpected = expectedString.normalizeResult()
        assertEquals(normalizedExpected, normalizedResult)
    }

    @Test
    fun `getRelativeDateTimeString returns proper relative string for hours ago`() {
        every {
            DateUtils.getRelativeDateTimeString(
                any<Context>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { "2 hours ago" }

        val time = JANUARY_27_2026 - TWO_HOURS

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        assert(result.isNotEmpty())
    }

    @Test
    fun `getRelativeDateTimeString returns relative string for one week ago`() {
        val expectedResult = "Jan 20"

        every {
            DateUtils.getRelativeDateTimeString(
                any<Context>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { expectedResult }

        val time = JANUARY_27_2026 - ONE_WEEK

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        assertEquals(expectedResult, result)
    }

    @Test
    fun `getRelativeDateTimeString returns relative string for one month ago`() {
        val expectedResult = "12/28/2025"

        every {
            DateUtils.getRelativeDateTimeString(
                any<Context>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { expectedResult }

        val time = JANUARY_27_2026 - ONE_MONTH

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        assertEquals(expectedResult, result)
    }

    @Test
    fun `getRelativeDateTimeString returns relative string for one year ago`() {
        val expectedResult = "1/27/2025"

        every {
            DateUtils.getRelativeDateTimeString(
                any<Context>(),
                any(),
                any(),
                any(),
                any()
            )
        } answers { expectedResult }

        val time = JANUARY_27_2026 - ONE_YEAR

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )

        assertEquals(expectedResult, result)
    }

    private fun String.normalizeResult(): String {
        return replace('\u202F', ' ').replace('\u00A0', ' ')
    }
}
