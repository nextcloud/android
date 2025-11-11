/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.assistant.repository.remote

import com.nextcloud.utils.extensions.getRandomString
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
import com.owncloud.android.lib.resources.assistant.chat.model.CreateConversation
import com.owncloud.android.lib.resources.assistant.v2.model.Shape
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

@Suppress("MagicNumber")
class MockAssistantRemoteRepository(private val giveEmptyTasks: Boolean = false) : AssistantRemoteRepository {
    override fun getTaskTypes(): List<TaskTypeData> = listOf(
        TaskTypeData(
            id = "core:text2text",
            name = "Free text to text prompt",
            description = "Runs an arbitrary prompt through a language model that returns a reply",
            inputShape = mapOf(
                "input" to Shape(
                    name = "Prompt",
                    description = "Describe a task that you want the assistant to do or ask a question",
                    type = "Text"
                )
            ),
            outputShape = mapOf(
                "output" to Shape(
                    name = "Generated reply",
                    description = "The generated text from the assistant",
                    type = "Text"
                )
            )
        )
    )

    override fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void> =
        RemoteOperationResult<Void>(RemoteOperationResult.ResultCode.OK)

    override fun getTaskList(taskType: String): List<Task> = if (giveEmptyTasks) {
        listOf()
    } else {
        listOf(
            Task(
                1,
                "FreePrompt",
                null,
                "12",
                "",
                TaskInput("Give me some long text 1"),
                TaskOutput("Lorem ipsum".getRandomString(100)),
                1707692337,
                1707692337,
                1707692337,
                1707692337,
                1707692337
            )
        )
    }

    override fun deleteTask(id: Long): RemoteOperationResult<Void> =
        RemoteOperationResult<Void>(RemoteOperationResult.ResultCode.OK)

    override fun fetchChatMessages(id: Long): List<ChatMessage>? {
        return emptyList()
    }

    override fun sendChatMessage(request: ChatMessageRequest): ChatMessage? {
        return null
    }

    override fun createConversation(title: String): CreateConversation? {
        return null
    }
}
