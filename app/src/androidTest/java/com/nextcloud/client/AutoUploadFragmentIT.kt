/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment.Companion.newInstance
import com.owncloud.android.ui.fragment.autoUpload.AutoUploadFragment
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class AutoUploadFragmentIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.AutoUploadFragmentIT"

    private fun findFragment(sut: NavigatorActivity): AutoUploadFragment =
        sut.supportFragmentManager.fragments.filterIsInstance<AutoUploadFragment>().firstOrNull()
            ?: throw IllegalStateException("AutoUploadFragment not found in NavigatorActivity!")

    private fun launch(block: (ActivityScenario<NavigatorActivity>) -> Unit) {
        val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.AutoUpload)
        ActivityScenario.launch<NavigatorActivity>(intent).use(block)
    }

    @Test
    @ScreenshotTest
    fun open() {
        launch { scenario ->
            val screenShotName = createName(testClassName + "_" + "open", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                val fragment = findFragment(sut)
                fragment.adapter.clear()
                val emptyListView = fragment.requireView().findViewById<View>(R.id.empty_list_view)
                screenshotViaName(emptyListView, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun testSyncedFolderDialog() {
        val item = SyncedFolderDisplayItem(
            1,
            "/sdcard/DCIM/",
            "/InstantUpload/",
            true,
            false,
            false,
            true,
            "test@https://nextcloud.localhost",
            0,
            0,
            true,
            1000,
            "Name",
            MediaFolderType.IMAGE,
            false,
            SubFolderRule.YEAR_MONTH,
            false,
            SyncedFolder.NOT_SCANNED_YET
        )

        launch { scenario ->
            val screenShotName = createName(testClassName + "_" + "testSyncedFolderDialog", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                val dialog = newInstance(item, 0) ?: throw IllegalStateException("Dialog could not be created")
                dialog.show(findFragment(sut).childFragmentManager, "")
                findFragment(sut).childFragmentManager.executePendingTransactions()
                screenshot(dialog.requireDialog().window?.decorView, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showPowerCheckDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        launch { scenario ->
            var dialog: AlertDialog? = null
            scenario.onActivity { sut ->
                val fragment = findFragment(sut)
                dialog = fragment.buildPowerCheckDialog()
                fragment.showPowerCheckDialog()
            }

            val screenShotName = createName(testClassName + "_" + "showPowerCheckDialog", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshot(dialog?.window?.decorView, screenShotName)
        }
    }
}
