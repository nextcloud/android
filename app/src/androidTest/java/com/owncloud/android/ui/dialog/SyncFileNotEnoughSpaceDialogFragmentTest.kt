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
package com.owncloud.android.ui.dialog

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Rule
import org.junit.Test
import java.util.Objects

class SyncFileNotEnoughSpaceDialogFragmentTest : AbstractIT() {
    @Rule
    var activityRule = IntentsTestRule(
        FileDisplayActivity::class.java,
        true,
        false
    )

    @Test
    fun showNotEnoughSpaceDialogForFolder() {
        val test = activityRule.launchActivity(null)
        val ocFile = OCFile("/Document/")
        ocFile.fileLength = 5000000
        val dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 1000)
        dialog.show(test.listOfFilesFragment!!.fragmentManager!!, "1")
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        screenshot(Objects.requireNonNull(dialog.requireDialog().window)!!.decorView)
    }

    @Test
    fun showNotEnoughSpaceDialogForFile() {
        val test = activityRule.launchActivity(null)
        val ocFile = OCFile("/Video.mp4")
        ocFile.fileLength = 1000000
        val dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 2000)
        dialog.show(test.listOfFilesFragment!!.fragmentManager!!, "2")
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        screenshot(Objects.requireNonNull(dialog.requireDialog().window)!!.decorView)
    }
}