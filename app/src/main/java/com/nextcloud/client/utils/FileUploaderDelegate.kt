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

package com.nextcloud.client.utils

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.UploadFileOperation

class FileUploaderDelegate {
    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners once we drop FileUploader
     */
    fun sendBroadcastUploadsAdded(context: Context, localBroadcastManager: LocalBroadcastManager) {
        val start = Intent(FileUploader.getUploadsAddedMessage())
        // nothing else needed right now
        start.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(start)
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners once we drop FileUploader
     *
     * @param upload Finished upload operation
     */
    fun sendBroadcastUploadStarted(
        upload: UploadFileOperation,
        context: Context,
        localBroadcastManager: LocalBroadcastManager
    ) {
        val start = Intent(FileUploader.getUploadStartMessage())
        start.putExtra(FileUploader.EXTRA_REMOTE_PATH, upload.remotePath) // real remote
        start.putExtra(FileUploader.EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
        start.putExtra(FileUploader.ACCOUNT_NAME, upload.user.accountName)
        start.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(start)
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners once we drop FileUploader
     *
     * @param upload                 Finished upload operation
     * @param uploadResult           Result of the upload operation
     * @param unlinkedFromRemotePath Path in the uploads tree where the upload was unlinked from
     */
    fun sendBroadcastUploadFinished(
        upload: UploadFileOperation,
        uploadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?,
        context: Context,
        localBroadcastManager: LocalBroadcastManager
    ) {
        val end = Intent(FileUploader.getUploadFinishMessage())
        // real remote path, after possible automatic renaming
        end.putExtra(FileUploader.EXTRA_REMOTE_PATH, upload.remotePath)
        if (upload.wasRenamed()) {
            end.putExtra(FileUploader.EXTRA_OLD_REMOTE_PATH, upload.oldFile!!.remotePath)
        }
        end.putExtra(FileUploader.EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
        end.putExtra(FileUploader.ACCOUNT_NAME, upload.user.accountName)
        end.putExtra(FileUploader.EXTRA_UPLOAD_RESULT, uploadResult.isSuccess)
        if (unlinkedFromRemotePath != null) {
            end.putExtra(FileUploader.EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
        }
        end.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(end)
    }
}
