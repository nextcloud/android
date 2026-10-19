/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity.filedisplayactivity

import androidx.lifecycle.ViewModel
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.folderDownload.FolderDownloadWorker
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class FileDisplayActivityViewModel @Inject constructor(private val workManager: WorkManager) : ViewModel() {

    private val currentFolderId = MutableStateFlow<Long?>(null)

    suspend fun observeOfflineWorker(onComplete: () -> Unit) {
        workManager
            .getWorkInfosByTagFlow(BackgroundJobManagerImpl.formatClassTag(OfflineOperationsWorker::class))
            .collect { workInfos ->
                if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
                    onComplete()
                }
            }
    }

    fun setCurrentFolder(folderId: Long) {
        currentFolderId.value = folderId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun observeFolderDownloadWorker(
        onFileProgress: (fileId: Long, previousFileId: Long?) -> Unit,
        onCompleted: () -> Unit
    ) {
        currentFolderId
            .filterNotNull()
            .flatMapLatest { folderId ->
                merge(
                    observeRunningProgress(FolderDownloadWorker.formatTag(folderId)),
                    observeRunningProgress(FolderDownloadWorker.formatParentTag(folderId))
                        .filter { it.isFolderDownloaded() }
                )
            }
            .collect { progress ->
                if (progress.isFolderDownloaded()) {
                    onCompleted()
                    return@collect
                }

                val fileId = progress.keyValueMap[FolderDownloadWorker.FILE_ID] as? Long ?: return@collect
                onFileProgress(fileId, progress.keyValueMap[FolderDownloadWorker.PREVIOUS_FILE_ID] as? Long)
            }
    }

    private fun observeRunningProgress(tag: String): Flow<Data> = workManager
        .getWorkInfosByTagFlow(tag)
        .mapNotNull { workInfos -> workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }?.progress }
        .distinctUntilChanged()

    private fun Data.isFolderDownloaded(): Boolean = getBoolean(FolderDownloadWorker.IS_FOLDER_DOWNLOADED, false)
}
