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

private val RUNNING_STATES = listOf(WorkInfo.State.RUNNING)
private val PENDING_STATES = listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED)

fun WorkManager.isWorkRunning(tag: String): Boolean = getWorkInfosByTag(tag).hasWorkIn(RUNNING_STATES)

fun WorkManager.isWorkScheduled(tag: String): Boolean = getWorkInfosByTag(tag).hasWorkIn(PENDING_STATES)

/**
 * Unique work names are not tags, [getWorkInfosByTag] never matches them.
 */
fun WorkManager.isUniqueWorkRunning(uniqueWorkName: String): Boolean =
    getWorkInfosForUniqueWork(uniqueWorkName).hasWorkIn(RUNNING_STATES)

private fun ListenableFuture<List<WorkInfo>>.hasWorkIn(stateConditions: List<WorkInfo.State>): Boolean {
    var workInfoList: List<WorkInfo> = emptyList()

    try {
        workInfoList = get()
    } catch (e: ExecutionException) {
        Log_OC.d(TAG, "ExecutionException in hasWorkIn: $e")
    } catch (e: InterruptedException) {
        Log_OC.d(TAG, "InterruptedException in hasWorkIn: $e")
    }

    return workInfoList.any { workInfo -> stateConditions.contains(workInfo.state) }
}
