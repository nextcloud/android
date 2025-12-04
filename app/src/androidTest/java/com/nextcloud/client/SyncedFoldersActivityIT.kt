/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.content.Intent
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment.Companion.newInstance
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class SyncedFoldersActivityIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.SyncedFoldersActivityIT"

    @Test
    @ScreenshotTest
    fun open() {
        launchActivity<SyncedFoldersActivity>().use { scenario ->
            val screenShotName = createName(testClassName + "_" + "open", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                sut.adapter.clear()
                screenshotViaName(sut.binding.emptyList.emptyListView, screenShotName)
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

        val intent = Intent(targetContext, SyncedFoldersActivity::class.java)
        launchActivity<SyncedFoldersActivity>(intent).use { scenario ->
            val screenShotName = createName(testClassName + "_" + "testSyncedFolderDialog", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                val fragment = newInstance(item, 0)
                fragment!!.show(sut.supportFragmentManager, "")
                screenshot(fragment.requireDialog().window?.decorView, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showPowerCheckDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val intent = Intent(targetContext, SyncedFoldersActivity::class.java)

        launchActivity<SyncedFoldersActivity>(intent).use { scenario ->
            var dialog: AlertDialog? = null
            scenario.onActivity { sut ->
                dialog = sut.buildPowerCheckDialog()
                sut.showPowerCheckDialog()
            }

            val screenShotName = createName(testClassName + "_" + "showPowerCheckDialog", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshot(dialog!!.window?.decorView, screenShotName)
        }
    }
}
