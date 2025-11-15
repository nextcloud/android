/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.assistant.repository.remote

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
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

class AssistantRemoteRepositoryImpl(private val client: NextcloudClient, capability: OCCapability) :
    AssistantRemoteRepository {

    private val supportsV2 = capability.version.isNewerOrEqual(NextcloudVersion.nextcloud_30)

    @Suppress("ReturnCount")
    override fun getTaskTypes(): List<TaskTypeData>? {
        if (supportsV2) {
            val result = GetTaskTypesRemoteOperationV2().execute(client)
            if (result.isSuccess) {
                return result.resultData
            }
        } else {
            val result = GetTaskTypesRemoteOperationV1().execute(client)
            if (result.isSuccess) {
                return result.resultData.toV2()
            }
        }

        return null
    }

    override fun createTask(input: String, taskType: TaskTypeData): RemoteOperationResult<Void> = if (supportsV2) {
        CreateTaskRemoteOperationV2(input, taskType).execute(client)
    } else {
        if (taskType.id.isNullOrEmpty()) {
            RemoteOperationResult<Void>(ResultCode.CANCELLED)
        } else {
            CreateTaskRemoteOperationV1(input, taskType.id!!).execute(client)
        }
    }

    @Suppress("ReturnCount")
    override fun getTaskList(taskType: String): List<Task>? {
        if (supportsV2) {
            val result = GetTaskListRemoteOperationV2(taskType).execute(client)
            if (result.isSuccess) {
                return result.resultData.tasks.filter { it.appId == "assistant" }
            }
        } else {
            val result = GetTaskListRemoteOperationV1("assistant").execute(client)
            if (result.isSuccess) {
                return result.resultData.toV2().tasks.filter { it.type == taskType }
            }
        }

        return null
    }

    override fun deleteTask(id: Long): RemoteOperationResult<Void> = if (supportsV2) {
        DeleteTaskRemoteOperationV2(id).execute(client)
    } else {
        DeleteTaskRemoteOperationV1(id).execute(client)
    }
}
