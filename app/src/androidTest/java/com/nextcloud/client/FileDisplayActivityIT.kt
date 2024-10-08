/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
import com.owncloud.android.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FileDisplayActivityIT : AbstractOnServerIT() {

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @get:Rule
    val retryRule = RetryTestRule()

    @Suppress("DEPRECATION")
    @Test
    fun showShares() {
        EspressoIdlingResource.increment()

        Assert.assertTrue(ExistenceCheckRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/shareToGroup/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/shareViaLink/", true).execute(client).isSuccess)
        Assert.assertTrue(CreateFolderRemoteOperation("/noShare/", true).execute(client).isSuccess)

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

        EspressoIdlingResource.decrement()

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { _ ->
                onIdleSync {
                    // open drawer
                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

                    // click "shared"
                    onView(withId(R.id.nav_view))
                        .perform(NavigationViewActions.navigateTo(R.id.nav_shared))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun allFiles() {
        EspressoIdlingResource.increment()

        Assert.assertTrue(
            CreateFolderOperation("/test/", user, targetContext, storageManager)
                .execute(client)
                .isSuccess
        )

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val test = storageManager.getFileByPath("/test/")
                    sut.file = test
                    Assert.assertEquals(storageManager.getFileByPath("/test/"), sut.currentDir)

                    sut.startSyncFolderOperation(test, false)
                    EspressoIdlingResource.decrement()

                    // open drawer
                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

                    // click "all files"
                    onView(withId(R.id.nav_view))
                        .perform(NavigationViewActions.navigateTo(R.id.nav_all_files))

                    Assert.assertEquals(storageManager.getFileByPath("/"), sut.currentDir)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun checkToolbarTitleOnNavigation() {
        EspressoIdlingResource.increment()

        val topFolder = "folder1"
        val childFolder = "folder2"

        CreateFolderOperation("/$topFolder/", user, targetContext, storageManager)
            .execute(client)

        CreateFolderOperation("/$topFolder/$childFolder/", user, targetContext, storageManager)
            .execute(client)

        EspressoIdlingResource.decrement()

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { _ ->
                onIdleSync {
                    // go into "foo"
                    onView(withText(topFolder)).perform(click())

                    // check title is right
                    checkToolbarTitle(topFolder)

                    // go into "bar"
                    onView(withText(childFolder)).perform(click())

                    // check title is right
                    checkToolbarTitle(childFolder)

                    // browse back up, we should be back in "foo"
                    Espresso.pressBack()

                    // check title is right
                    checkToolbarTitle(topFolder)
                }
            }
        }
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

    @Suppress("DEPRECATION")
    @Test
    fun browseFavoriteAndBack() {
        EspressoIdlingResource.increment()

        val topFolder = "folder1"

        CreateFolderOperation("/$topFolder/", user, targetContext, storageManager)
            .execute(client)
        ToggleFavoriteRemoteOperation(true, "/$topFolder/")
            .execute(client)

        EspressoIdlingResource.decrement()

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    // navigate to favorites
                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
                    onView(withId(R.id.nav_view))
                        .perform(NavigationViewActions.navigateTo(R.id.nav_favorites))

                    // check sort button is not shown, favorites are not sortable
                    onView(
                        withId(R.id.sort_button)
                    ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

                    // browse into folder
                    onView(withId(R.id.list_root))
                        .perform(closeSoftKeyboard())
                        .perform(
                            RecyclerViewActions.actionOnItemAtPosition<OCFileListItemViewHolder>(
                                0,
                                click()
                            )
                        )
                    checkToolbarTitle(topFolder)
                    // sort button should now be visible
                    onView(withId(R.id.sort_button)).check(matches(isDisplayed()))

                    // browse back, should be back to All Files
                    Espresso.pressBack()
                    checkToolbarTitle(sut.getString(R.string.app_name))
                    onView(withId(R.id.sort_button)).check(matches(isDisplayed()))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun switchToGridView() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { _ ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    Assert.assertTrue(
                        CreateFolderOperation("/test/", user, targetContext, storageManager)
                            .execute(client)
                            .isSuccess
                    )
                    EspressoIdlingResource.decrement()
                    onView(withId(R.id.switch_grid_view_button)).perform(click())
                }
            }
        }
    }

    @Test
    fun openAccountSwitcher() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { _ ->
                onIdleSync {
                    onView(withId(R.id.switch_account_button)).perform(click())
                }
            }
        }
    }
}
