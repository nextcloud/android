/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
