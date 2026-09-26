/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import kotlin.random.Random

@Suppress("MagicNumber")
class UploadDelayPolicy {
    private var delayInMs: Long = 0

    companion object {
        private const val MAX_DELAY = 120_000L
        private const val INCREMENT_VALUE = 3500L
        private const val MAX_RANDOM_DELAY = 200L
    }

    fun increase() {
        if (delayInMs >= MAX_DELAY) {
            return
        }

        // random next long used for prevent retrying at the same time if uploads are in parallel
        delayInMs += (INCREMENT_VALUE + Random.nextLong(MAX_RANDOM_DELAY))
    }

    fun getDelay(): Long = delayInMs

    fun reset() {
        delayInMs = 0L
    }
}
