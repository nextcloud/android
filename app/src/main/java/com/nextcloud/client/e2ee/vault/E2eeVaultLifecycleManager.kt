/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.nextcloud.client.core.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class E2eeVaultLifecycleManager @Inject constructor(
    private val clock: Clock,
    private val config: E2eeVaultSessionConfig,
    private val session: E2eeVaultSession
) {
    private var startedActivities = 0
    private var backgroundSinceMillis: Long? = null

    fun onActivityStarted() {
        lockIfBackgroundSessionExpired()
        startedActivities += 1
        backgroundSinceMillis = null
    }

    fun onActivityStopped(isDeviceInteractive: Boolean) {
        if (!isDeviceInteractive) {
            session.lockAll()
            backgroundSinceMillis = null
        }

        startedActivities = max(0, startedActivities - 1)

        if (startedActivities == 0 && isDeviceInteractive) {
            backgroundSinceMillis = clock.millisSinceBoot
        }
    }

    private fun lockIfBackgroundSessionExpired() {
        val backgroundStartedAt = backgroundSinceMillis ?: return
        val backgroundDuration = clock.millisSinceBoot - backgroundStartedAt

        if (backgroundDuration > config.unlockDurationMillis) {
            session.lockAll()
        }
    }
}
