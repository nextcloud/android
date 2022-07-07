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
package com.owncloud.android.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileStorageUtils.checkIfEnoughSpace
import com.owncloud.android.utils.FileStorageUtils.pathToUserFriendlyDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileStorageUtilsIT : AbstractIT() {
    private fun openFile(name: String): File {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val externalFilesDir = ctx.getExternalFilesDir(null)
        return File(externalFilesDir, name)
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithoutLocalFile() {
        val ocFile = OCFile("/test.txt")
        val file = openFile("test.txt")
        file.createNewFile()

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithLocalFile() {
        val ocFile = OCFile("/test.txt")
        val file = openFile("test.txt")
        file.writeText("123123")

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithoutLocalFolder() {
        val ocFile = OCFile("/test/")
        val file = openFile("test")
        File(file, "1.txt").writeText("123123")

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithLocalFolder() {
        val ocFile = OCFile("/test/")
        val folder = openFile("test")
        folder.mkdirs()
        val file = File(folder, "1.txt")
        file.createNewFile()
        file.writeText("123123")

        ocFile.storagePath = folder.absolutePath
        ocFile.mimeType = "DIR"

        ocFile.fileLength = 100
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 44
        assertTrue(checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertTrue(checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithNoLocalFolder() {
        val ocFile = OCFile("/test/")

        ocFile.mimeType = "DIR"

        ocFile.fileLength = 100
        assertTrue(checkIfEnoughSpace(200L, ocFile))
    }

    @Test
    fun testPathToUserFriendlyDisplay() {
        assertEquals("/", pathToUserFriendlyDisplay("/"))
        assertEquals("/sdcard/", pathToUserFriendlyDisplay("/sdcard/"))
        assertEquals("/sdcard/test/1/", pathToUserFriendlyDisplay("/sdcard/test/1/"))
        assertEquals("Internal storage/Movies/", pathToUserFriendlyDisplay("/storage/emulated/0/Movies/"))
        assertEquals("Internal storage/", pathToUserFriendlyDisplay("/storage/emulated/0/"))
    }

    private fun pathToUserFriendlyDisplay(path: String): String {
        return pathToUserFriendlyDisplay(path, targetContext, targetContext.resources)
    }
}
