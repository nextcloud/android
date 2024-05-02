/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import org.junit.Rule

class PreviewImageFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

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
