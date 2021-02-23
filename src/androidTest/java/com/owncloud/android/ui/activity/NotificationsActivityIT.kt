/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
package com.owncloud.android.ui.activity

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.lib.resources.notifications.models.RichObject
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar

class NotificationsActivityIT : AbstractIT() {
    @get:Rule
    var activityRule = IntentsTestRule(NotificationsActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun loading() {
        val sut: NotificationsActivity = activityRule.launchActivity(null)

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun empty() {
        val sut: NotificationsActivity = activityRule.launchActivity(null)

        waitForIdleSync()

        sut.runOnUiThread { sut.populateList(ArrayList<Notification>()) }

        shortSleep()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    @SuppressWarnings("MagicNumber")
    fun showNotifications() {
        val sut: NotificationsActivity = activityRule.launchActivity(null)

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

        val actions = ArrayList<Action>()
        actions.add(Action("Send usage", "link", "url", true))
        actions.add(Action("Not now", "link", "url", false))

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

        sut.runOnUiThread { sut.populateList(notifications) }

        shortSleep()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun error() {
        val sut: NotificationsActivity = activityRule.launchActivity(null)

        shortSleep()

        sut.runOnUiThread { sut.setEmptyContent("Error", "Error! Please try again later!") }

        screenshot(sut)
    }
}
