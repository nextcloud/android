/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.junit.Assert.assertTrue
import java.io.File

@Suppress("DEPRECATION")
abstract class AbstractInternalTwoWaySyncIT : AbstractOnServerIT() {

    companion object {
        const val TWO_WAY_SYNC_ENABLED = 0L
        const val BINARY_MIME_TYPE = "application/octet-stream"
        const val MILLIS_IN_SECOND = 1000
        const val POLL_ATTEMPTS = 10
        const val SMALL_DUMMY_FILE = "nonEmpty.txt"
        const val LARGE_DUMMY_FILE = "chunkedFile.txt"
        const val FILE_NAME = "file.bin"
    }

    protected fun sync(remotePath: String): RemoteOperationResult<*> =
        SynchronizeFolderOperation(targetContext, remotePath, user, storageManager, false, true)
            .execute(targetContext)

    protected fun syncAndAssertSuccess(remotePath: String) = assertTrue(sync(remotePath).isSuccess)

    protected fun setUpTwoWaySyncFolder(remotePath: String): OCFile {
        createFolder(remotePath)
        syncAndAssertSuccess(remotePath)

        return storageManager.getFileByPath(remotePath).apply {
            internalFolderSyncTimestamp = TWO_WAY_SYNC_ENABLED
            storageManager.saveFile(this)
        }
    }

    protected fun setUpTwoWaySyncFolderWithFile(remotePath: String): OCFile = setUpTwoWaySyncFolder(remotePath).also {
        uploadFile(getDummyFile(SMALL_DUMMY_FILE), it.childPath(FILE_NAME))
        syncAndAssertSuccess(it.remotePath)
    }

    protected fun uploadDirectlyToServer(localFile: File, remotePath: String) {
        val modificationTimestamp = System.currentTimeMillis() / MILLIS_IN_SECOND
        val result = UploadFileRemoteOperation(
            localFile.absolutePath,
            remotePath,
            BINARY_MIME_TYPE,
            modificationTimestamp
        ).execute(client)
        assertTrue(result.isSuccess)
    }

    protected fun existsOnServer(remotePath: String): Boolean =
        ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess

    protected fun serverEtagOf(remotePath: String): String? = ReadFileRemoteOperation(remotePath)
        .execute(client)
        .takeIf { it.isSuccess }
        ?.let { (it.data.first() as RemoteFile).etag }

    protected fun waitUntil(condition: () -> Boolean): Boolean = (1..POLL_ATTEMPTS).any {
        val fulfilled = condition()
        if (!fulfilled) {
            shortSleep()
        }
        fulfilled
    }

    protected fun waitUntilServerEtagChanges(remotePath: String, previousEtag: String): Boolean = waitUntil {
        serverEtagOf(remotePath)?.let { etag -> etag != previousEtag } == true
    }

    protected fun OCFile.childPath(name: String): String = remotePath + name

    protected fun OCFile.childFolderPath(name: String): String = remotePath + name + OCFile.PATH_SEPARATOR
}
