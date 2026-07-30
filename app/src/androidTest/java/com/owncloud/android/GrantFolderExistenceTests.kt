/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.operations.UploadFileOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GrantFolderExistenceTests : AbstractOnServerIT() {

    private val root = "/autoupload/"
    private val yearFolder = root + "2026/"
    private val monthFolder = yearFolder + "07/"

    private val uploadsStorageManager = UploadsStorageManager(
        UserAccountManagerImpl.fromContext(targetContext),
        targetContext.contentResolver
    )

    private val powerManagementServiceMock = object : PowerManagementService {
        override val isPowerSavingEnabled = false
        override val isIgnoringOptimization = true
        override val battery = BatteryStatus(false, 0)
    }

    @Before
    @Throws(IOException::class)
    fun before() {
        createDummyFiles()
    }

    @Test
    fun testUploadFileThenDeleteFolder() {
        uploadAndAssertSuccess("first.txt")

        assertTrue("month folder should exist on server", existsOnServer(monthFolder))
        assertNotNull("month folder should be cached locally", storageManager.getFileByDecryptedRemotePath(monthFolder))

        removeYearFolderOnServerOnly()

        assertFalse("month folder should be deleted", existsOnServer(monthFolder))
        assertNotNull(
            "app still has a cached entry for the removed folder",
            storageManager.getFileByDecryptedRemotePath(monthFolder)
        )

        // attempting upload to the same folder
        val result = upload("nonEmpty.txt", monthFolder + "nonEmpty.txt")

        assertEquals(
            "upload must not fail with a conflict, the missing folder has to be recreated",
            RemoteOperationResult.ResultCode.OK,
            result.code
        )
        assertTrue("month folder should be recreated on server", existsOnServer(monthFolder))
        assertTrue("uploaded file should exist on server", existsOnServer(monthFolder + "nonEmpty.txt"))
    }

    @Test
    fun testRemoveRootThenUploadFile() {
        uploadAndAssertSuccess("first.txt")

        assertTrue(
            "root should be removed",
            RemoveFileRemoteOperation(root).execute(client).isSuccess
        )
        assertFalse(existsOnServer(root))

        val result = upload("nonEmpty.txt", monthFolder + "nonEmpty.txt")

        assertEquals(
            "every missing folder level has to be recreated",
            RemoteOperationResult.ResultCode.OK,
            result.code
        )
        assertTrue(existsOnServer(yearFolder))
        assertTrue(existsOnServer(monthFolder))
        assertTrue(existsOnServer(monthFolder + "nonEmpty.txt"))
    }

    private fun uploadAndAssertSuccess(filename: String) {
        val result = upload(filename, monthFolder + filename)
        assertTrue(result.logMessage, result.isSuccess)
        assertTrue("uploaded file should exist on server", existsOnServer(monthFolder + filename))
    }

    private fun removeYearFolderOnServerOnly() {
        assertTrue(
            "year folder should be removed",
            RemoveFileRemoteOperation(yearFolder).execute(client).isSuccess
        )
    }

    private fun existsOnServer(remotePath: String): Boolean =
        ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess

    private fun upload(localFileName: String, remotePath: String): RemoteOperationResult<*> {
        val localFile = createFile(localFileName, FILE_LINE_COUNT)
        assertTrue("local test file must exist before uploading", localFile.exists())

        val ocUpload = OCUpload(
            localFile.absolutePath,
            remotePath,
            account.name
        ).apply {
            isCreateRemoteFolder = true
            createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        }

        return UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.ASK_USER,
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false,
            storageManager
        ).execute(client)
    }

    companion object {
        private const val FILE_LINE_COUNT = 100
    }
}
