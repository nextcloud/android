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

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.CreateTaskRemoteOperation
import com.owncloud.android.lib.resources.assistant.DeleteTaskRemoteOperation
import com.owncloud.android.lib.resources.assistant.GetTaskListRemoteOperation
import com.owncloud.android.lib.resources.assistant.GetTaskTypesRemoteOperation
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskTypes

class AssistantRepository(private val client: NextcloudClient) : AssistantRepositoryType {

    override fun getTaskTypes(): RemoteOperationResult<TaskTypes> {
        return GetTaskTypesRemoteOperation().execute(client)
    }

    override fun createTask(
        input: String,
        type: String
    ): RemoteOperationResult<Void> {
        return CreateTaskRemoteOperation(input, type).execute(client)
    }

    override fun getTaskList(appId: String): RemoteOperationResult<TaskList> {
        return GetTaskListRemoteOperation(appId).execute(client)
    }

    override fun deleteTask(id: Long): RemoteOperationResult<Void> {
        return DeleteTaskRemoteOperation(id).execute(client)
    }
}
