/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import org.junit.Assert.fail
import org.junit.Test
import kotlin.io.path.Path
import kotlin.io.path.exists

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
                storageManager.getFileByDecryptedRemotePath(FOLDER)!!,
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

        refreshFolder(FOLDER)

        var file1 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        var file2 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty2.txt")

        if (file1 == null) {
            fail("file 1 cannot be null")
        }
        val operation1 = DownloadFileOperation(user, file1!!, targetContext)
        val operation1Result = operation1.execute(client)
        Assert.assertTrue(operation1Result.isSuccess)

        if (file2 == null) {
            fail("file 2 cannot be null")
        }
        val operation2 = DownloadFileOperation(user, file2!!, targetContext)
        val operation2Result = operation2.execute(client)
        Assert.assertTrue(operation2Result.isSuccess)

        refreshFolder(FOLDER)
        file1 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        file2 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty2.txt")

        verifyDownload(file1, file2)
    }

    private fun verifyDownload(file1: OCFile?, file2: OCFile?) {
        Assert.assertNotNull(file1)
        Assert.assertNotNull(file2)
        Assert.assertNotSame(file1!!.storagePath, file2!!.storagePath)

        Assert.assertTrue(Path(file1.storagePath).exists())
        Assert.assertTrue(Path(file2.storagePath).exists())

        // test against hardcoded path to make sure that it is correct
        Assert.assertEquals(
            "/storage/emulated/0/Android/media/" + targetContext.packageName + "/nextcloud/" +
                Uri.encode(account.name, "@") + "/testUpload/nonEmpty.txt",
            file1.storagePath
        )
        Assert.assertEquals(
            "/storage/emulated/0/Android/media/" + targetContext.packageName + "/nextcloud/" +
                Uri.encode(account.name, "@") + "/testUpload/nonEmpty2.txt",
            file2.storagePath
        )
    }

    companion object {
        private const val FOLDER = "/testUpload/"
    }
}
