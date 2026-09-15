/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.database.entity.toUploadEntity
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class UploadDateTests {

    companion object {
        private const val THIRTY_SECONDS = 30_000L
        private const val ONE_MINUTE = 60_000L
        private const val ONE_HOUR = 60 * ONE_MINUTE
        private const val ONE_DAY = 24 * ONE_HOUR

        private const val ONE_AND_A_HALF_MINUTES = ONE_MINUTE + THIRTY_SECONDS
        private const val TWO_AND_A_HALF_HOURS = 2 * ONE_HOUR + ONE_HOUR / 2
        private const val TWO_DAYS = 2 * ONE_DAY
        private const val ONE_WEEK = 7 * ONE_DAY
        private const val ONE_MONTH = 30 * ONE_DAY
        private const val ONE_YEAR = 365 * ONE_DAY

        private const val DATE_AND_TIME_PARTS = 2
        private const val TIME_OF_DAY_SEPARATOR = ":"
        private const val NARROW_NO_BREAK_SPACE = ' '
        private const val NO_BREAK_SPACE = ' '
    }

    private lateinit var context: Context
    private lateinit var systemLocale: Locale

    @Before
    fun setup() {
        systemLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        context = InstrumentationRegistry.getInstrumentation().targetContext.localized(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(systemLocale)
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
        assertRelativeDateTimeString(
            time = System.currentTimeMillis() - THIRTY_SECONDS,
            expected = context.getString(R.string.file_list_seconds_ago)
        )
    }

    @Test
    fun getRelativeDateTimeStringReturnsFutureAsAbsoluteWhenShowFutureIsFalse() {
        val time = System.currentTimeMillis() + ONE_AND_A_HALF_MINUTES

        assertRelativeDateTimeString(
            time = time,
            expected = DateFormat.getDateTimeInstance().format(Date(time))
        )
    }

    @Test
    fun getRelativeDateTimeStringReturnsFutureAsRelativeWhenShowFutureIsTrue() {
        assertRelativeDateTimeString(
            time = System.currentTimeMillis() + ONE_AND_A_HALF_MINUTES,
            expected = "In 1 minute",
            showFuture = true
        )
    }

    @Test
    fun getRelativeDateTimeStringReturnsRelativeStringForHoursAgo() {
        assertRelativeDateTimeString(
            time = System.currentTimeMillis() - TWO_AND_A_HALF_HOURS,
            expected = "2 hours ago",
            minResolution = DateUtils.SECOND_IN_MILLIS
        )
    }

    @Test
    fun getRelativeDateTimeStringReturnsTodayForNow() {
        assertRelativeDateTimeString(
            time = System.currentTimeMillis(),
            expected = "Today",
            minResolution = DateUtils.DAY_IN_MILLIS
        )
    }

    @Test
    fun getRelativeDateTimeStringReturnsYesterdayForOneDayAgo() {
        assertRelativeDateTimeString(
            time = System.currentTimeMillis() - ONE_DAY,
            expected = "Yesterday",
            minResolution = DateUtils.DAY_IN_MILLIS
        )
    }

    @Test
    fun getRelativeDateTimeStringReturnsDaysAgoForTwoDaysAgo() {
        assertRelativeDateTimeString(
            time = System.currentTimeMillis() - TWO_DAYS,
            expected = "2 days ago",
            minResolution = DateUtils.DAY_IN_MILLIS
        )
    }

    @Test
    fun getRelativeDateTimeStringDropsTimeOfDayForOneWeekAgo() {
        assertDateWithoutTimeOfDay(System.currentTimeMillis() - ONE_WEEK, DateUtils.MINUTE_IN_MILLIS)
    }

    @Test
    fun getRelativeDateTimeStringDropsTimeOfDayForOneWeekAgoWithDayResolution() {
        assertDateWithoutTimeOfDay(System.currentTimeMillis() - ONE_WEEK, DateUtils.DAY_IN_MILLIS)
    }

    @Test
    fun getRelativeDateTimeStringDropsTimeOfDayForOneMonthAgo() {
        assertDateWithoutTimeOfDay(System.currentTimeMillis() - ONE_MONTH, DateUtils.SECOND_IN_MILLIS)
    }

    @Test
    fun getRelativeDateTimeStringDropsTimeOfDayForOneYearAgo() {
        assertDateWithoutTimeOfDay(System.currentTimeMillis() - ONE_YEAR, DateUtils.SECOND_IN_MILLIS)
    }

    private fun assertRelativeDateTimeString(
        time: Long,
        expected: String,
        minResolution: Long = DateUtils.MINUTE_IN_MILLIS,
        showFuture: Boolean = false
    ) {
        val result = DisplayUtils.getRelativeDateTimeString(
            context,
            time,
            minResolution,
            DateUtils.WEEK_IN_MILLIS,
            0,
            showFuture
        )

        assertEquals(expected.normalizeSpaces(), result.toString().normalizeSpaces())
    }

    private fun assertDateWithoutTimeOfDay(time: Long, minResolution: Long) = assertRelativeDateTimeString(
        time = time,
        expected = platformDateClause(time, minResolution),
        minResolution = minResolution
    )

    private fun platformDateClause(time: Long, minResolution: Long): String {
        val platformString = DateUtils.getRelativeDateTimeString(
            context,
            time,
            minResolution,
            DateUtils.WEEK_IN_MILLIS,
            0
        ).toString()

        val parts = platformString.split(",").map(String::trim)
        if (parts.size != DATE_AND_TIME_PARTS) {
            return platformString
        }

        val (date, timeOfDay) = parts
        return when {
            timeOfDay.hasTimeOfDay() && !date.hasTimeOfDay() -> date
            date.hasTimeOfDay() && !timeOfDay.hasTimeOfDay() -> timeOfDay
            else -> platformString
        }
    }

    private fun Context.localized(locale: Locale): Context {
        val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
        return createConfigurationContext(configuration)
    }

    private fun String.hasTimeOfDay(): Boolean = contains(TIME_OF_DAY_SEPARATOR)

    private fun String.normalizeSpaces(): String = replace(NARROW_NO_BREAK_SPACE, ' ').replace(NO_BREAK_SPACE, ' ')
}
