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

import android.app.Activity
import android.os.PowerManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.ui.activity.SettingsActivity
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * This class should really be unit tests, but PassCodeManager needs a refactor
 * to decouple the locking logic from the platform classes
 */
class PassCodeManagerIT {
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
    fun testResumeDuplicateActivity() {
        // mock activity instead of using real one to avoid dealing with activity transitions
        val activity: Activity = mockk()
        val powerManager: PowerManager = mockk()
        every { powerManager.isScreenOn } returns true
        every { activity.getSystemService(Activity.POWER_SERVICE) } returns powerManager
        every { activity.window } returns null
        every { activity.startActivityForResult(any(), any()) } just runs
        every { activity.moveTaskToBack(any()) } returns true

        // set locked state
        every { appPreferences.lockPreference } returns SettingsActivity.LOCK_PASSCODE
        every { appPreferences.lockTimestamp } returns 200
        every { clockImpl.millisSinceBoot } returns 10000

        // resume activity twice
        var askedForPin = sut.onActivityResumed(activity)
        assertTrue("Passcode not requested on first launch", askedForPin)
        sut.onActivityResumed(activity)

        // stop it once
        sut.onActivityStopped(activity)

        // resume again. should ask for passcode
        askedForPin = sut.onActivityResumed(activity)
        assertTrue("Passcode not requested on subsequent launch after stop", askedForPin)
    }
}
