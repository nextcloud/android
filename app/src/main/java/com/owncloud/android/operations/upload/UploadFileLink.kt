/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import java.io.File

object UploadFileLink {
    fun completeIdentical(upload: OCUpload, remote: OCFile, uploads: UploadsStorageManager) {
        upload.remotePath = remote.remotePath
        upload.fileSize = remote.fileLength
        upload.uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED
        upload.lastResult = UploadResult.SAME_FILE_CONFLICT
        uploads.updateUpload(upload)
    }

    fun findUploadedFile(storage: FileDataStorageManager, remotePath: String): OCFile? =
        storage.getFileByEncryptedRemotePath(remotePath)
            ?: storage.getFileByDecryptedRemotePath(remotePath)?.takeIf { it.isEncrypted }

    fun displayPath(storage: FileDataStorageManager, upload: OCUpload, file: OCFile?): String {
        if (file?.isEncrypted == true && file.decryptedRemotePath != file.remotePath) {
            return file.decryptedRemotePath
        }
        val parentPath = upload.remotePath.substringBeforeLast('/') + "/"
        val parent = storage.getFileByEncryptedRemotePath(parentPath)
        return if (parent?.isEncrypted == true) {
            parent.decryptedRemotePath + File(upload.localPath).name
        } else {
            upload.remotePath
        }
    }
}
