/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.assistant.repository

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.lib.resources.assistant.model.TaskTypes

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
                        "Give me some text",
                        "Lorem Ipsum is simply dummy text of the printing and typesetting industry. " +
                            "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s," +
                            " when an unknown printer took a galley of type and scrambled it to make a type" +
                            " specimen book. It has survived not only five centuries, " +
                            "but also the leap into electronic typesetting, remaining essentially unchanged." +
                            " It was popularised in the 1960s with the release of Letraset sheets containing " +
                            "Lorem Ipsum passages, and more recently with desktop publishing software like Aldus" +
                            " PageMaker including versions of Lorem Ipsum",
                        "",
                        ""
                    ),
                    Task(
                        2,
                        "GenerateHeadline",
                        null,
                        "12",
                        "",
                        "Give me some text 2",
                        "Lorem Ipsum is simply dummy text of the printing and typesetting industry.",
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
