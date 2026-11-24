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
import androidx.work.WorkQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private val scheduledStates = listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED)

fun WorkManager.scheduledWorkInfos(scope: CoroutineScope): StateFlow<List<WorkInfo>> =
    getWorkInfosFlow(WorkQuery.fromStates(scheduledStates))
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

fun List<WorkInfo>.isWorkScheduled(tag: String): Boolean = hasWork(tag, scheduledStates)

private fun List<WorkInfo>.hasWork(tag: String, states: List<WorkInfo.State>): Boolean =
    any { workInfo -> tag in workInfo.tags && workInfo.state in states }
