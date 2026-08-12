/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.nextcloud.client.core.Clock
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class E2eeVaultSessionTest {
    @MockK
    lateinit var clock: Clock

    private lateinit var session: E2eeVaultSession

    private var now = START_TIME

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { clock.millisSinceBoot } answers { now }
        session = E2eeVaultSession(clock, E2eeVaultSessionConfig())
    }

    @Test
    fun startsLocked() {
        assertFalse(session.isUnlocked(VAULT))
    }

    @Test
    fun unlockMakesVaultAvailable() {
        session.unlock(VAULT)

        assertTrue(session.isUnlocked(VAULT))
    }

    @Test
    fun expiredSessionLocksVault() {
        session.unlock(VAULT)

        now += E2eeVaultSessionConfig().unlockDurationMillis + ONE_MILLISECOND

        assertFalse(session.isUnlocked(VAULT))
    }

    @Test
    fun lockOnlyLocksSelectedVault() {
        session.unlock(VAULT)
        session.unlock(OTHER_VAULT)

        session.lock(VAULT)

        assertFalse(session.isUnlocked(VAULT))
        assertTrue(session.isUnlocked(OTHER_VAULT))
    }

    @Test
    fun lockAllLocksEveryVault() {
        session.unlock(VAULT)
        session.unlock(OTHER_VAULT)

        session.lockAll()

        assertFalse(session.isUnlocked(VAULT))
        assertFalse(session.isUnlocked(OTHER_VAULT))
    }

    @Test
    fun lockNotifiesListener() {
        val listener = RecordingLockListener()
        session.addLockListener(listener)
        session.unlock(VAULT)

        session.lock(VAULT)

        assertEquals(listOf(VAULT), listener.lockedVaults)
        assertEquals(0, listener.allVaultLocks)
    }

    @Test
    fun expiredSessionNotifiesListener() {
        val listener = RecordingLockListener()
        session.addLockListener(listener)
        session.unlock(VAULT)

        now += E2eeVaultSessionConfig().unlockDurationMillis + ONE_MILLISECOND
        session.isUnlocked(VAULT)

        assertEquals(listOf(VAULT), listener.lockedVaults)
    }

    @Test
    fun lockAllNotifiesListener() {
        val listener = RecordingLockListener()
        session.addLockListener(listener)

        session.lockAll()

        assertEquals(1, listener.allVaultLocks)
    }

    private class RecordingLockListener : E2eeVaultSessionLockListener {
        val lockedVaults = mutableListOf<E2eeVaultSessionKey>()
        var allVaultLocks = 0

        override fun onVaultLocked(key: E2eeVaultSessionKey) {
            lockedVaults.add(key)
        }

        override fun onAllVaultsLocked() {
            allVaultLocks++
        }
    }

    companion object {
        private const val START_TIME = 1_000L
        private const val ONE_MILLISECOND = 1L
        private val VAULT = E2eeVaultSessionKey("account", 1L)
        private val OTHER_VAULT = E2eeVaultSessionKey("account", 2L)
    }
}
