/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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

package com.nextcloud.client.utils

import com.owncloud.android.utils.FileStorageUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.Locale

class FileStorageUtilsTest {
    @Test
    fun testInstantUploadPathSubfolder() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathNoSubfolder() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathEmptyDateZero() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 0L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPath() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDate() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 1569918628000L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/2019/10/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderFile() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDateWithSubfolderFile() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 1569918628000L

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate
        )
        val expected = "/Camera/2019/10/subfolder/file.jpg"

        assertEquals(expected, result)
    }
}
