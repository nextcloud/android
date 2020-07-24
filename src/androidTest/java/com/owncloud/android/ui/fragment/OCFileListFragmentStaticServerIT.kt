/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.owncloud.android.ui.fragment

import android.Manifest
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.GrantPermissionRule
import com.facebook.testing.screenshot.Screenshot
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class OCFileListFragmentStaticServerIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    @ScreenshotTest
    fun showFiles() {
        val sut = testActivityRule.launchActivity(null)

        val textFile = OCFile("/1.md", "00000001")
        textFile.mimeType = "text/markdown"
        textFile.fileLength = 1024000
        textFile.modificationTimestamp = 1188206955000
        textFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        sut.storageManager.saveFile(textFile)

        val imageFile = OCFile("/image.png", "00000002")
        imageFile.mimeType = "image/png"
        imageFile.isPreviewAvailable = true
        imageFile.fileLength = 3072000
        imageFile.modificationTimestamp = 746443755000
        imageFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        sut.storageManager.saveFile(imageFile)

        sut.addFragment(OCFileListFragment())

        val fragment = (sut.fragment as OCFileListFragment)
        val root = sut.storageManager.getFileByEncryptedRemotePath("/")

        shortSleep()

        sut.runOnUiThread { fragment.listDirectory(root, false, false) }

        waitForIdleSync()
        shortSleep()
        shortSleep()
        shortSleep()

        Screenshot.snapActivity(sut).record()
    }
}
