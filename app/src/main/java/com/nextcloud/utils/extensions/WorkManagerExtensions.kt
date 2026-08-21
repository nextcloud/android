/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.owncloud.android.lib.common.utils.Log_OC
import java.util.concurrent.ExecutionException

private const val TAG = "WorkManager"

fun WorkManager.isWorkRunning(tag: String): Boolean = checkWork(tag, listOf(WorkInfo.State.RUNNING))

fun WorkManager.isWorkScheduled(tag: String): Boolean =
    checkWork(tag, listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED))

private fun WorkManager.checkWork(tag: String, stateConditions: List<WorkInfo.State>): Boolean {
    val statuses: ListenableFuture<List<WorkInfo>> = getWorkInfosByTag(tag)
    var workInfoList: List<WorkInfo> = emptyList()

    try {
        workInfoList = statuses.get()
    } catch (e: ExecutionException) {
        Log_OC.d(TAG, "ExecutionException in checkWork: $e")
    } catch (e: InterruptedException) {
        Log_OC.d(TAG, "InterruptedException in checkWork: $e")
    }

    return workInfoList.any { workInfo -> stateConditions.contains(workInfo.state) }
}
