/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class E2eeVaultSessionConfig @Inject constructor() {
    val unlockDurationMillis: Long = UNLOCK_DURATION_MILLIS

    companion object {
        private const val UNLOCK_DURATION_MINUTES = 5L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val SECONDS_PER_MINUTE = 60L
        private const val UNLOCK_DURATION_MILLIS = UNLOCK_DURATION_MINUTES * SECONDS_PER_MINUTE * MILLIS_PER_SECOND
    }
}
