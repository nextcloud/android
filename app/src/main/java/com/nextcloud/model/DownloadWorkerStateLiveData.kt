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

package com.nextcloud.model

import androidx.lifecycle.LiveData
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile

class DownloadWorkerStateLiveData private constructor(): LiveData<ArrayList<DownloadWorkerState>>() {

    private var workers: ArrayList<DownloadWorkerState> = arrayListOf()

    fun isDownloading(user: User?, file: OCFile?): Boolean {
        if (user == null || file == null) {
            return false
        }

        var result = false

        workers.forEach { downloadState ->
            downloadState.pendingDownloads?.all?.forEach { download ->
                result = download.value?.payload?.file?.fileId == file.fileId
            }
        }

        return result
    }

    fun removeWorker(tag: String) {
        workers.forEach {
            if (it.tag == tag) {
                workers.remove(it)
            }
        }
        postValue(workers)
    }

    fun addWorker(state: DownloadWorkerState) {
        workers.add(state)
        postValue(workers)
    }

    companion object {
        private var instance: DownloadWorkerStateLiveData? = null

        fun instance(): DownloadWorkerStateLiveData {
            return instance ?: synchronized(this) {
                instance ?: DownloadWorkerStateLiveData().also { instance = it }
            }
        }
    }
}
