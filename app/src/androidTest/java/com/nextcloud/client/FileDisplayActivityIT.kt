/*
 * Nextcloud - Android Client
 *
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    val retryRule = RetryTestRule() // showShares is flaky

    @Suppress("DEPRECATION")
    @Test
    fun showShares() {
        assertTrue(ExistenceCheckRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess)
        assertTrue(CreateFolderRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess)
        assertTrue(CreateFolderRemoteOperation("/shareToGroup/", true).execute(client).isSuccess)
        assertTrue(CreateFolderRemoteOperation("/shareViaLink/", true).execute(client).isSuccess)
        assertTrue(CreateFolderRemoteOperation("/noShare/", true).execute(client).isSuccess)

        // share folder to user "admin"
        assertTrue(
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
        assertTrue(
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
        assertTrue(
            CreateShareRemoteOperation(
                "/shareToGroup/",
                ShareType.GROUP,
                "users",
                false,
                "",
                OCShare.NO_PERMISSION
            ).execute(client).isSuccess
        )

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
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
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    // given test folder
                    assertTrue(
                        CreateFolderOperation("/test/", user, targetContext, storageManager)
                            .execute(client)
                            .isSuccess
                    )

                    // navigate into it
                    val test = storageManager.getFileByPath("/test/")
                    sut.file = test
                    sut.startSyncFolderOperation(test, false)
                    assertEquals(storageManager.getFileByPath("/test/"), sut.currentDir)
                    EspressoIdlingResource.decrement()

                    // open drawer
                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

                    // click "all files"
                    onView(withId(R.id.nav_view))
                        .perform(NavigationViewActions.navigateTo(R.id.nav_all_files))

                    // then should be in root again
                    assertEquals(storageManager.getFileByPath("/"), sut.currentDir)
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
        // Create folder structure
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
                    checkToolbarTitle(topFolder)
                    // sort button should now be visible
                    onView(withId(R.id.sort_button)).check(matches(ViewMatchers.isDisplayed()))

                    // browse back, should be back to All Files
                    Espresso.pressBack()
                    checkToolbarTitle(sut.getString(R.string.app_name))
                    onView(withId(R.id.sort_button)).check(matches(ViewMatchers.isDisplayed()))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun switchToGridView() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    assertTrue(
                        CreateFolderOperation("/test/", user, targetContext, storageManager)
                            .execute(client)
                            .isSuccess
                    )
                    onView(withId(R.id.switch_grid_view_button)).perform(click())
                }
            }
        }
    }

    @Test
    fun openAccountSwitcher() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    onView(withId(R.id.switch_account_button)).perform(click())
                }
            }
        }
    }
}
