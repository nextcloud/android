/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.repository.local

import com.owncloud.android.lib.resources.assistant.v2.model.Task

interface AssistantLocalRepository {
    suspend fun cacheTasks(tasks: List<Task>, accountName: String)
    suspend fun getCachedTasks(accountName: String): List<Task>
    suspend fun insertTask(task: Task, accountName: String)
    suspend fun deleteTask(id: Long, accountName: String)
}
