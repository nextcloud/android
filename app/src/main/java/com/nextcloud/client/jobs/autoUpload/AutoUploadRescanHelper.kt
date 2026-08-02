/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Puts the auto-upload bookkeeping of every enabled folder back into a state where files that never made it to the
 * server are picked up again. This is the manual counterpart to deleting and recreating an auto-upload configuration.
 *
 * A file is queued again when it is marked as sent for upload but no successful upload backs that claim. Files with a
 * successful upload are left untouched so a rescan never produces duplicates on the server.
 *
 * Dropping the unsuccessful upload records can hit a record of an upload that is running at this very moment. The
 * upload itself keeps going, only its bookkeeping row is gone, and the rescan queues the file again. That is the
 * accepted price for freeing the far more common case of a record left behind in progress by a killed worker.
 */
class AutoUploadRescanHelper @Inject constructor(
    private val fileSystemDao: FileSystemDao,
    private val uploadDao: UploadDao,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager
) {
    companion object {
        private const val TAG = "AutoUploadRescanHelper"
    }

    /**
     * @return the number of folders a rescan was started for
     */
    suspend fun rescan(accountName: String): Int = withContext(Dispatchers.IO) {
        val folders = syncedFolderProvider.syncedFolders.filter { it.isEnabled && it.account == accountName }

        Log_OC.d(TAG, "Rescan requested for ${folders.size} enabled folders of $accountName")

        folders.forEach { rescanFolder(it) }

        folders.size
    }

    private suspend fun rescanFolder(syncedFolder: SyncedFolder) {
        val removedUploads = clearUnsuccessfulUploads(syncedFolder)
        val requeuedFiles = fileSystemDao.requeueFilesWithoutSuccessfulUpload(
            syncedFolderId = syncedFolder.id.toString(),
            succeededStatus = UploadStatus.UPLOAD_SUCCEEDED.value
        )

        // forget the last scan so the worker starts right away and does not skip unchanged files
        syncedFolder.lastScanTimestampMs = SyncedFolder.NOT_SCANNED_YET
        syncedFolderProvider.updateSyncFolder(syncedFolder)

        backgroundJobManager.startAutoUpload(syncedFolder, overridePowerSaving = true, fullRescan = true)

        Log_OC.d(
            TAG,
            "Rescanning ${syncedFolder.localPath}: $requeuedFiles files queued again, " +
                "$removedUploads unsuccessful uploads dropped"
        )
    }

    private suspend fun clearUnsuccessfulUploads(syncedFolder: SyncedFolder): Int {
        val localPath = syncedFolder.localPath
        if (localPath.isNullOrEmpty()) {
            Log_OC.w(TAG, "Cannot clear uploads, local path is missing for folder ${syncedFolder.id}")
            return 0
        }

        val localPathPrefix = if (localPath.endsWith(File.separator)) localPath else localPath + File.separator

        return uploadDao.deleteUnsuccessfulUploadsInFolder(
            accountName = syncedFolder.account,
            localPathPrefix = localPathPrefix,
            succeededStatus = UploadStatus.UPLOAD_SUCCEEDED.value
        )
    }
}
