/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.nextcloud.ui.ImageDetailFragment
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.activities.model.RichElement
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.activities.models.PreviewObject
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar

class FileDetailFragmentStaticServerIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    var file = getFile("gps.jpg")
    val oCFile = OCFile("/").apply {
        storagePath = file.absolutePath
        fileId = 12
        fileDataStorageManager.saveFile(this)
    }

    @Test
    @ScreenshotTest
    fun showFileDetailActivitiesFragment() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailActivitiesFragment.newInstance(oCFile, user))

        waitForIdleSync()
        shortSleep()
        shortSleep()
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun showFileDetailSharingFragment() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailSharingFragment.newInstance(oCFile, user))

        waitForIdleSync()
        shortSleep()
        shortSleep()
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun showFileDetailDetailsFragment() {
        val activity = testActivityRule.launchActivity(null)
        val sut = ImageDetailFragment.newInstance(oCFile, user)
        activity.addFragment(sut)

        shortSleep()
        shortSleep()
        waitForIdleSync()

        activity.runOnUiThread {
            sut.hideMap()
        }

        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showDetailsActivities() {
        val date = GregorianCalendar()
        date.set(2005, 4, 17, 10, 35, 30) // random date

        val richObjectList: ArrayList<RichObject> = ArrayList()
        richObjectList.add(RichObject("file", "abc", "text.txt", "/text.txt", "link", "tag"))
        richObjectList.add(RichObject("file", "1", "text.txt", "/text.txt", "link", "tag"))

        val previewObjectList1: ArrayList<PreviewObject> = ArrayList()
        previewObjectList1.add(PreviewObject(1, "source", "link", true, "text/plain", "view", "text.txt"))

        val richObjectList2: ArrayList<RichObject> = ArrayList()
        richObjectList2.add(RichObject("user", "admin", "Admin", "", "", ""))

        val activities = mutableListOf(
            Activity(
                1,
                date.time,
                date.time,
                "files",
                "file_changed",
                "user1",
                "user1",
                "You changed text.txt",
                "",
                "icon",
                "link",
                "files",
                "1",
                "/text.txt",
                previewObjectList1,
                RichElement("", richObjectList)
            ),
            Activity(
                2,
                date.time,
                date.time,
                "comments",
                "comments",
                "user1",
                "user1",
                "admin commented",
                "test2",
                "icon",
                "link",
                "files",
                "1",
                "/text.txt",
                emptyList(),
                RichElement()
            )
        )

        val sut = FileDetailFragment.newInstance(oCFile, user, 0)
        testActivityRule.launchActivity(null).apply {
            addFragment(sut)
            waitForIdleSync()
            runOnUiThread {
                sut.fileDetailActivitiesFragment.populateList(activities as List<Any>?, true)
            }
            longSleep()
            screenshot(sut.fileDetailActivitiesFragment.binding.swipeContainingList)
        }
    }

    // @Test
    // @ScreenshotTest
    fun showDetailsActivitiesNone() {
        val activity = testActivityRule.launchActivity(null)
        val sut = FileDetailFragment.newInstance(oCFile, user, 0)
        activity.addFragment(sut)

        waitForIdleSync()

        activity.runOnUiThread {
            sut.fileDetailActivitiesFragment.populateList(emptyList(), true)
        }

        shortSleep()
        shortSleep()
        screenshot(sut.fileDetailActivitiesFragment.binding.list)
    }

    @Test
    @ScreenshotTest
    fun showDetailsActivitiesError() {
        val activity = testActivityRule.launchActivity(null)
        val sut = FileDetailFragment.newInstance(oCFile, user, 0)
        activity.addFragment(sut)

        waitForIdleSync()

        activity.runOnUiThread {
            sut.fileDetailActivitiesFragment.disableLoadingActivities()
            sut
                .fileDetailActivitiesFragment
                .setErrorContent(targetContext.resources.getString(R.string.file_detail_activity_error))
        }

        shortSleep()
        shortSleep()
        screenshot(sut.fileDetailActivitiesFragment.binding.emptyList.emptyListView)
    }

    @Test
    @ScreenshotTest
    fun showDetailsSharing() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailFragment.newInstance(oCFile, user, 1))

        waitForIdleSync()

        shortSleep()
        shortSleep()
        screenshot(sut)
    }
}
