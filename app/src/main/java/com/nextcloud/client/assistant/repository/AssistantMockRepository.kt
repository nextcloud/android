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
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.lib.resources.assistant.model.TaskTypes

@Suppress("MagicNumber")
class AssistantMockRepository(private val giveEmptyTasks: Boolean = false) : AssistantRepositoryType {
    override fun getTaskTypes(): RemoteOperationResult<TaskTypes> {
        return RemoteOperationResult<TaskTypes>(RemoteOperationResult.ResultCode.OK).apply {
            resultData = TaskTypes(
                listOf(
                    TaskType("1", "FreePrompt", "You can create free prompt text"),
                    TaskType("2", "Generate Headline", "You can create generate headline text")
                )
            )
        }
    }

    override fun createTask(input: String, type: String): RemoteOperationResult<Void> {
        return RemoteOperationResult<Void>(RemoteOperationResult.ResultCode.OK)
    }

    override fun getTaskList(appId: String): RemoteOperationResult<TaskList> {
        val taskList = if (giveEmptyTasks) {
            TaskList(listOf())
        } else {
            TaskList(
                listOf(
                    Task(
                        1,
                        "FreePrompt",
                        null,
                        "12",
                        "",
                        "Give me some long text 1",
                        "Lorem ipsum".getRandomString(100),
                        ""
                    ),
                    Task(
                        2,
                        "GenerateHeadline",
                        null,
                        "12",
                        "",
                        "Give me some text 2",
                        "Lorem".getRandomString(100),
                        "",
                        ""
                    ),
                    Task(
                        3,
                        "FreePrompt",
                        null,
                        "12",
                        "",
                        "Give me some text 3",
                        "Lorem".getRandomString(300),
                        "",
                        ""
                    ),
                    Task(
                        4,
                        "FreePrompt",
                        null,
                        "12",
                        "",
                        "Give me some text 4",
                        "Lorem".getRandomString(300),
                        "",
                        ""
                    ),
                    Task(
                        5,
                        "FreePrompt",
                        null,
                        "12",
                        "",
                        "Give me some text 5",
                        "Lorem".getRandomString(300),
                        "",
                        ""
                    ),
                    Task(
                        6,
                        "FreePrompt",
                        null,
                        "12",
                        "",
                        "Give me some text 6",
                        "Lorem".getRandomString(300),
                        "",
                        ""
                    ),
                    Task(
                        7,
                        "FreePrompt",
                        null,
                        "12",
                        "",
                        "Give me some text 7",
                        "Lorem".getRandomString(300),
                        "",
                        ""
                    )
                )
            )
        }

        return RemoteOperationResult<TaskList>(RemoteOperationResult.ResultCode.OK).apply {
            resultData = taskList
        }
    }

    override fun deleteTask(id: Long): RemoteOperationResult<Void> {
        return RemoteOperationResult<Void>(RemoteOperationResult.ResultCode.OK)
    }
}
