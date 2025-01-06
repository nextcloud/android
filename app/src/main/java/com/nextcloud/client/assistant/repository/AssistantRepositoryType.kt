/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.repository

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

interface AssistantRepositoryType {
    fun getTaskTypes(): List<TaskTypeData>?

    fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void>

    fun getTaskList(taskType: String): List<com.owncloud.android.lib.resources.assistant.v2.model.Task>?

    fun deleteTask(id: Long): RemoteOperationResult<Void>
}
