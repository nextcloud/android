/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar

class FileDetailFragmentStaticServerIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<TestActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TestActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TestActivity>(intent)

    private var file = getFile("gps.jpg")
    private val oCFile = OCFile("/").apply {
        storagePath = file.absolutePath
        fileId = 12
        fileDataStorageManager.saveFile(this)
    }

    @Test
    @ScreenshotTest
    fun showFileDetailActivitiesFragment() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.addFragment(FileDetailActivitiesFragment.newInstance(oCFile, user))
            onIdleSync {
                shortSleep()
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showFileDetailSharingFragment() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.addFragment(FileDetailSharingFragment.newInstance(oCFile, user))
            onIdleSync {
                shortSleep()
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showFileDetailDetailsFragment() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val sut = ImageDetailFragment.newInstance(oCFile, user)
            activity.addFragment(sut)

            shortSleep()
            shortSleep()

            onIdleSync {
                activity.runOnUiThread {
                    sut.hideMap()
                }

                screenshot(activity)
            }
        }
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
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            activity.addFragment(sut)
            onIdleSync {
                activity.runOnUiThread {
                    sut.fileDetailActivitiesFragment.populateList(activities as List<Any>?, true)
                }
                longSleep()
                screenshot(sut.fileDetailActivitiesFragment.binding.swipeContainingList)
            }
        }
    }

    // @Test
    // @ScreenshotTest
    fun showDetailsActivitiesNone() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val sut = FileDetailFragment.newInstance(oCFile, user, 0)
            activity.addFragment(sut)

            onIdleSync {
                activity.runOnUiThread {
                    sut.fileDetailActivitiesFragment.populateList(emptyList(), true)
                }

                shortSleep()
                shortSleep()
                screenshot(sut.fileDetailActivitiesFragment.binding.list)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showDetailsActivitiesError() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val sut = FileDetailFragment.newInstance(oCFile, user, 0)
            activity.addFragment(sut)

            onIdleSync {
                sut.fileDetailActivitiesFragment.run {
                    disableLoadingActivities()
                    setErrorContent(targetContext.resources.getString(R.string.file_detail_activity_error))
                }

                shortSleep()
                shortSleep()
                screenshot(sut.fileDetailActivitiesFragment.binding.emptyList.emptyListView)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showDetailsSharing() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.addFragment(FileDetailFragment.newInstance(oCFile, user, 1))
            onIdleSync {
                shortSleep()
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @After
    fun cleanup() {
        scenario.close()
    }
}
