/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.facebook.testing.screenshot.Screenshot
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.activities.model.RichElement
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.activities.models.PreviewObject
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activities.ActivitiesActivity
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar

class ActivitiesActivityIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<ActivitiesActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), ActivitiesActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<ActivitiesActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun openDrawer() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            shortSleep()
            Espresso.onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            sut.runOnUiThread {
                sut.dismissSnackbar()
            }
            shortSleep()
            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun loading() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.runOnUiThread {
                sut.dismissSnackbar()
                sut.binding.emptyList.root.visibility = View.GONE
                sut.binding.swipeContainingList.visibility = View.GONE
                sut.binding.loadingContent.visibility = View.VISIBLE
            }

            shortSleep()
            onIdleSync {
                Screenshot.snap(sut.binding.loadingContent).record()
            }
        }
    }

    @Test
    @ScreenshotTest
    fun empty() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.runOnUiThread {
                sut.showActivities(mutableListOf(), nextcloudClient, -1)
                sut.setProgressIndicatorState(false)
                sut.dismissSnackbar()
            }
            shortSleep()
            onIdleSync {
                screenshot(sut.binding.emptyList.emptyListView)
            }
        }
    }

    @Test
    @ScreenshotTest
    @SuppressWarnings("MagicNumber")
    fun showActivities() {
        val capability = OCCapability()
        capability.versionMayor = 20
        fileDataStorageManager.saveCapabilities(capability)

        val date = GregorianCalendar()
        date.set(2005, 4, 17, 10, 35, 30) // random date

        val richObjectList: ArrayList<RichObject> = ArrayList()
        richObjectList.add(RichObject("file", "abc", "text.txt", "/text.txt", "link", "tag"))
        richObjectList.add(RichObject("file", "1", "text.txt", "/text.txt", "link", "tag"))

        val previewObjectList1: ArrayList<PreviewObject> = ArrayList()
        previewObjectList1.add(PreviewObject(1, "source", "link", true, "text/plain", "view", "test1.txt"))

        val previewObjectList3: ArrayList<PreviewObject> = ArrayList()
        previewObjectList3.add(PreviewObject(1, "source", "link", true, "image/jpg", "view", "test1.jpg"))

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
                1,
                date.time,
                date.time,
                "dav",
                "calendar_event",
                "user1",
                "user1",
                "You have deleted calendar entry Appointment",
                "",
                "icon",
                "link",
                "calendar",
                "35",
                "",
                ArrayList<PreviewObject>(),
                RichElement()
            ),
            Activity(
                1,
                date.time,
                date.time,
                "files",
                "file_changed",
                "user1",
                "user1",
                "You changed image.jpg",
                "",
                "icon",
                "link",
                "files",
                "1",
                "/image.jpg",
                previewObjectList3,
                RichElement("", richObjectList)
            )
        )

        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.runOnUiThread {
                sut.showActivities(activities as List<Any>?, nextcloudClient, -1)
                sut.setProgressIndicatorState(false)
                sut.dismissSnackbar()
            }

            longSleep()

            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            shortSleep()

            sut.runOnUiThread {
                sut.showEmptyContent("Error", "Error! Please try again later!")
                sut.setProgressIndicatorState(false)
                sut.dismissSnackbar()
            }

            shortSleep()
            shortSleep()
            onIdleSync {
                screenshot(sut)
            }
        }
    }
}
