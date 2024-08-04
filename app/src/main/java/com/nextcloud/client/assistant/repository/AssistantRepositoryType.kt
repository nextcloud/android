/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.repository

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskTypes

interface AssistantRepositoryType {
    fun getTaskTypes(): RemoteOperationResult<TaskTypes>

    fun createTask(input: String, type: String): RemoteOperationResult<Void>

    fun getTaskList(appId: String): RemoteOperationResult<TaskList>

    fun deleteTask(id: Long): RemoteOperationResult<Void>
}
