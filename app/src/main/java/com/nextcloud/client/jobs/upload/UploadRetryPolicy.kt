/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import com.nextcloud.utils.TimeConstants

class UploadRetryPolicy {
    private var delayInMs: Long = 0

    fun increase() {
        if (delayInMs >= TimeConstants.ONE_SECOND.times(10)) {
            return
        }

        delayInMs += TimeConstants.ONE_SECOND
    }

    fun getDelay(): Long = delayInMs

    fun reset() {
        delayInMs = 0L
    }
}
