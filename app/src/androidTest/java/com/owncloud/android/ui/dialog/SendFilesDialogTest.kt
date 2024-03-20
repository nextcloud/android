/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
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

    private lateinit var scenario: ActivityScenario<TestActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TestActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TestActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun showDialog() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val fm: FragmentManager = activity.supportFragmentManager
            val ft = fm.beginTransaction()
            ft.addToBackStack(null)

            val sut = SendFilesDialog.newInstance(FILES_SAME_TYPE)
            sut.show(ft, "TAG_SEND_SHARE_DIALOG")

            onIdleSync {
                shortSleep()
                val recyclerview: RecyclerView = sut.requireDialog().findViewById(R.id.send_button_recycler_view)
                Assert.assertNotNull("Adapter is null", recyclerview.adapter)
                Assert.assertNotEquals("Send button list is empty", 0, recyclerview.adapter!!.itemCount)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showDialog_Screenshot() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val fm: FragmentManager = activity.supportFragmentManager
            val ft = fm.beginTransaction()
            ft.addToBackStack(null)

            val sut = SendFilesDialog.newInstance(FILES_SAME_TYPE)
            sut.show(ft, "TAG_SEND_SHARE_DIALOG")

            onIdleSync {
                shortSleep()
                sut.requireDialog().window?.decorView.let { screenshot(it) }
            }
        }
    }

    @Test
    fun showDialogDifferentTypes() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val fm: FragmentManager = activity.supportFragmentManager
            val ft = fm.beginTransaction()
            ft.addToBackStack(null)

            val sut = SendFilesDialog.newInstance(FILES_MIXED_TYPE)
            sut.show(ft, "TAG_SEND_SHARE_DIALOG")

            onIdleSync {
                shortSleep()
                val recyclerview: RecyclerView = sut.requireDialog().findViewById(R.id.send_button_recycler_view)
                Assert.assertNotNull("Adapter is null", recyclerview.adapter)
                Assert.assertNotEquals("Send button list is empty", 0, recyclerview.adapter!!.itemCount)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showDialogDifferentTypes_Screenshot() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val fm: FragmentManager = activity.supportFragmentManager
            val ft = fm.beginTransaction()
            ft.addToBackStack(null)

            val sut = SendFilesDialog.newInstance(FILES_MIXED_TYPE)
            sut.show(ft, "TAG_SEND_SHARE_DIALOG")

            onIdleSync {
                shortSleep()
                sut.requireDialog().window?.decorView.let { screenshot(it) }
            }
        }
    }
}
