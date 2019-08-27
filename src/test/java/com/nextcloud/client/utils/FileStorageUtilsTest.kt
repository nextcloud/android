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
import java.util.Locale

class FileStorageUtilsTest {
    @Test
    fun testInstantUploadPathSubfolder() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "subfolder",
            "file.pdf",
            123123123L,
            false)
        val expected = "/remotePath/subfolder/file.pdf"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathNoSubfolder() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "",
            "file.pdf",
            123123123L,
            false)
        val expected = "/remotePath/file.pdf"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathNullFilename() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "subfolder",
            null,
            123123123L,
            false)
        val expected = "/remotePath/subfolder/"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathNullEmptyDate() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "",
            "file.pdf",
            0,
            true)
        val expected = "/remotePath/file.pdf"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPath() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "",
            "file.pdf",
            123123123L,
            false)
        val expected = "/remotePath/file.pdf"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDate() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "",
            "file.pdf",
            1569918628000,
            true)
        val expected = "/remotePath/2019/10/file.pdf"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderFile() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "",
            "/sub/file.pdf",
            123123123L,
            false)
        val expected = "/remotePath/sub/file.pdf"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDateWithSubfolderFile() {
        val result = FileStorageUtils.getInstantUploadFilePath(Locale.ROOT,
            "/remotePath/",
            "",
            "/sub/file.pdf",
            1569918628000,
            true)
        val expected = "/remotePath/2019/10/sub/file.pdf"

        assertEquals(expected, result)
    }
}
