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

    private var _taskList: RemoteOperationResult<TaskList>? = null

    private val _filteredTaskList = MutableStateFlow<RemoteOperationResult<TaskList>?>(null)
    val filteredTaskList: StateFlow<RemoteOperationResult<TaskList>?> = _filteredTaskList

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _isTaskCreated = MutableStateFlow(false)
    val isTaskCreated: StateFlow<Boolean> = _isTaskCreated

    private val _isTaskDeleted = MutableStateFlow<Boolean?>(null)
    val isTaskDeleted: StateFlow<Boolean?> = _isTaskDeleted

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

    fun selectTaskType(task: TaskType) {
        _selectedTaskType.update {
            filterTaskList(it?.id)
            task
        }
    }

    private fun getTaskTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getTaskTypes()
            val new = ArrayList(result.resultData.types ?: listOf()).apply {
                add(TaskType(null, "All", null))
            }

            result.resultData.types = new

            _taskTypes.update {
                result
            }

            _selectedTaskType.update {
                result.resultData.types?.first()
            }
        }
    }

    fun getTaskList(appId: String = "assistant", onCompleted: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            _taskList = repository.getTaskList(appId)

            filterTaskList(_selectedTaskType.value?.id)

            _loading.update {
                false
            }

            onCompleted()
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

    fun filterTaskList(taskTypeId: String?) {
        if (taskTypeId == null) {
            _filteredTaskList.update {
                _taskList
            }
        } else {
            val result = _taskList?.resultData?.tasks?.filter { it.type == taskTypeId }
            _filteredTaskList.update {
                it?.resultData?.tasks = result
                it
            }
        }
    }

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.resultData?.tasks?.let { tasks ->
                currentList.resultData.tasks = tasks.filter { it.id != id }
            }
            currentList
        }
    }
}
