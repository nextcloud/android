/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
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
package com.nextcloud.client.files.downloader

import android.content.ContentResolver
import android.content.Context
import com.nextcloud.client.core.IsCancelled
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
    val context: Context,
    val contentResolver: ContentResolver,
    val clientProvider: () -> OwnCloudClient
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
        fun create(): DownloadTask {
            return DownloadTask(context, contentResolver, clientProvider)
        }
    }

    fun download(request: DownloadRequest, progress: (Int) -> Unit, isCancelled: IsCancelled): Result {
        val op = DownloadFileOperation(request.user.toPlatformAccount(), request.file, context)
        val client = clientProvider.invoke()
        val result = op.execute(client)
        if (result.isSuccess) {
            val storageManager = FileDataStorageManager(
                request.user.toPlatformAccount(),
                contentResolver
            )
            val file = saveDownloadedFile(op, storageManager)
            return Result(file, true)
        } else {
            return Result(request.file, false)
        }
    }

    private fun saveDownloadedFile(op: DownloadFileOperation, storageManager: FileDataStorageManager): OCFile {
        val file = storageManager.getFileById(op.getFile().getFileId()) as OCFile
        val syncDate = System.currentTimeMillis()
        file.lastSyncDateForProperties = syncDate
        file.lastSyncDateForData = syncDate
        file.isUpdateThumbnailNeeded = true
        file.modificationTimestamp = op.getModificationTimestamp()
        file.modificationTimestampAtLastSyncForData = op.getModificationTimestamp()
        file.etag = op.getEtag()
        file.mimeType = op.getMimeType()
        file.storagePath = op.getSavePath()
        file.fileLength = File(op.getSavePath()).length()
        file.remoteId = op.getFile().getRemoteId()
        storageManager.saveFile(file)
        if (MimeTypeUtil.isMedia(op.getMimeType())) {
            FileDataStorageManager.triggerMediaScan(file.storagePath)
        }
        return file
    }
}
