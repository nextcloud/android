/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import android.os.SystemClock
import java.util.Date
import java.util.TimeZone

class ClockImpl : Clock {
    override val currentTime: Long
        get() = System.currentTimeMillis()

    override val currentDate: Date
        get() = Date(currentTime)

    override val millisSinceBoot: Long
        get() = SystemClock.elapsedRealtime()

    override val tz: TimeZone
        get() = TimeZone.getDefault()
}
