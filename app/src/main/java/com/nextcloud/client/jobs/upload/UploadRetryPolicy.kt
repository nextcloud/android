/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import com.nextcloud.utils.TimeConstants
import kotlin.random.Random

class UploadRetryPolicy {
    private var delayInMs: Long = 0

    companion object {
        private const val MAX_RANDOM_DELAY = 200L
    }

    fun increase() {
        if (delayInMs >= TimeConstants.ONE_SECOND.times(10)) {
            return
        }

        // random next long used for prevent retrying at the same time if uploads are in parallel
        delayInMs += (TimeConstants.ONE_SECOND + Random.nextLong(MAX_RANDOM_DELAY))
    }

    fun getDelay(): Long = delayInMs

    fun reset() {
        delayInMs = 0L
    }
}
