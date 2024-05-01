/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
