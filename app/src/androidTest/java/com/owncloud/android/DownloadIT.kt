/*
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android

import android.net.Uri
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.utils.FileStorageUtils
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.File

/**
 * Tests related to file uploads
 */
class DownloadIT : AbstractOnServerIT() {
    @After
    override fun after() {
        val result = RefreshFolderOperation(
            storageManager.getFileByPath("/"),
            System.currentTimeMillis() / 1000L,
            false,
            true,
            storageManager,
            user,
            targetContext
        )
            .execute(client)

        // cleanup only if folder exists
        if (result.isSuccess && storageManager.getFileByDecryptedRemotePath(FOLDER) != null) {
            RemoveFileOperation(
                storageManager.getFileByDecryptedRemotePath(FOLDER),
                false,
                user,
                false,
                targetContext,
                storageManager
            )
                .execute(client)
        }
    }

    @Test
    fun verifyDownload() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload)
        val ocUpload2 = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty2.txt",
            account.name
        )
        uploadOCUpload(ocUpload2)
        refreshFolder("/")
        refreshFolder(FOLDER)
        var file1 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        var file2 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty2.txt")
        verifyDownload(file1, file2)
        Assert.assertTrue(DownloadFileOperation(user, file1, targetContext).execute(client).isSuccess)
        Assert.assertTrue(DownloadFileOperation(user, file2, targetContext).execute(client).isSuccess)
        refreshFolder(FOLDER)
        file1 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        file2 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty2.txt")
        verifyDownload(file1, file2)
    }

    private fun verifyDownload(file1: OCFile?, file2: OCFile?) {
        Assert.assertNotNull(file1)
        Assert.assertNotNull(file2)
        Assert.assertNotSame(file1!!.storagePath, file2!!.storagePath)
        Assert.assertTrue(File(file1.storagePath).exists())
        Assert.assertTrue(File(file2.storagePath).exists())

        // test against hardcoded path to make sure that it is correct
        Assert.assertEquals(
            "/storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/" +
                Uri.encode(account.name, "@") + "/testUpload/nonEmpty.txt",
            file1.storagePath
        )
        Assert.assertEquals(
            "/storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/" +
                Uri.encode(account.name, "@") + "/testUpload/nonEmpty2.txt",
            file2.storagePath
        )
    }

    companion object {
        private const val FOLDER = "/testUpload/"
    }
}