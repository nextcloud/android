/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication

import androidx.test.core.app.launchActivity
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.test.TestActivity
import com.owncloud.android.ui.activity.SettingsActivity
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
        // set locked state
        every { appPreferences.lockPreference } returns SettingsActivity.LOCK_PASSCODE
        every { appPreferences.lockTimestamp } returns 200
        every { clockImpl.millisSinceBoot } returns 10000

        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
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
    }
}
