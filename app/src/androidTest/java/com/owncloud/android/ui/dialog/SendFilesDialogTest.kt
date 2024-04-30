/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class SendFilesDialogTest : AbstractIT() {
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

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    private fun showDialog(files: Set<OCFile>): SendFilesDialog {
        val activity = testActivityRule.launchActivity(null)

        val fm: FragmentManager = activity.supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)

        val sut = SendFilesDialog.newInstance(files)
        sut.show(ft, "TAG_SEND_SHARE_DIALOG")

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()

        return sut
    }

    @Test
    fun showDialog() {
        val sut = showDialog(FILES_SAME_TYPE)
        val recyclerview: RecyclerView = sut.requireDialog().findViewById(R.id.send_button_recycler_view)
        Assert.assertNotNull("Adapter is null", recyclerview.adapter)
        Assert.assertNotEquals("Send button list is empty", 0, recyclerview.adapter!!.itemCount)
    }

    @Test
    @ScreenshotTest
    fun showDialog_Screenshot() {
        val sut = showDialog(FILES_SAME_TYPE)
        sut.requireDialog().window?.decorView.let { screenshot(it) }
    }

    @Test
    fun showDialogDifferentTypes() {
        val sut = showDialog(FILES_MIXED_TYPE)
        val recyclerview: RecyclerView = sut.requireDialog().findViewById(R.id.send_button_recycler_view)
        Assert.assertNotNull("Adapter is null", recyclerview.adapter)
        Assert.assertNotEquals("Send button list is empty", 0, recyclerview.adapter!!.itemCount)
    }

    @Test
    @ScreenshotTest
    fun showDialogDifferentTypes_Screenshot() {
        val sut = showDialog(FILES_MIXED_TYPE)
        sut.requireDialog().window?.decorView.let { screenshot(it) }
    }
}
