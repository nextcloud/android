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

package com.nextcloud.utils.extensions

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.owncloud.android.lib.common.utils.Log_OC
import java.util.concurrent.ExecutionException

fun WorkManager.isWorkScheduled(tag: String): Boolean {
    val statuses: ListenableFuture<List<WorkInfo>> = this.getWorkInfosByTag(tag)
    var running = false
    var workInfoList: List<WorkInfo> = emptyList()

    try {
        workInfoList = statuses.get()
    } catch (e: ExecutionException) {
        Log_OC.d("Worker", "ExecutionException in isWorkScheduled: $e")
    } catch (e: InterruptedException) {
        Log_OC.d("Worker", "InterruptedException in isWorkScheduled: $e")
    }

    for (workInfo in workInfoList) {
        val state = workInfo.state
        running = running || (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED)
    }

    return running
}
