/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.test.GrantStoragePermissionRule.Companion.grant
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.fragment.notifications.NotificationsFragment
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.util.Date
import java.util.GregorianCalendar

class NotificationsFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.notifications.NotificationsFragmentIT"

    @get:Rule
    var storagePermissionRules: TestRule = grant()

    private fun buildDate(): Date {
        val cal = GregorianCalendar()
        cal.set(2005, 4, 17, 10, 35, 30)
        return cal.time
    }

    private fun buildNotificationNoActions(): Notification = Notification(
        1,
        "files",
        "user",
        buildDate(),
        "objectType",
        "objectId",
        "App recommendation: Tasks",
        "SubjectRich",
        HashMap(),
        "Sync tasks from various devices with your Nextcloud and edit them online.",
        "MessageRich",
        HashMap(),
        "link",
        "icon",
        ArrayList()
    )

    private fun buildNotificationTwoActions(): Notification {
        val actions = ArrayList<Action>()
        actions.add(Action("Send usage", "link", "url", true))
        actions.add(Action("Not now", "link", "url", false))

        return Notification(
            2,
            "files",
            "user",
            buildDate(),
            "objectType",
            "objectId",
            "Help improve Nextcloud",
            "SubjectRich",
            HashMap(),
            "Do you want to help us to improve Nextcloud by providing some anonymize data about your setup and usage?",
            "MessageRich",
            HashMap(),
            "link",
            "icon",
            actions
        )
    }

    private fun buildNotificationManyActions(): Notification {
        val actions = ArrayList<Action>()
        actions.add(Action("Send usage", "link", "url", true))
        actions.add(Action("Not now", "link", "url", false))
        actions.add(Action("Third action", "link", "url", false))
        actions.add(Action("Delay", "link", "url", false))

        return Notification(
            3,
            "files",
            "user",
            buildDate(),
            "objectType",
            "objectId",
            "Help improve Nextcloud",
            "SubjectRich",
            HashMap(),
            "Do you want to help us to improve Nextcloud by providing some anonymize data about your setup" +
                " and usage?",
            "MessageRich",
            HashMap(),
            "link",
            "icon",
            actions
        )
    }

    fun buildMockNotifications(): ArrayList<Notification> = ArrayList<Notification>().apply {
        add(buildNotificationNoActions())
        add(buildNotificationTwoActions())
        add(buildNotificationManyActions())
    }

    private fun findFragment(sut: NavigatorActivity): NotificationsFragment? = sut.supportFragmentManager
        .findFragmentByTag(NotificationsFragment::class.java.simpleName) as? NotificationsFragment

    @Test
    @ScreenshotTest
    fun empty() {
        val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.Notifications)
        ActivityScenario.launch<NavigatorActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                findFragment(sut)?.populateList(ArrayList())
            }

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            val screenShotName = createName(testClassName + "_" + "empty", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showNotifications() {
        val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.Notifications)
        ActivityScenario.launch<NavigatorActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                findFragment(sut)?.populateList(buildMockNotifications())
            }

            val screenShotName = createName(testClassName + "_" + "showNotifications", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.Notifications)
        ActivityScenario.launch<NavigatorActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                findFragment(sut)?.setEmptyContent("Error", "Error! Please try again later!")
            }

            val screenShotName = createName(testClassName + "_" + "error", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }
}
