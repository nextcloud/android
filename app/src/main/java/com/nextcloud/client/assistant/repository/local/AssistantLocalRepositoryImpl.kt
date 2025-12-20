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

    override suspend fun cacheTasks(tasks: List<Task>, accountName: String) {
        val entities = tasks.map { it.toEntity(accountName) }
        assistantDao.insertAssistantTasks(entities)
    }

    override suspend fun getCachedTasks(accountName: String): List<Task> {
        val entities = assistantDao.getAssistantTasksByAccount(accountName)
        return entities.map { it.toTask() }
    }

    override suspend fun insertTask(task: Task, accountName: String) {
        assistantDao.insertAssistantTask(task.toEntity(accountName))
    }

    override suspend fun deleteTask(id: Long, accountName: String) {
        val cached = assistantDao.getAssistantTasksByAccount(accountName).firstOrNull { it.id == id } ?: return
        assistantDao.deleteAssistantTask(cached)
    }

    // region Mapping helpers
    private fun Task.toEntity(accountName: String): AssistantEntity = AssistantEntity(
        id = this.id,
        accountName = accountName,
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
