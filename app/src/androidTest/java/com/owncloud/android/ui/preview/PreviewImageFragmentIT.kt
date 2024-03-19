/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import org.junit.After
import org.junit.Rule

class PreviewImageFragmentIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<TestActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TestActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TestActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    // Disabled for now due to strange failing when using entire test suite
    // Findings so far:
    // PreviewImageFragmentIT runs fine when only running this
    // running it in whole test suite fails
    // manually tried to execute LoadBitmapTask, but this does not start "doInBackground", but only creates class

    // @Test
    // @ScreenshotTest
    // fun corruptImage() {
    //     val activity = testActivityRule.launchActivity(null)
    //
    //     val ocFile = OCFile("/test.png")
    //     val sut = PreviewImageFragment.newInstance(ocFile, true, false)
    //
    //     activity.addFragment(sut)
    //
    //     while (!sut.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
    //         shortSleep()
    //     }
    //
    //     screenshot(activity)
    // }
    //
    // @Test
    // @ScreenshotTest
    // fun validImage() {
    //     val activity = testActivityRule.launchActivity(null)
    //
    //     val ocFile = OCFile("/test.png")
    //     ocFile.storagePath = getFile("imageFile.png").absolutePath
    //
    //     val sut = PreviewImageFragment.newInstance(ocFile, true, false)
    //
    //     activity.addFragment(sut)
    //
    //     while (!sut.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
    //         shortSleep()
    //     }
    //
    //     screenshot(activity)
    // }
}
