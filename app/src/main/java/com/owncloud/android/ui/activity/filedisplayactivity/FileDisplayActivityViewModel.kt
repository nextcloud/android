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
import com.nextcloud.client.jobs.folderDownload.FolderDownloadWorker
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsWorker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FileDisplayActivityViewModel @Inject constructor(private val workManager: WorkManager) : ViewModel() {

    suspend fun observeOfflineWorker(onComplete: () -> Unit) {
        workManager
            .getWorkInfosByTagFlow(BackgroundJobManagerImpl.formatClassTag(OfflineOperationsWorker::class))
            .collect { workInfos ->
                if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
                    onComplete()
                }
            }
    }

    suspend fun observeFolderDownloadWorker(onStarted: (Long) -> Unit, onCompleted: (Long) -> Unit) {
        var downloadingFolderId: Long? = null
        workManager
            .getWorkInfosForUniqueWorkFlow(BackgroundJobManagerImpl.JOB_DOWNLOAD_FOLDER)
            .map { workInfos -> workInfos.firstNotNullOfOrNull { it.downloadingFolderId() } }
            .distinctUntilChanged()
            .collect { folderId ->
                downloadingFolderId?.let(onCompleted)
                folderId?.let(onStarted)
                downloadingFolderId = folderId
            }
    }

    private fun WorkInfo.downloadingFolderId(): Long? = if (state == WorkInfo.State.RUNNING) {
        progress.keyValueMap[FolderDownloadWorker.FOLDER_ID] as? Long
    } else {
        null
    }
}
