/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.view.View
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.activities.model.RichElement
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.activities.models.PreviewObject
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activities.ActivitiesActivity
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test
import java.util.GregorianCalendar

class ActivitiesActivityIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.ActivitiesActivityIT"

    @Test
    @ScreenshotTest
    fun openDrawer() {
        launchActivity<ActivitiesActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.dismissSnackbar()
            }

            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

            scenario.onActivity { sut ->
                val screenShotName = createName("${testClassName}_openDrawer", "")
                screenshotViaName(sut, screenShotName)
            }

            onView(withId(R.id.drawer_layout)).check(matches(isDisplayed()))
        }
    }

    @Test
    @ScreenshotTest
    fun loading() {
        launchActivity<ActivitiesActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.dismissSnackbar()
                sut.binding.emptyList.root.visibility = View.GONE
                sut.binding.swipeContainingList.visibility = View.GONE
                sut.binding.loadingContent.visibility = View.VISIBLE
            }

            val screenShotName = createName(testClassName + "_" + "loading", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun empty() {
        launchActivity<ActivitiesActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.showActivities(mutableListOf(), nextcloudClient, -1)
                sut.setProgressIndicatorState(false)
                sut.dismissSnackbar()
            }

            val screenShotName = createName(testClassName + "_" + "empty", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
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
                ArrayList(),
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

        launchActivity<ActivitiesActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.showActivities(activities as List<Any>?, nextcloudClient, -1)
                sut.setProgressIndicatorState(false)
                sut.dismissSnackbar()
            }

            val screenShotName = createName(testClassName + "_" + "showActivities", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        launchActivity<ActivitiesActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.showEmptyContent("Error", "Error! Please try again later!")
                sut.setProgressIndicatorState(false)
                sut.dismissSnackbar()
            }

            val screenShotName = createName(testClassName + "_" + "error", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }
}
