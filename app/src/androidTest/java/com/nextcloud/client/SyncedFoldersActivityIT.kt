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
package com.nextcloud.client

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import java.util.Objects

class SyncedFoldersActivityIT : AbstractIT() {
    @Rule
    var activityRule = IntentsTestRule(
        SyncedFoldersActivity::class.java,
        true,
        false
    )

    @Test
    @ScreenshotTest
    fun open() {
        val sut: Activity = activityRule.launchActivity(null)
        screenshot(sut)
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
            SubFolderRule.YEAR_MONTH
        )
        val sut = SyncedFolderPreferencesDialogFragment.newInstance(item, 0)
        val intent = Intent(targetContext, SyncedFoldersActivity::class.java)
        val activity = activityRule.launchActivity(intent)
        sut.show(activity.supportFragmentManager, "")
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()
        screenshot(Objects.requireNonNull(sut.requireDialog().window)?.decorView)
    }
}