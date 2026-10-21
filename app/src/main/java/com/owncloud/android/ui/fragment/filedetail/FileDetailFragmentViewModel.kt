/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.filedetail

import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.owncloud.android.datamodel.OCFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class FileDetailFragmentViewModel @Inject constructor(private val workManager: WorkManager) : ViewModel() {

    private val downloadWorkName = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadProgress: Flow<Int> = downloadWorkName
        .filterNotNull()
        .flatMapLatest { workManager.getWorkInfosForUniqueWorkFlow(it) }
        .mapNotNull { workInfos -> workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }?.downloadPercent() }
        .distinctUntilChanged()

    fun observeDownloadProgress(user: User, file: OCFile) {
        downloadWorkName.value = BackgroundJobManagerImpl.formatFileDownloadTag(user.accountName, file.fileId)
    }

    private fun WorkInfo.downloadPercent(): Int? = progress.keyValueMap[FileDownloadWorker.PROGRESS_PERCENT] as? Int
}
