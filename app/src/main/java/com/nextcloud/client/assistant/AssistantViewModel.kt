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

package com.nextcloud.client.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.repository.AssistantRepository
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.assistant.model.TaskTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(client: NextcloudClient) : ViewModel() {

    private val repository: AssistantRepository = AssistantRepository(client)

    private val _taskTypes = MutableStateFlow<RemoteOperationResult<TaskTypes>?>(null)
    val taskTypes: StateFlow<RemoteOperationResult<TaskTypes>?> = _taskTypes

    /*
     private val _task = MutableStateFlow<CreatedTask?>(null)
    val task: StateFlow<CreatedTask?> = _task
     */


    init {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getTaskTypes()
            _taskTypes.update {
                result
            }
        }
    }

    /*
    fun deleteTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.deleteTask(id)
        }
    }

    fun getTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _task.update {
                repository?.getTask(id)
            }
        }
    }

    fun createTask(
        input: String,
        type: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.createTask(input, type, identifier = " ")
        }
    }
     */


    fun createTask(
        input: String,
        type: String,
    ) {
    }

}
