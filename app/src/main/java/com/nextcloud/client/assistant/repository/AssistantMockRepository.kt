/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.repository

import com.nextcloud.utils.extensions.getRandomString
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInputShape
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutputShape
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

@Suppress("MagicNumber")
class AssistantMockRepository(private val giveEmptyTasks: Boolean = false) : AssistantRepositoryType {
    override fun getTaskTypes(): List<TaskTypeData> = listOf(
        TaskTypeData(
            "core:text2text",
            "Free text to text prompt",
            "Runs an arbitrary prompt through a language model that returns a reply",
            listOf(
                TaskInputShape(
                    "Prompt",
                    "Describe a task that you want the assistant to do or ask a question",
                    "Text"
                )
            ),
            listOf(
                TaskOutputShape(
                    "Generated reply",
                    "The generated text from the assistant",
                    "Text"
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
}
