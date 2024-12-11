/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.repository

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskTypeData

interface AssistantRepositoryType {
    fun getTaskTypes(): RemoteOperationResult<List<TaskTypeData>>

    fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void>

    fun getTaskList(taskType: String): RemoteOperationResult<TaskList>

    fun deleteTask(id: Long): RemoteOperationResult<Void>
}
