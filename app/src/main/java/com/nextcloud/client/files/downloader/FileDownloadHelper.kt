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

package com.nextcloud.client.files.downloader

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File
import javax.inject.Inject

class FileDownloadHelper : OnDatatransferProgressListener {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    private val boundListeners: MutableMap<Long, OnDatatransferProgressListener> = HashMap()
    private var currentDownload: DownloadFileOperation? = null

    init {
        MainApp.getAppComponent().inject(this)
    }

    fun addDataTransferProgressListener(listener: OnDatatransferProgressListener?, file: OCFile?) {
        if (file == null || listener == null) {
            return
        }

        boundListeners[file.fileId] = listener
    }

    fun removeDataTransferProgressListener(listener: OnDatatransferProgressListener?, file: OCFile?) {
        if (file == null || listener == null) {
            return
        }

        val fileId = file.fileId
        if (boundListeners[fileId] === listener) {
            boundListeners.remove(fileId)
        }
    }

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        fileName: String
    ) {
        val listener = boundListeners[currentDownload?.file?.fileId]
        listener?.onTransferProgress(
            progressRate,
            totalTransferredSoFar,
            totalToTransfer,
            fileName
        )
    }

    fun setCurrentDownload(operation: DownloadFileOperation) {
        currentDownload = operation
    }

    fun isDownloading(user: User?, file: OCFile?): Boolean {
        return user != null && file != null && backgroundJobManager.isStartFileDownloadJobScheduled(
            user,
            file
        )
    }

    fun cancelPendingOrCurrentDownloads(user: User?, file: OCFile?) {
        if (user == null || file == null) return
        backgroundJobManager.cancelFilesDownloadJob(user, file)
    }

    fun cancelAllDownloadsForAccount(accountName: String?, currentDownload: DownloadFileOperation) {
        if (currentDownload.user.nameEquals(accountName)) {
            currentDownload.file?.let { file ->
                backgroundJobManager.cancelFilesDownloadJob(currentDownload.user, file)
            }

            currentDownload.cancel()
        }

        // removePendingDownload(accountName)
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

    fun downloadFile(user: User, ocFile: OCFile) {
        backgroundJobManager.startFileDownloadJob(
            user,
            ocFile,
            "",
            DownloadType.DOWNLOAD,
            "",
            "",
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, behaviour: String) {
        backgroundJobManager.startFileDownloadJob(
            user,
            ocFile,
            behaviour,
            DownloadType.DOWNLOAD,
            "",
            "",
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, behaviour: String, packageName: String, activityName: String) {
        backgroundJobManager.startFileDownloadJob(
            user,
            ocFile,
            behaviour,
            DownloadType.DOWNLOAD,
            packageName,
            packageName,
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, downloadType: DownloadType) {
        backgroundJobManager.startFileDownloadJob(
            user,
            ocFile,
            "",
            downloadType,
            "",
            "",
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, conflictUploadId: Long) {
        backgroundJobManager.startFileDownloadJob(
            user,
            ocFile,
            "",
            DownloadType.DOWNLOAD,
            "",
            "",
            conflictUploadId
        )
    }

    @Suppress("LongParameterList")
    fun downloadFile(
        user: User,
        ocFile: OCFile,
        behaviour: String,
        downloadType: DownloadType?,
        activityName: String,
        packageName: String,
        conflictUploadId: Long?
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
