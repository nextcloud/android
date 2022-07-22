/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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
package com.owncloud.android.ui.adapter

import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("MagicNumber")
class OCFileListAdapterIT : AbstractIT() {
    @Test
    fun testParseMedia() {
        // empty start
        storageManager.deleteAllFiles()

        val startDate: Long = 0
        val endDate: Long = 3642752043
        assertEquals(0, storageManager.getGalleryItems(startDate, endDate).size)

        // create dummy files
        OCFile("/test.txt").apply {
            mimeType = "text/plain"
        }.let {
            storageManager.saveFile(it)
        }

        OCFile("/image.png").apply {
            mimeType = "image/png"
            modificationTimestamp = 1000000
        }.let {
            storageManager.saveFile(it)
        }

        OCFile("/image2.png").apply {
            mimeType = "image/png"
            modificationTimestamp = 1000050
        }.let {
            storageManager.saveFile(it)
        }

        OCFile("/video.mpg").apply {
            mimeType = "video/mpg"
            modificationTimestamp = 1000045
        }.let {
            storageManager.saveFile(it)
        }

        OCFile("/video2.avi").apply {
            mimeType = "video/avi"
            modificationTimestamp = endDate + 10
        }.let {
            storageManager.saveFile(it)
        }

        // list of remoteFiles
        assertEquals(5, storageManager.allFiles.size)
        assertEquals(3, storageManager.getGalleryItems(startDate, endDate).size)
        assertEquals(4, storageManager.allGalleryItems.size)
    }
}
