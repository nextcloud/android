/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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
package com.nextcloud.client

import android.app.Activity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.OCFileListItemViewHolder
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class FileDisplayActivityIT : AbstractOnServerIT() {
    @get:Rule
    val activityRule = IntentsTestRule(
        FileDisplayActivity::class.java,
        true,
        false
    )

    @get:Rule
    val retryRule = RetryTestRule() // showShares is flaky

    // @ScreenshotTest // todo run without real server
    @Test
    fun showShares() {
        Assert.assertTrue(ExistenceCheckRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/shareToGroup/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/shareViaLink/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/noShare/", true).execute(client).isSuccess)
        // assertTrue(new CreateFolderRemoteOperation("/shareToCircle/", true).execute(client).isSuccess());

        // share folder to user "admin"
        Assert.assertTrue(
            CreateShareRemoteOperation(
                "/shareToAdmin/",
                ShareType.USER,
                "admin",
                false,
                "",
                OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            ).execute(client).isSuccess
        )

        // share folder via public link
        Assert.assertTrue(
            CreateShareRemoteOperation(
                "/shareViaLink/",
                ShareType.PUBLIC_LINK,
                "",
                true,
                "",
                OCShare.READ_PERMISSION_FLAG
            ).execute(client).isSuccess
        )

        // share folder to group
        Assert.assertTrue(
            CreateShareRemoteOperation(
                "/shareToGroup/",
                ShareType.GROUP,
                "users",
                false,
                "",
                OCShare.NO_PERMISSION
            ).execute(client).isSuccess
        )

        // share folder to circle
        // get share
//        RemoteOperationResult searchResult = new GetShareesRemoteOperation("publicCircle", 1, 50).execute(client);
//        assertTrue(searchResult.getLogMessage(), searchResult.isSuccess());
//
//        JSONObject resultJson = (JSONObject) searchResult.getData().get(0);
//        String circleId = resultJson.getJSONObject("value").getString("shareWith");
//
//        assertTrue(new CreateShareRemoteOperation("/shareToCircle/",
//                                                  ShareType.CIRCLE,
//                                                  circleId,
//                                                  false,
//                                                  "",
//                                                  OCShare.DEFAULT_PERMISSION)
//                       .execute(client).isSuccess());

        val sut: Activity = activityRule.launchActivity(null)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // open drawer
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

        // click "shared"
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_shared))
        shortSleep()
        shortSleep()
        screenshot(sut)
    }

    @Test
    fun allFiles() {
        val sut = activityRule.launchActivity(null)

        // given test folder
        Assert.assertTrue(
            CreateFolderOperation("/test/", user, targetContext, storageManager)
                .execute(client)
                .isSuccess
        )

        // navigate into it
        val test = storageManager.getFileByPath("/test/")
        sut.file = test
        sut.startSyncFolderOperation(test, false)
        Assert.assertEquals(storageManager.getFileByPath("/test/"), sut.currentDir)

        // open drawer
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

        // click "all files"
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_all_files))

        // then should be in root again
        shortSleep()
        Assert.assertEquals(storageManager.getFileByPath("/"), sut.currentDir)
    }

    @Test
    fun checkToolbarTitleOnNavigation() {
        // Create folder structure
        val topFolder = "folder1"
        val childFolder = "folder2"

        CreateFolderOperation("/$topFolder/", user, targetContext, storageManager)
            .execute(client)

        CreateFolderOperation("/$topFolder/$childFolder/", user, targetContext, storageManager)
            .execute(client)

        activityRule.launchActivity(null)

        shortSleep()

        // go into "foo"
        onView(withText(topFolder)).perform(click())
        shortSleep()

        // check title is right
        checkToolbarTitle(topFolder)

        // go into "bar"
        onView(withText(childFolder)).perform(click())
        shortSleep()

        // check title is right
        checkToolbarTitle(childFolder)

        // browse back up, we should be back in "foo"
        Espresso.pressBack()
        shortSleep()

        // check title is right
        checkToolbarTitle(topFolder)
    }

    private fun checkToolbarTitle(childFolder: String) {
        onView(withId(R.id.appbar)).check(
            matches(
                hasDescendant(
                    withText(childFolder)
                )
            )
        )
    }

    @Test
    fun browseFavoriteAndBack() {
        // Create folder structure
        val topFolder = "folder1"

        CreateFolderOperation("/$topFolder/", user, targetContext, storageManager)
            .execute(client)
        ToggleFavoriteRemoteOperation(true, "/$topFolder/")
            .execute(client)

        val sut = activityRule.launchActivity(null)

        // navigate to favorites
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_favorites))
        shortSleep()

        // check sort button is not shown, favorites are not sortable
        onView(withId(R.id.sort_button)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // browse into folder
        onView(withId(R.id.list_root))
            .perform(closeSoftKeyboard())
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<OCFileListItemViewHolder>(
                    0,
                    click()
                )
            )
        shortSleep()
        checkToolbarTitle(topFolder)
        // sort button should now be visible
        onView(withId(R.id.sort_button)).check(matches(ViewMatchers.isDisplayed()))

        // browse back, should be back to All Files
        Espresso.pressBack()
        checkToolbarTitle(sut.getString(R.string.app_name))
        onView(withId(R.id.sort_button)).check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun switchToGridView() {
        activityRule.launchActivity(null)
        Assert.assertTrue(
            CreateFolderOperation("/test/", user, targetContext, storageManager)
                .execute(client)
                .isSuccess
        )
        onView(withId(R.id.switch_grid_view_button)).perform(click())
    }

    @Test
    fun openAccountSwitcher() {
        activityRule.launchActivity(null)
        onView(withId(R.id.switch_account_button)).perform(click())
    }
}
