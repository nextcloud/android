/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
