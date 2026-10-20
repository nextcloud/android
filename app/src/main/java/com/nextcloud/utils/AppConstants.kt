/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils

import kotlin.time.Duration.Companion.seconds

object AppConstants {
    const val MILLIS_PER_SECOND = 1000
    const val SECONDS_PER_MINUTE = 60
    const val MINUTES_PER_HOUR = 60
    val ONE_SECOND = 1.seconds

    const val BYTES_PER_KB = 1024
    const val BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB

    const val PROGRESS_PERCENTAGE_MIN = 0
    const val PROGRESS_PERCENTAGE_MAX = 100

    const val DEFAULT_CONNECTION_TIMEOUT_MS = 5000
    const val DEFAULT_READ_TIMEOUT_MS = 40000
}
