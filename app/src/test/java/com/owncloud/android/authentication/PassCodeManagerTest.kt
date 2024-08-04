/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication

import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.ui.activity.SettingsActivity
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PassCodeManagerTest {
    @MockK
    lateinit var appPreferences: AppPreferences

    @MockK
    lateinit var clockImpl: Clock

    lateinit var sut: PassCodeManager

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxed = true)
        sut = PassCodeManager(appPreferences, clockImpl)
    }

    @Test
    fun testLocked() {
        every { appPreferences.lockPreference } returns SettingsActivity.LOCK_PASSCODE
        every { clockImpl.millisSinceBoot } returns 10000
        assertTrue("Passcode not requested", sut.passCodeShouldBeRequested(200))
    }

    @Test
    fun testPasscodeNotRequested_notEnabled() {
        every { appPreferences.lockPreference } returns ""
        every { clockImpl.millisSinceBoot } returns 10000
        assertFalse("Passcode requested but it shouldn't have been", sut.passCodeShouldBeRequested(200))
    }

    @Test
    fun testPasscodeNotRequested_unlockedRecently() {
        every { appPreferences.lockPreference } returns SettingsActivity.LOCK_PASSCODE
        every { clockImpl.millisSinceBoot } returns 210
        assertFalse("Passcode requested but it shouldn't have been", sut.passCodeShouldBeRequested(200))
    }
}
