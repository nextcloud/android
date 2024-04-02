/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.core

import java.util.Date
import java.util.TimeZone

interface Clock {
    val currentTime: Long
    val currentDate: Date
    val millisSinceBoot: Long
    val tz: TimeZone
}
