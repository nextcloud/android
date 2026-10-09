/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity.filedisplayactivity

import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsWorker
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import javax.inject.Inject

class FileDisplayActivityViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val repository: FileDisplayActivityRepository
) : ViewModel() {

    suspend fun observeOfflineWorker(onComplete: () -> Unit) {
        workManager
            .getWorkInfosByTagFlow(BackgroundJobManagerImpl.formatClassTag(OfflineOperationsWorker::class))
            .collect { workInfos ->
                if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
                    onComplete()
                }
            }
    }

    suspend fun syncFolder(folder: OCFile, ignoreETag: Boolean): RemoteOperationResult<*> =
        repository.syncFolder(folder, ignoreETag)

    suspend fun fetchRecommendedFiles(ignoreETag: Boolean, folder: OCFile?): ArrayList<OCFile>? =
        repository.fetchRecommendedFiles(ignoreETag, folder)

    fun downloadFileIfNotStartedBefore(file: OCFile) {
        repository.downloadFileIfNotStartedBefore(file)
    }

    fun downloadFile(file: OCFile, downloadBehaviour: String, packageName: String, activityName: String) {
        repository.downloadFile(file, downloadBehaviour, packageName, activityName)
    }

    fun uploadFiles(localPaths: Array<String>, remotePaths: Array<String>, localBehaviour: Int) {
        repository.uploadFiles(localPaths, remotePaths, localBehaviour)
    }

    fun startMetadataSync(remotePath: String, folderAlreadySynced: Boolean) {
        repository.startMetadataSync(remotePath, folderAlreadySynced)
    }
}
