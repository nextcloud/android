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
import com.nextcloud.client.assistant.repository.AssistantRepositoryType
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(private val repository: AssistantRepositoryType) : ViewModel() {

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Error(val messageId: Int) : State()
        data class TaskCreated(val messageId: Int) : State()
        data class TaskDeleted(val messageId: Int) : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state

    private val _selectedTaskType = MutableStateFlow<TaskType?>(null)
    val selectedTaskType: StateFlow<TaskType?> = _selectedTaskType

    private val _taskTypes = MutableStateFlow<List<TaskType>?>(null)
    val taskTypes: StateFlow<List<TaskType>?> = _taskTypes

    private var _taskList: List<Task>? = null

    private val _filteredTaskList = MutableStateFlow<List<Task>?>(null)
    val filteredTaskList: StateFlow<List<Task>?> = _filteredTaskList

    init {
        getTaskTypes()
        getTaskList()
    }

    fun createTask(
        input: String,
        type: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.createTask(input, type)

            val messageId = if (result.isSuccess) {
                R.string.assistant_screen_task_create_success_message
            } else {
                R.string.assistant_screen_task_create_fail_message
            }

            _state.update {
                State.TaskCreated(messageId)
            }
        }
    }

    fun selectTaskType(task: TaskType) {
        _selectedTaskType.update {
            filterTaskList(task.id)
            task
        }
    }

    private fun getTaskTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val allTaskType = MainApp.getAppContext().getString(R.string.assistant_screen_all_task_type)
            val result = arrayListOf(TaskType(null, allTaskType, null))
            val taskTypesResult = repository.getTaskTypes()

            if (taskTypesResult.isSuccess) {
                result.addAll(taskTypesResult.resultData.types)
                _taskTypes.update {
                    result.toList()
                }

                _selectedTaskType.update {
                    result.first()
                }
            } else {
                _state.update {
                    State.Error(R.string.assistant_screen_task_types_error_state_message)
                }
            }
        }
    }

    fun getTaskList(appId: String = "assistant", onCompleted: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getTaskList(appId)
            if (result.isSuccess) {
                _taskList = result.resultData.tasks

                filterTaskList(_selectedTaskType.value?.id)

                _state.update {
                    State.Idle
                }

                onCompleted()
            } else {
                _state.update {
                    State.Error(R.string.assistant_screen_task_list_error_state_message)
                }
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteTask(id)

            val messageId = if (result.isSuccess) {
                R.string.assistant_screen_task_delete_success_message
            } else {
                R.string.assistant_screen_task_delete_fail_message
            }

            _state.update {
                State.TaskDeleted(messageId)
            }

            if (result.isSuccess) {
                removeTaskFromList(id)
            }
        }
    }

    fun resetState() {
        _state.update {
            State.Idle
        }
    }

    private fun filterTaskList(taskTypeId: String?) {
        if (taskTypeId == null) {
            _filteredTaskList.update {
                _taskList
            }
        } else {
            _filteredTaskList.update {
                _taskList?.filter { it.type == taskTypeId }
            }
        }
    }

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.filter { it.id != id }
        }
    }
}
