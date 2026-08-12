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
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class E2eeVaultLifecycleManagerTest {
    @MockK
    lateinit var clock: Clock

    @MockK(relaxed = true)
    lateinit var session: E2eeVaultSession

    private lateinit var lifecycleManager: E2eeVaultLifecycleManager

    private var now = START_TIME

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { clock.millisSinceBoot } answers { now }
        lifecycleManager = E2eeVaultLifecycleManager(clock, E2eeVaultSessionConfig(), session)
    }

    @Test
    fun doesNotLockWhenAppReturnsBeforeSessionTimeout() {
        lifecycleManager.onActivityStarted()
        lifecycleManager.onActivityStopped(isDeviceInteractive = true)

        now += E2eeVaultSessionConfig().unlockDurationMillis
        lifecycleManager.onActivityStarted()

        verify(exactly = 0) { session.lockAll() }
    }

    @Test
    fun locksWhenAppReturnsAfterSessionTimeout() {
        lifecycleManager.onActivityStarted()
        lifecycleManager.onActivityStopped(isDeviceInteractive = true)

        now += E2eeVaultSessionConfig().unlockDurationMillis + ONE_MILLISECOND
        lifecycleManager.onActivityStarted()

        verify(exactly = 1) { session.lockAll() }
    }

    @Test
    fun locksImmediatelyWhenDeviceIsNotInteractive() {
        lifecycleManager.onActivityStarted()

        lifecycleManager.onActivityStopped(isDeviceInteractive = false)

        verify(exactly = 1) { session.lockAll() }
    }

    @Test
    fun doesNotStartBackgroundTimerUntilLastActivityStops() {
        lifecycleManager.onActivityStarted()
        lifecycleManager.onActivityStarted()
        lifecycleManager.onActivityStopped(isDeviceInteractive = true)

        now += E2eeVaultSessionConfig().unlockDurationMillis + ONE_MILLISECOND
        lifecycleManager.onActivityStarted()

        verify(exactly = 0) { session.lockAll() }
    }

    companion object {
        private const val START_TIME = 1_000L
        private const val ONE_MILLISECOND = 1L
    }
}
