/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.fragment.app.FragmentManager
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class SendShareDialogTest : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.dialog.SendShareDialogTest"

    @Test
    @ScreenshotTest
    fun showDialog() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val fm: FragmentManager = activity.supportFragmentManager
                    val ft = fm.beginTransaction()
                    ft.addToBackStack(null)

                    val file = OCFile("/1.jpg").apply {
                        mimeType = "image/jpg"
                    }
                    EspressoIdlingResource.decrement()

                    val sut = SendShareDialog.newInstance(file, false, OCCapability())
                    sut.show(ft, "TAG_SEND_SHARE_DIALOG")
                    val screenShotName = createName(testClassName + "_" + "showDialog", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut.requireDialog().window?.decorView, screenShotName)
                }
            }
        }
    }
}
