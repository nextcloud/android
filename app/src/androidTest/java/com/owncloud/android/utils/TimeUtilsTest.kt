/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.utils.TimeUtils.getDurationParts
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TimeUtilsTest {

    @Test
    fun shouldGetDurationParts() {
        val days = 5
        val hours = 10
        val minutes = 30
        val duration = TimeUnit.DAYS.toMillis(days.toLong()) +
            TimeUnit.HOURS.toMillis(hours.toLong()) +
            TimeUnit.MINUTES.toMillis(minutes.toLong())

        val durationParts = getDurationParts(duration)

        assertEquals(days, durationParts.days)
        assertEquals(hours, durationParts.hours)
        assertEquals(minutes, durationParts.minutes)
    }
}
