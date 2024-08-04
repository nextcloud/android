/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.repository.AssistantRepositoryType
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class AssistantViewModel(
    private val repository: AssistantRepositoryType,
    private val context: WeakReference<Context>
) : ViewModel() {

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

    private var taskList: List<Task>? = null

    private val _filteredTaskList = MutableStateFlow<List<Task>?>(null)
    val filteredTaskList: StateFlow<List<Task>?> = _filteredTaskList

    init {
        fetchTaskTypes()
        fetchTaskList()
    }

    @Suppress("MagicNumber")
    fun createTask(input: String, type: String) {
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

            delay(2000L)
            fetchTaskList()
        }
    }

    fun selectTaskType(task: TaskType) {
        _selectedTaskType.update {
            filterTaskList(task.id)
            task
        }
    }

    private fun fetchTaskTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val allTaskType = context.get()?.getString(R.string.assistant_screen_all_task_type)
            val excludedIds = listOf("OCA\\ContextChat\\TextProcessing\\ContextChatTaskType")
            val result = arrayListOf(TaskType(null, allTaskType, null))
            val taskTypesResult = repository.getTaskTypes()

            if (taskTypesResult.isSuccess) {
                val excludedTaskTypes = taskTypesResult.resultData.types.filter { item -> item.id !in excludedIds }
                result.addAll(excludedTaskTypes)
                _taskTypes.update {
                    result.toList()
                }

                selectTaskType(result.first())
            } else {
                _state.update {
                    State.Error(R.string.assistant_screen_task_types_error_state_message)
                }
            }
        }
    }

    fun fetchTaskList(appId: String = "assistant", onCompleted: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getTaskList(appId)
            if (result.isSuccess) {
                taskList = result.resultData.tasks

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
                taskList
            }
        } else {
            _filteredTaskList.update {
                taskList?.filter { it.type == taskTypeId }
            }
        }

        _filteredTaskList.update {
            it?.sortedByDescending { task ->
                task.id
            }
        }
    }

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.filter { it.id != id }
        }
    }
}
