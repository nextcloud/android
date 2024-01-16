/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

fun WorkManager.isWorkRunning(tag: String): Boolean {
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
        running = running || state == WorkInfo.State.RUNNING
    }

    return running
}
