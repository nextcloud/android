/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.assistant.repository.remote

import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.TimeConstants
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.assistant.chat.CheckGenerationRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.CheckSessionRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.CreateConversationRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.CreateMessageRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.GenerateSessionRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.GetMessagesRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
import com.owncloud.android.lib.resources.assistant.chat.model.CreateConversation
import com.owncloud.android.lib.resources.assistant.chat.model.Session
import com.owncloud.android.lib.resources.assistant.chat.model.SessionTask
import com.owncloud.android.lib.resources.assistant.v1.CreateTaskRemoteOperationV1
import com.owncloud.android.lib.resources.assistant.v1.DeleteTaskRemoteOperationV1
import com.owncloud.android.lib.resources.assistant.v1.GetTaskListRemoteOperationV1
import com.owncloud.android.lib.resources.assistant.v1.GetTaskTypesRemoteOperationV1
import com.owncloud.android.lib.resources.assistant.v1.model.toV2
import com.owncloud.android.lib.resources.assistant.v2.CreateTaskRemoteOperationV2
import com.owncloud.android.lib.resources.assistant.v2.DeleteTaskRemoteOperationV2
import com.owncloud.android.lib.resources.assistant.v2.GetTaskListRemoteOperationV2
import com.owncloud.android.lib.resources.assistant.v2.GetTaskTypesRemoteOperationV2
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssistantRemoteRepositoryImpl(private val client: NextcloudClient, capability: OCCapability) :
    AssistantRemoteRepository {

    private val supportsV2 = capability.version.isNewerOrEqual(NextcloudVersion.nextcloud_30)

    override suspend fun getTaskTypes(): List<TaskTypeData>? = withContext(Dispatchers.IO) {
        if (supportsV2) {
            val result = GetTaskTypesRemoteOperationV2().execute(client)
            if (result.isSuccess) {
                return@withContext result.resultData
            }
        } else {
            val result = GetTaskTypesRemoteOperationV1().execute(client)
            if (result.isSuccess) {
                return@withContext result.resultData.toV2()
            }
        }
        return@withContext null
    }

    override suspend fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void> =
        withContext(Dispatchers.IO) {
            if (supportsV2) {
                CreateTaskRemoteOperationV2(input, taskType).execute(client)
            } else {
                if (taskType.id.isNullOrEmpty()) {
                    RemoteOperationResult<Void>(ResultCode.CANCELLED)
                } else {
                    CreateTaskRemoteOperationV1(input, taskType.id!!).execute(client)
                }
            }
        }

    override suspend fun getTaskList(taskType: String): List<Task>? = withContext(Dispatchers.IO) {
        if (supportsV2) {
            val result = GetTaskListRemoteOperationV2(taskType).execute(client)
            if (result.isSuccess) {
                return@withContext result.resultData.tasks.filter { it.appId == "assistant" }
            }
        } else {
            val result = GetTaskListRemoteOperationV1("assistant").execute(client)
            if (result.isSuccess) {
                return@withContext result.resultData.toV2().tasks.filter { it.type == taskType }
            }
        }
        return@withContext null
    }

    override suspend fun deleteTask(id: Long): RemoteOperationResult<Void> = withContext(Dispatchers.IO) {
        if (supportsV2) {
            DeleteTaskRemoteOperationV2(id).execute(client)
        } else {
            DeleteTaskRemoteOperationV1(id).execute(client)
        }
    }

    override suspend fun fetchChatMessages(id: Long): List<ChatMessage>? = withContext(Dispatchers.IO) {
        val result = GetMessagesRemoteOperation(id.toString()).execute(client)
        if (result.isSuccess) result.resultData else null
    }

    override suspend fun sendChatMessage(request: ChatMessageRequest): ChatMessage? = withContext(Dispatchers.IO) {
        val result = CreateMessageRemoteOperation(request).execute(client)
        if (result.isSuccess) result.resultData else null
    }

    override suspend fun createConversation(title: String): CreateConversation? = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis().div(TimeConstants.MILLIS_PER_SECOND)
        val result = CreateConversationRemoteOperation(title, timestamp).execute(client)
        if (result.isSuccess) result.resultData else null
    }

    override suspend fun checkSession(sessionId: String): Session? = withContext(Dispatchers.IO) {
        val result = CheckSessionRemoteOperation(sessionId).execute(client)
        if (result.isSuccess) result.resultData else null
    }

    override suspend fun generateSession(sessionId: String): SessionTask? = withContext(Dispatchers.IO) {
        val result = GenerateSessionRemoteOperation(sessionId).execute(client)
        if (result.isSuccess) result.resultData else null
    }

    override suspend fun checkGeneration(taskId: String, sessionId: String): ChatMessage? =
        withContext(Dispatchers.IO) {
            val result = CheckGenerationRemoteOperation(taskId, sessionId).execute(client)
            if (result.isSuccess) result.resultData else null
        }
}
