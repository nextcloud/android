/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.nextcloud.client.jobs.download

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File
import javax.inject.Inject

class FileDownloadHelper {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    companion object {
        private var instance: FileDownloadHelper? = null

        fun instance(): FileDownloadHelper {
            return instance ?: synchronized(this) {
                instance ?: FileDownloadHelper().also { instance = it }
            }
        }
    }

    init {
        MainApp.getAppComponent().inject(this)
    }

    fun isDownloading(user: User?, file: OCFile?): Boolean {
        if (user == null || file == null) {
            return false
        }

        val fileStorageManager = FileDataStorageManager(user, MainApp.getAppContext().contentResolver)
        val topParentId = fileStorageManager.getTopParentId(file)

        return if (file.isFolder) {
            backgroundJobManager.isStartFileDownloadJobScheduled(user, file.fileId) ||
                backgroundJobManager.isStartFileDownloadJobScheduled(user, topParentId)
        } else {
            FileDownloadWorker.isDownloading(user.accountName, file.fileId)
        }
    }

    fun cancelPendingOrCurrentDownloads(user: User?, files: List<OCFile>?) {
        if (user == null || files == null) return

        files.forEach { file ->
            FileDownloadWorker.cancelOperation(user.accountName, file.fileId)
            backgroundJobManager.cancelFilesDownloadJob(user, file.fileId)
        }
    }

    fun cancelAllDownloadsForAccount(accountName: String?, currentDownload: DownloadFileOperation?) {
        if (accountName == null || currentDownload == null) return

        val currentUser = currentDownload.user
        val currentFile = currentDownload.file

        if (!currentUser.nameEquals(accountName)) {
            return
        }

        currentDownload.cancel()
        FileDownloadWorker.cancelOperation(currentUser.accountName, currentFile.fileId)
        backgroundJobManager.cancelFilesDownloadJob(currentUser, currentFile.fileId)
    }

    fun saveFile(
        file: OCFile,
        currentDownload: DownloadFileOperation?,
        storageManager: FileDataStorageManager?
    ) {
        val syncDate = System.currentTimeMillis()

        file.apply {
            lastSyncDateForProperties = syncDate
            lastSyncDateForData = syncDate
            isUpdateThumbnailNeeded = true
            modificationTimestamp = currentDownload?.modificationTimestamp ?: 0L
            modificationTimestampAtLastSyncForData = currentDownload?.modificationTimestamp ?: 0L
            etag = currentDownload?.etag
            mimeType = currentDownload?.mimeType
            storagePath = currentDownload?.savePath

            val savePathFile = currentDownload?.savePath?.let { File(it) }
            savePathFile?.let {
                fileLength = savePathFile.length()
            }

            remoteId = currentDownload?.file?.remoteId
        }

        storageManager?.saveFile(file)

        if (MimeTypeUtil.isMedia(currentDownload?.mimeType)) {
            FileDataStorageManager.triggerMediaScan(file.storagePath, file)
        }

        storageManager?.saveConflict(file, null)
    }

    fun downloadFileIfNotStartedBefore(user: User, file: OCFile) {
        if (!isDownloading(user, file)) {
            downloadFile(user, file, downloadType = DownloadType.DOWNLOAD)
        }
    }

    fun downloadFile(user: User, file: OCFile) {
        downloadFile(user, file, downloadType = DownloadType.DOWNLOAD)
    }

    @Suppress("LongParameterList")
    fun downloadFile(
        user: User,
        ocFile: OCFile,
        behaviour: String = "",
        downloadType: DownloadType? = DownloadType.DOWNLOAD,
        activityName: String = "",
        packageName: String = "",
        conflictUploadId: Long? = null
    ) {
        backgroundJobManager.startFileDownloadJob(
            user,
            ocFile,
            behaviour,
            downloadType,
            activityName,
            packageName,
            conflictUploadId
        )
    }
}
