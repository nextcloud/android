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

import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS

object TimeUtils {

    @JvmStatic
    fun getDurationParts(duration: Long): DurationParts {
        val days = MILLISECONDS.toDays(duration)
        val hours = MILLISECONDS.toHours(duration) - DAYS.toHours(days)
        val minutes = MILLISECONDS.toMinutes(duration) - HOURS.toMinutes(MILLISECONDS.toHours(duration))
        return DurationParts(days.toInt(), hours.toInt(), minutes.toInt())
    }

    class DurationParts(val days: Int, val hours: Int, val minutes: Int)
}
