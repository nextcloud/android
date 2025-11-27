/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.lib.resources.notifications.models.RichObject
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.GregorianCalendar

class NotificationsActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.NotificationsActivityIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @ScreenshotTest
    fun empty() {
        launchActivity<NotificationsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.populateList(ArrayList())
                    EspressoIdlingResource.decrement()
                    val screenShotName = createName(testClassName + "_" + "empty", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    @SuppressWarnings("MagicNumber")
    fun showNotifications() {
        val date = GregorianCalendar()
        date.set(2005, 4, 17, 10, 35, 30) // random date

        val notifications = ArrayList<Notification>()
        notifications.add(
            Notification(
                1,
                "files",
                "user",
                date.time,
                "objectType",
                "objectId",
                "App recommendation: Tasks",
                "SubjectRich",
                HashMap<String, RichObject>(),
                "Sync tasks from various devices with your Nextcloud and edit them online.",
                "MessageRich",
                HashMap<String, RichObject>(),
                "link",
                "icon",
                ArrayList<Action>()
            )
        )

        val actions = ArrayList<Action>().apply {
            add(Action("Send usage", "link", "url", true))
            add(Action("Not now", "link", "url", false))
        }

        notifications.add(
            Notification(
                1,
                "files",
                "user",
                date.time,
                "objectType",
                "objectId",
                "Help improve Nextcloud",
                "SubjectRich",
                HashMap<String, RichObject>(),
                "Do you want to help us to improve Nextcloud" +
                    " by providing some anonymize data about your setup and usage?",
                "MessageRich",
                HashMap<String, RichObject>(),
                "link",
                "icon",
                actions
            )
        )

        val moreAction = ArrayList<Action>().apply {
            add(Action("Send usage", "link", "url", true))
            add(Action("Not now", "link", "url", false))
            add(Action("third action", "link", "url", false))
            add(Action("Delay", "link", "url", false))
        }

        notifications.add(
            Notification(
                2,
                "files",
                "user",
                date.time,
                "objectType",
                "objectId",
                "Help improve Nextcloud",
                "SubjectRich",
                HashMap<String, RichObject>(),
                "Do you want to help us to improve Nextcloud by providing some anonymize data about your setup and " +
                    "usage?",
                "MessageRich",
                HashMap<String, RichObject>(),
                "link",
                "icon",
                moreAction
            )
        )

        launchActivity<NotificationsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.populateList(notifications)
                    EspressoIdlingResource.decrement()
                    val screenShotName = createName(testClassName + "_" + "showNotifications", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        launchActivity<NotificationsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.setEmptyContent("Error", "Error! Please try again later!")
                    EspressoIdlingResource.decrement()
                    val screenShotName = createName(testClassName + "_" + "error", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
