/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.Context
import android.text.format.DateUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.database.entity.toUploadEntity
import com.nextcloud.utils.date.DateFormatPattern
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadDateTests {

    companion object {
        private const val THIRTY_SECONDS = 30_000L
        private const val ONE_MINUTE = 60_000L
        private const val ONE_HOUR = 60 * ONE_MINUTE
        private const val ONE_DAY = 24 * ONE_HOUR

        private const val ONE_YEAR = 365L * ONE_DAY
        private const val ONE_MONTH = 30L * ONE_DAY
        private const val ONE_WEEK = 7L * ONE_DAY
        private const val TWO_HOURS = 2L * ONE_HOUR
    }

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun uploadEntityConvertsToOCUploadAndBackCorrectly() {
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
    fun getRelativeDateTimeStringReturnsSecondsAgoForRecentPast() {
        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            System.currentTimeMillis() - THIRTY_SECONDS,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0
        )
        assertEquals(context.getString(R.string.file_list_seconds_ago), result.toString())
    }

    @Test
    fun getRelativeDateTimeStringReturnsFutureAsAbsoluteWhenShowFutureIsFalse() {
        val formatter = SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.US)
        val expected = formatter.format(Date(System.currentTimeMillis() + ONE_MINUTE))

        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            System.currentTimeMillis() + ONE_MINUTE,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
            false
        )
        assertEquals(expected, result.toString())
    }

    @Test
    fun getRelativeDateTimeStringReturnsFutureAsRelativeWhenShowFutureIsTrue() {
        val expected = "In 1 minute"
        val time = System.currentTimeMillis() + ONE_MINUTE

        assertRelativeDateTimeString(time, expected, DateUtils.MINUTE_IN_MILLIS, showFuture = true)
    }

    @Test
    fun getRelativeDateTimeStringReturnsRelativeStringForHoursAgo() {
        val expected = "2 hours ago"
        val time = System.currentTimeMillis() - TWO_HOURS

        assertRelativeDateTimeString(time, expected, DateUtils.SECOND_IN_MILLIS)
    }

    @Test
    fun getRelativeDateTimeStringReturnsAbbreviatedStringForOneWeekAgo() {
        val time = System.currentTimeMillis() - ONE_WEEK
        val formatter = SimpleDateFormat(DateFormatPattern.MonthWithDate.pattern, Locale.US)
        val expected = formatter.format(Date(time))

        assertRelativeDateTimeString(time, expected)
    }

    @Test
    fun getRelativeDateTimeStringReturnsAbbreviatedStringForOneMonthAgo() {
        val time = System.currentTimeMillis() - ONE_MONTH
        val formatter = SimpleDateFormat(DateFormatPattern.MonthWithDate.pattern, Locale.US)
        val expected = formatter.format(Date(time))

        assertRelativeDateTimeString(time, expected, DateUtils.SECOND_IN_MILLIS)
    }

    @Test
    fun getRelativeDateTimeStringReturnsAbsoluteStringForOneYearAgo() {
        val time = System.currentTimeMillis() - ONE_YEAR
        val formatter = SimpleDateFormat("M/d/YYYY", Locale.US)
        val expected = formatter.format(Date(time))

        assertRelativeDateTimeString(time, expected, DateUtils.SECOND_IN_MILLIS)
    }

    @Suppress("MagicNumber")
    @Test
    fun getRelativeDateTimeStringReturnsDaysForDayInMillis() {
        var testTimestamp = System.currentTimeMillis()
        var expected = "Today"
        var result = DisplayUtils.getRelativeDateTimeString(
            context,
            testTimestamp,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
            false
        )
        assertEquals(expected, result)

        testTimestamp = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS
        expected = "Yesterday"
        result = DisplayUtils.getRelativeDateTimeString(
            context,
            testTimestamp,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
            false
        )
        assertEquals(expected, result)

        testTimestamp = System.currentTimeMillis() - 2 * DateUtils.DAY_IN_MILLIS
        expected = "2 days ago"
        result = DisplayUtils.getRelativeDateTimeString(
            context,
            testTimestamp,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
            false
        )
        assertEquals(expected, result)

        testTimestamp = System.currentTimeMillis() - 7 * DateUtils.DAY_IN_MILLIS
        expected = SimpleDateFormat(DateFormatPattern.MonthWithDate.pattern, Locale.US).format(testTimestamp)
        result = DisplayUtils.getRelativeDateTimeString(
            context,
            testTimestamp,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
            false
        )
        assertEquals(expected, result)
    }

    private fun assertRelativeDateTimeString(
        time: Long,
        expected: String,
        minResolution: Long = DateUtils.MINUTE_IN_MILLIS,
        transitionResolution: Long = DateUtils.WEEK_IN_MILLIS,
        showFuture: Boolean = false
    ) {
        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            minResolution,
            transitionResolution,
            0,
            showFuture
        )
        assertEquals(expected.normalizeResult(), result.toString().normalizeResult())
    }

    private fun String.normalizeResult(): String = replace('\u202F', ' ').replace('\u00A0', ' ')
}
