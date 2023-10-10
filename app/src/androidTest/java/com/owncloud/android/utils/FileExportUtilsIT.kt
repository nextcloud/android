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

package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileExportUtilsIT : AbstractIT() {
    @Test
    fun exportFile() {
        val file = createFile("export.txt", 10)

        val sut = FileExportUtils()

        val expectedFile = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File("/sdcard/Downloads/export.txt")
        } else {
            File("/storage/emulated/0/Download/export.txt")
        }

        assertFalse(expectedFile.exists())

        sut.exportFile("export.txt", "/text/plain", targetContext.contentResolver, null, file)

        assertTrue(expectedFile.exists())
        assertEquals(file.length(), expectedFile.length())
        assertTrue(expectedFile.delete())
    }

    @Test
    fun exportOCFile() {
        val file = createFile("export.txt", 10)
        val ocFile = OCFile("/export.txt").apply {
            storagePath = file.absolutePath
        }

        val sut = FileExportUtils()

        val expectedFile = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File("/sdcard/Downloads/export.txt")
        } else {
            File("/storage/emulated/0/Download/export.txt")
        }

        assertFalse(expectedFile.exists())

        sut.exportFile("export.txt", "/text/plain", targetContext.contentResolver, ocFile, null)

        assertTrue(expectedFile.exists())
        assertEquals(file.length(), expectedFile.length())
        assertTrue(expectedFile.delete())
    }
}
