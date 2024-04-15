/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.assistant.repository

import com.nextcloud.utils.extensions.getRandomString
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
                        "Give me some long text",
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc consectetur condimentum augue, sit amet maximus nibh pharetra sodales. Donec blandit nulla vitae diam aliquam, vel viverra sem faucibus. Duis vitae pretium sapien. Curabitur nec libero erat. Nunc pretium eleifend mi non congue. Sed efficitur ullamcorper mi, ac faucibus mi mollis sed. Donec vestibulum enim vel massa sodales facilisis. Integer in consequat nibh. Sed id nisi erat. Donec sollicitudin pharetra massa, id sodales arcu mollis luctus. Donec sed ullamcorper nisl, non euismod metus. Proin eget sollicitudin purus.\n" +
                            "\n" +
                            "Etiam sit amet nisl pretium, facilisis ligula vel, luctus arcu. Nunc bibendum hendrerit ultricies. Integer scelerisque sem arcu, eget fermentum neque pretium in. Curabitur facilisis neque vel leo blandit tincidunt. Nullam at sodales nisl, eu porta leo. Nulla facilisi. Fusce cursus, turpis eget auctor iaculis, eros lectus posuere magna, a tristique purus nunc id quam.\n" +
                            "\n" +
                            "Nullam faucibus mauris eget magna vehicula auctor. Aliquam molestie purus quis magna mattis, sed commodo dolor condimentum. Mauris hendrerit libero ut tellus rutrum, et sagittis diam luctus. Nunc non semper neque, eget scelerisque tortor. Donec hendrerit faucibus dolor, at congue orci dignissim nec. Duis vel interdum elit, maximus elementum orci. Phasellus ut ligula id sapien dictum euismod. Vestibulum tincidunt vitae orci a congue. Nunc nec commodo urna, quis vulputate orci. Suspendisse euismod urna orci. Phasellus in metus lobortis, auctor mauris vel, venenatis nulla.\n" +
                            "\n" +
                            "Quisque lectus felis, placerat eget consequat quis, mattis et nisi. In bibendum in orci fermentum rhoncus. Nam eu nibh ex. Cras vel ligula eu quam pharetra ullamcorper. Integer a ultricies eros, at rutrum ligula. Nam laoreet convallis velit sit amet vulputate. In eleifend interdum risus, pulvinar dictum tellus. Fusce in posuere mauris, sed commodo urna. Etiam a ante id felis viverra commodo vel sed elit. Maecenas in libero turpis. Donec non elit feugiat, ullamcorper massa sit amet, lobortis turpis. Fusce mollis felis eu elementum ornare. Nulla facilisi.\n" +
                            "\n" +
                            "Curabitur sed erat vel urna luctus consequat sit amet et lacus. Donec eu tempus sapien. Morbi commodo finibus sapien, in consequat dui. Etiam ac odio magna. Cras ut nisl scelerisque, consectetur ante et, laoreet diam. Morbi efficitur, nibh ac volutpat rhoncus, urna eros cursus ipsum, non vulputate magna ipsum sit amet risus. Donec at arcu ullamcorper, pretium augue porttitor, dapibus arcu. Ut mollis velit sed tristique maximus. Duis iaculis porta ligula iaculis congue. Quisque laoreet ligula euismod faucibus consectetur. Nunc sit amet quam venenatis, dignissim nisl id, commodo augue. Nulla faucibus dui nec tortor viverra, at posuere orci sollicitudin. Sed fringilla porta lectus, id pretium tortor mattis eget. Cras suscipit mi pharetra, eleifend nisl quis, ultrices eros. Maecenas a nulla in dui blandit pellentesque non et mi.\n" +
                            "\n" +
                            "Curabitur purus mauris, scelerisque vitae metus vitae, pellentesque suscipit eros. Morbi tincidunt consequat felis at sagittis. Aenean purus elit, porta eu fringilla vitae, tincidunt ac leo. Quisque non neque et eros pretium sagittis vel sit amet sem. Fusce nec finibus elit. Proin scelerisque libero a mi blandit, quis accumsan dolor interdum. Praesent aliquet nisi in vehicula viverra. Nunc vitae accumsan neque. Donec.",
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
                        "Give me some text",
                        "Lorem".getRandomString(300),
                        "",
                        ""
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
