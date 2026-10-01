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
}
