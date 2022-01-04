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
package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileWriter

class SynchronizeFileOperationIT : AbstractOnServerIT() {
    @Test
    fun syncFile() {
        val remoteFilePath = "/testSyncFile.md"
        val file = getDummyFile("nonEmpty.txt")
        val fileLength = file.length()

        assertTrue(fileLength > 0)

        uploadFile(file, remoteFilePath)

        val ocFile = storageManager.getFileByDecryptedRemotePath(remoteFilePath)
        assertNotNull(ocFile)
        assertTrue(File(ocFile!!.storagePath!!).exists())
        assertEquals(fileLength, File(ocFile.storagePath).length())

        // sync file
        assertTrue(
            SynchronizeFileOperation(ocFile, null, user, true, targetContext, storageManager)
                .execute(client)
                .isSuccess
        )

        val localFile = File(ocFile.storagePath)
        assertTrue(localFile.exists())
        assertEquals(fileLength, localFile.length())

        // write something in it
        FileWriter(localFile, true).apply {
            appendLine("some random text")
            close()
        }
        val newFileLength = localFile.length()
        assertTrue(newFileLength > fileLength)

        // sync file
        assertTrue(
            SynchronizeFileOperation(ocFile, null, user, true, targetContext, storageManager)
                .execute(client)
                .isSuccess
        )
        assertEquals(newFileLength, File(ocFile.storagePath).length())

        // remove local file, download to check that server file is correct
        assertTrue(File(ocFile.storagePath).delete())

        val downloadFileOperation = DownloadFileOperation(user, ocFile, targetContext)
        val downloadedFileLocation = downloadFileOperation.savePath

        assertFalse(File(downloadedFileLocation).exists())
        assertTrue(downloadFileOperation.execute(client).isSuccess)
        assertEquals(newFileLength, File(downloadedFileLocation).length())
    }
}
