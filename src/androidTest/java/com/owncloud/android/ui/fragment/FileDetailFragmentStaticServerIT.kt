/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.client.TestActivity
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

    val file = OCFile("/", "00000001")

    @Test
    @ScreenshotTest
    fun showFileDetailActivitiesFragment() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailActivitiesFragment.newInstance(file, user))

        waitForIdleSync()
        shortSleep()
        shortSleep()
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun showFileDetailSharingFragment() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailSharingFragment.newInstance(file, user))

        waitForIdleSync()
        shortSleep()
        shortSleep()
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showDetailsActivities() {
        val activity = testActivityRule.launchActivity(null)
        val sut = FileDetailFragment.newInstance(file, user, 0)
        activity.addFragment(sut)

        waitForIdleSync()

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
                null,
                null
            )
        )

        activity.runOnUiThread {
            sut.fileDetailActivitiesFragment.populateList(activities as List<Any>?, true)
        }

        shortSleep()
        shortSleep()
        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    fun showDetailsActivitiesNone() {
        val activity = testActivityRule.launchActivity(null)
        val sut = FileDetailFragment.newInstance(file, user, 0)
        activity.addFragment(sut)

        waitForIdleSync()

        activity.runOnUiThread {
            sut.fileDetailActivitiesFragment.populateList(emptyList(), true)
        }

        shortSleep()
        shortSleep()
        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    fun showDetailsActivitiesError() {
        val activity = testActivityRule.launchActivity(null)
        val sut = FileDetailFragment.newInstance(file, user, 0)
        activity.addFragment(sut)

        waitForIdleSync()

        activity.runOnUiThread {
            sut
                .fileDetailActivitiesFragment
                .setErrorContent(targetContext.resources.getString(R.string.file_detail_activity_error))
        }

        shortSleep()
        shortSleep()
        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    fun showDetailsSharing() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailFragment.newInstance(file, user, 1))

        waitForIdleSync()

        shortSleep()
        shortSleep()
        screenshot(sut)
    }
}
