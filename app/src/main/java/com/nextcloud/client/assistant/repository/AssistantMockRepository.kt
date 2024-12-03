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
import com.owncloud.android.lib.resources.assistant.model.TaskInput
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskOutput
import com.owncloud.android.lib.resources.assistant.model.TaskTypes

@Suppress("MagicNumber")
class AssistantMockRepository(private val giveEmptyTasks: Boolean = false) : AssistantRepositoryType {
    override fun getTaskTypes(): RemoteOperationResult<TaskTypes> {
        return RemoteOperationResult<TaskTypes>(RemoteOperationResult.ResultCode.OK).apply {
            resultData = null
        }
    }

    override fun createTask(input: String, type: String): RemoteOperationResult<Void> {
        return RemoteOperationResult<Void>(RemoteOperationResult.ResultCode.OK)
    }

    override fun getTaskList(taskType: String): RemoteOperationResult<TaskList> {
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
                        TaskInput("Give me some long text 1"),
                        TaskOutput("Lorem ipsum".getRandomString(100)),
                        1707692337,
                        1707692337,
                        1707692337,
                        1707692337,
                        1707692337,
                    ),
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
