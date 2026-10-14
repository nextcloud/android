/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity.filedisplayactivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsWorker
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileDisplayActivityViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val repository: FileDisplayActivityRepository
) : ViewModel() {

    private val _recommendedFiles = MutableSharedFlow<List<OCFile>>(replay = 1)
    val recommendedFiles: SharedFlow<List<OCFile>> = _recommendedFiles.asSharedFlow()

    private val _syncFolderResult = MutableSharedFlow<RemoteOperationResult<*>>()
    val syncFolderResult: SharedFlow<RemoteOperationResult<*>> = _syncFolderResult.asSharedFlow()

    suspend fun observeOfflineWorker(onComplete: () -> Unit) {
        workManager
            .getWorkInfosByTagFlow(BackgroundJobManagerImpl.formatClassTag(OfflineOperationsWorker::class))
            .collect { workInfos ->
                if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
                    onComplete()
                }
            }
    }

    fun syncFolder(folder: OCFile, ignoreETag: Boolean) {
        viewModelScope.launch {
            _syncFolderResult.emit(repository.syncFolder(folder, ignoreETag))
        }
    }

    fun fetchRecommendedFiles(ignoreETag: Boolean, folder: OCFile?) {
        viewModelScope.launch {
            val files = repository.fetchRecommendedFiles(ignoreETag, folder) ?: return@launch
            _recommendedFiles.emit(files)
        }
    }

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
