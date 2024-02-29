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
import com.owncloud.android.lib.resources.assistant.model.TaskList
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.lib.resources.assistant.model.TaskTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(client: NextcloudClient) : ViewModel() {

    private val repository: AssistantRepository = AssistantRepository(client)

    private val _selectedTaskType = MutableStateFlow<TaskType?>(null)
    val selectedTaskType: StateFlow<TaskType?> = _selectedTaskType

    private val _taskTypes = MutableStateFlow<RemoteOperationResult<TaskTypes>?>(null)
    val taskTypes: StateFlow<RemoteOperationResult<TaskTypes>?> = _taskTypes

    private val _taskList = MutableStateFlow<RemoteOperationResult<TaskList>?>(null)
    val taskList: StateFlow<RemoteOperationResult<TaskList>?> = _taskList

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _isTaskCreated = MutableStateFlow(false)
    val isTaskCreated: StateFlow<Boolean> = _isTaskCreated

    private val _isTaskDeleted = MutableStateFlow(false)
    val isTaskDeleted: StateFlow<Boolean> = _isTaskDeleted

    init {
        getTaskTypes()
        getTaskList()
    }

    fun createTask(
        input: String,
        type: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.createTask(input, type)

            _isTaskCreated.update {
                result.isSuccess
            }
        }
    }

    fun selectTask(task: TaskType) {
        _selectedTaskType.update {
            task
        }
    }

    private fun getTaskTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getTaskTypes()

            _taskTypes.update {
                result
            }

            _selectedTaskType.update {
                result.resultData.types.first()
            }
        }
    }

    private fun getTaskList(appId: String = "assistant") {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getTaskList(appId)

            _taskList.update {
                result
            }

            _loading.update {
                false
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteTask(id)

            _isTaskDeleted.update {
                if (result.isSuccess) {
                    removeTaskFromList(id)
                }

                result.isSuccess
            }
        }
    }

    private fun removeTaskFromList(id: Long) {
        _taskList.update { currentList ->
            currentList?.resultData?.tasks?.let { tasks ->
                currentList.resultData.tasks = tasks.filter { it.id != id }
            }
            currentList
        }
    }
}
