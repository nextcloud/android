/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.repository.local

import com.nextcloud.client.database.dao.AssistantDao
import com.nextcloud.client.database.entity.AssistantEntity
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutput

class AssistantLocalRepositoryImpl(private val assistantDao: AssistantDao) : AssistantLocalRepository {

    override suspend fun cacheTasks(tasks: List<Task>) {
        val entities = tasks.map { it.toEntity() }
        assistantDao.insertAssistantTasks(entities)
    }

    override suspend fun getCachedTasks(): List<Task> {
        val entities = assistantDao.getAllAssistantTasks()
        return entities.map { it.toTask() }
    }

    override suspend fun insertTask(task: Task) {
        assistantDao.insertAssistantTask(task.toEntity())
    }

    override suspend fun deleteTask(id: Long) {
        val cached = assistantDao.getAllAssistantTasks().firstOrNull { it.id == id } ?: return
        assistantDao.deleteAssistantTask(cached)
    }

    override suspend fun clearAll() {
        assistantDao.deleteAllAssistantTasks()
    }

    // region Mapping helpers
    private fun Task.toEntity(): AssistantEntity = AssistantEntity(
        id = this.id,
        type = this.type,
        status = this.status,
        userId = this.userId,
        appId = this.appId,
        input = this.input?.input,
        output = this.output?.output,
        completionExpectedAt = this.completionExpectedAt,
        progress = this.progress,
        lastUpdated = this.lastUpdated,
        scheduledAt = this.scheduledAt,
        endedAt = this.endedAt
    )

    private fun AssistantEntity.toTask(): Task = Task(
        id = this.id,
        type = this.type,
        status = this.status,
        userId = this.userId,
        appId = this.appId,
        input = TaskInput(input = this.input),
        output = TaskOutput(output = this.output),
        completionExpectedAt = this.completionExpectedAt,
        progress = this.progress,
        lastUpdated = this.lastUpdated,
        scheduledAt = this.scheduledAt,
        endedAt = this.endedAt
    )
    // endregion
}
