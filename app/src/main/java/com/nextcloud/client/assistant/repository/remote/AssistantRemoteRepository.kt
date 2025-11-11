/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.repository.remote

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

interface AssistantRemoteRepository {
    fun getTaskTypes(): List<TaskTypeData>?

    fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void>

    fun getTaskList(taskType: String): List<Task>?

    fun deleteTask(id: Long): RemoteOperationResult<Void>

    fun fetchChatMessages(id: Long): List<ChatMessage>?

    fun sendChatMessage(request: ChatMessageRequest): ChatMessage?
}
