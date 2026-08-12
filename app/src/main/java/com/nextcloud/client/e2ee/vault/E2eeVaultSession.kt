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

@Singleton
class E2eeVaultSession @Inject constructor(private val clock: Clock, private val config: E2eeVaultSessionConfig) {
    private val unlockedUntilByVault = mutableMapOf<E2eeVaultSessionKey, Long>()
    private val lockListeners = mutableSetOf<E2eeVaultSessionLockListener>()

    @Synchronized
    fun isUnlocked(key: E2eeVaultSessionKey): Boolean {
        val unlockedUntil = unlockedUntilByVault[key]
        val hasActiveSession = unlockedUntil != null && clock.millisSinceBoot <= unlockedUntil

        if (!hasActiveSession) {
            lockExpiredVault(key)
        }

        return hasActiveSession
    }

    @Synchronized
    fun unlock(key: E2eeVaultSessionKey) {
        unlockedUntilByVault[key] = clock.millisSinceBoot + config.unlockDurationMillis
    }

    @Synchronized
    fun lock(key: E2eeVaultSessionKey) {
        if (unlockedUntilByVault.remove(key) != null) {
            lockListeners.forEach { it.onVaultLocked(key) }
        }
    }

    @Synchronized
    fun lockAll() {
        unlockedUntilByVault.clear()
        lockListeners.forEach { it.onAllVaultsLocked() }
    }

    @Synchronized
    fun addLockListener(listener: E2eeVaultSessionLockListener) {
        lockListeners.add(listener)
    }

    @Synchronized
    fun removeLockListener(listener: E2eeVaultSessionLockListener) {
        lockListeners.remove(listener)
    }

    private fun lockExpiredVault(key: E2eeVaultSessionKey) {
        if (unlockedUntilByVault.remove(key) != null) {
            lockListeners.forEach { it.onVaultLocked(key) }
        }
    }
}
