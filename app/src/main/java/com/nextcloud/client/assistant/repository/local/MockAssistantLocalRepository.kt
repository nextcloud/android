/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.repository.local

import com.owncloud.android.lib.resources.assistant.v2.model.Task
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MockAssistantLocalRepository : AssistantLocalRepository {

    private val tasks = mutableListOf<Task>()
    private val mutex = Mutex()

    override suspend fun cacheTasks(tasks: List<Task>) {
        mutex.withLock {
            this.tasks.clear()
            this.tasks.addAll(tasks)
        }
    }

    override suspend fun getCachedTasks(): List<Task> = mutex.withLock { tasks.toList() }

    override suspend fun insertTask(task: Task) {
        mutex.withLock { tasks.add(task) }
    }

    override suspend fun deleteTask(id: Long) {
        mutex.withLock { tasks.removeAll { it.id == id } }
    }

    override suspend fun clearAll() {
        mutex.withLock { tasks.clear() }
    }
}
