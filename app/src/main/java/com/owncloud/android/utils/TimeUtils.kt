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
