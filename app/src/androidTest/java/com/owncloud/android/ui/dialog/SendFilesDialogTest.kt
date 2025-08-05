/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SendFilesDialogTest : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.dialog.SendFilesDialogTest"

    companion object {
        private val FILES_SAME_TYPE = setOf(
            OCFile("/1.jpg").apply {
                mimeType = "image/jpg"
            },
            OCFile("/2.jpg").apply {
                mimeType = "image/jpg"
            }
        )
        private val FILES_MIXED_TYPE = setOf(
            OCFile("/1.jpg").apply {
                mimeType = "image/jpg"
            },
            OCFile("/2.pdf").apply {
                mimeType = "application/pdf"
            },
            OCFile("/3.png").apply {
                mimeType = "image/png"
            }
        )
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    private fun showDialog(files: Set<OCFile>, onComplete: (SendFilesDialog) -> Unit) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val fm: FragmentManager = sut.supportFragmentManager
                    val ft = fm.beginTransaction()
                    ft.addToBackStack(null)

                    val dialog = SendFilesDialog.newInstance(files)
                    dialog.show(ft, "TAG_SEND_SHARE_DIALOG")
                    onComplete(dialog)

                    EspressoIdlingResource.decrement()
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showDialog() {
        showDialog(FILES_SAME_TYPE) { sut ->
            val recyclerview: RecyclerView = sut.requireDialog().findViewById(R.id.send_button_recycler_view)
            Assert.assertNotNull("Adapter is null", recyclerview.adapter)
            Assert.assertNotEquals("Send button list is empty", 0, recyclerview.adapter!!.itemCount)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showDialog_Screenshot() {
        showDialog(FILES_SAME_TYPE) { sut ->
            val screenShotName = createName(testClassName + "_" + "showDialog_Screenshot", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut.requireDialog().window?.decorView, screenShotName)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showDialogDifferentTypes() {
        showDialog(FILES_MIXED_TYPE) { sut ->
            val recyclerview: RecyclerView = sut.requireDialog().findViewById(R.id.send_button_recycler_view)
            Assert.assertNotNull("Adapter is null", recyclerview.adapter)
            Assert.assertNotEquals("Send button list is empty", 0, recyclerview.adapter!!.itemCount)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showDialogDifferentTypes_Screenshot() {
        showDialog(FILES_MIXED_TYPE) { sut ->
            val screenShotName = createName(testClassName + "_" + "showDialogDifferentTypes_Screenshot", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut.requireDialog().window?.decorView, screenShotName)
        }
    }
}
