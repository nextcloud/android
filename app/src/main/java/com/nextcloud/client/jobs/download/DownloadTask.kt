/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.download

import android.content.ContentResolver
import android.content.Context
import com.nextcloud.client.core.IsCancelled
import com.nextcloud.client.files.DownloadRequest
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

/**
 * This runnable object encapsulates file download logic. It has been extracted to wrap
 * network operation and storage manager interactions, as those pose testing challenges
 * that cannot be addressed due to large number of dependencies.
 *
 * This design can be regarded as intermediary refactoring step.
 */
class DownloadTask(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val clientProvider: () -> OwnCloudClient
) {

    data class Result(val file: OCFile, val success: Boolean)

    /**
     * This class is a helper factory to to keep static dependencies
     * injection out of the downloader instance.
     *
     * @param context Context
     * @param clientProvider Provide client - this must be called on background thread
     * @param contentResolver content resovler used to access file storage
     */
    class Factory(
        private val context: Context,
        private val clientProvider: () -> OwnCloudClient,
        private val contentResolver: ContentResolver
    ) {
        fun create(): DownloadTask = DownloadTask(context, contentResolver, clientProvider)
    }

    // Unused progress, isCancelled arguments needed for TransferManagerTest
    fun download(request: DownloadRequest, progress: (Int) -> Unit, isCancelled: IsCancelled): Result {
        val op = DownloadFileOperation(request.user, request.file, context)
        val client = clientProvider.invoke()
        val result = op.execute(client)

        return if (result.isSuccess) {
            val storageManager = FileDataStorageManager(
                request.user,
                contentResolver
            )
            val file = saveDownloadedFile(op, storageManager)
            Result(file, true)
        } else {
            Result(request.file, false)
        }
    }

    private fun saveDownloadedFile(op: DownloadFileOperation, storageManager: FileDataStorageManager): OCFile {
        val file = storageManager.getFileById(op.file.fileId) as OCFile

        file.apply {
            val syncDate = System.currentTimeMillis()
            lastSyncDateForProperties = syncDate
            lastSyncDateForData = syncDate
            isUpdateThumbnailNeeded = true
            modificationTimestamp = op.modificationTimestamp
            modificationTimestampAtLastSyncForData = op.modificationTimestamp
            etag = op.etag
            mimeType = op.mimeType
            storagePath = op.savePath
            fileLength = File(op.savePath).length()
            remoteId = op.file.remoteId
        }

        storageManager.saveFile(file)

        if (MimeTypeUtil.isMedia(op.mimeType)) {
            FileDataStorageManager.triggerMediaScan(file.storagePath)
        }

        return file
    }
}
