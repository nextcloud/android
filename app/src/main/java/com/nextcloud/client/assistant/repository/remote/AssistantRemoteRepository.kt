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
import com.owncloud.android.lib.resources.assistant.chat.model.CreateConversation
import com.owncloud.android.lib.resources.assistant.chat.model.Session
import com.owncloud.android.lib.resources.assistant.chat.model.SessionTask
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

interface AssistantRemoteRepository {
    suspend fun getTaskTypes(): List<TaskTypeData>?

    suspend fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void>

    suspend fun getTaskList(taskType: String): List<Task>?

    suspend fun deleteTask(id: Long): RemoteOperationResult<Void>

    suspend fun fetchChatMessages(id: Long): List<ChatMessage>?

    suspend fun sendChatMessage(request: ChatMessageRequest): ChatMessage?

    suspend fun createConversation(title: String): CreateConversation?

    suspend fun checkSession(sessionId: String): Session?

    suspend fun generateSession(sessionId: String): SessionTask?

    suspend fun checkGeneration(taskId: String, sessionId: String): ChatMessage?
}
