/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.EmptyListState
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import androidx.test.rule.GrantPermissionRule

class DrawerNavigationScreenshotIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.DrawerNavigationScreenshotIT"

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private fun isDrawerLayout(): Boolean = targetContext.resources.getBoolean(R.bool.is_support_drawer)

    private fun prepareStableFileListUi(activity: FileDisplayActivity) {
        activity.resetScrolling(true)
        activity.hideInfoBox()
        activity.listOfFilesFragment?.let {
            it.setFabEnabled(false)
            it.setEmptyListMessage(EmptyListState.LOADING)
            it.isLoading = false
        }
    }

    @Test
    @ScreenshotTest
    fun phoneAllFiles() {
        assumeFalse(isDrawerLayout())
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { prepareStableFileListUi(it) }
            val screenShotName = createName("${testClassName}_phoneAllFiles", "")
            onView(isRoot()).check(matches(isDisplayed()))
            scenario.onActivity { screenshotViaName(it, screenShotName) }
        }
    }

    @Test
    @ScreenshotTest
    fun phoneDrawerOpen() {
        assumeFalse(isDrawerLayout())
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { prepareStableFileListUi(it) }
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            val screenShotName = createName("${testClassName}_phoneDrawerOpen", "")
            onView(isRoot()).check(matches(isDisplayed()))
            scenario.onActivity { screenshotViaName(it, screenShotName) }
        }
    }

    @Test
    @ScreenshotTest
    fun phoneNavigateToFavorites() {
        assumeFalse(isDrawerLayout())
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { prepareStableFileListUi(it) }
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_favorites))
            val screenShotName = createName("${testClassName}_phoneNavigateToFavorites", "")
            onView(isRoot()).check(matches(isDisplayed()))
            scenario.onActivity { screenshotViaName(it, screenShotName) }
            // Return from the screen opened by drawer navigation before scenario teardown.
            pressBack()
        }
    }

    @Test
    @ScreenshotTest
    fun tabletAllFilesSidebar() {
        assumeTrue(isDrawerLayout())
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { prepareStableFileListUi(it) }
            val screenShotName = createName("${testClassName}_tabletAllFilesSidebar", "")
            onView(isRoot()).check(matches(isDisplayed()))
            scenario.onActivity { screenshotViaName(it, screenShotName) }
        }
    }

    @Test
    @ScreenshotTest
    fun tabletNavigateToGallery() {
        assumeTrue(isDrawerLayout())
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { prepareStableFileListUi(it) }
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_gallery))
            val screenShotName = createName("${testClassName}_tabletNavigateToGallery", "")
            onView(isRoot()).check(matches(isDisplayed()))
            scenario.onActivity { screenshotViaName(it, screenShotName) }
        }
    }
}

