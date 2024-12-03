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
import com.nextcloud.client.assistant.model.ScreenOverlayState
import com.nextcloud.client.assistant.model.ScreenState
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

    private val _screenState = MutableStateFlow<ScreenState?>(null)
    val screenState: StateFlow<ScreenState?> = _screenState

    private val _screenOverlayState = MutableStateFlow<ScreenOverlayState?>(null)
    val screenOverlayState: StateFlow<ScreenOverlayState?> = _screenOverlayState

    private val _snackbarMessageId = MutableStateFlow<Int?>(null)
    val snackbarMessageId: StateFlow<Int?> = _snackbarMessageId

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

            updateSnackbarMessage(messageId)

            delay(2000L)
            fetchTaskList()
        }
    }

    fun selectTaskType(task: TaskType) {
        _selectedTaskType.update {
            filterTaskList(task.id)
            task
        }

        updateScreenState()
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
                updateSnackbarMessage(R.string.assistant_screen_task_types_error_state_message)
            }
        }
    }

    fun fetchTaskList(appId: String = "assistant") {
        viewModelScope.launch(Dispatchers.IO) {
            _screenState.update {
                ScreenState.Refreshing
            }

            val result = repository.getTaskList(appId)
            if (result.isSuccess) {
                taskList = result.resultData.tasks
                filterTaskList(_selectedTaskType.value?.id)
                updateSnackbarMessage(null)
            } else {
                updateSnackbarMessage(R.string.assistant_screen_task_list_error_state_message)
            }

            updateScreenState()
        }
    }

    private fun updateScreenState() {
        _screenState.update {
            if (_filteredTaskList.value?.isEmpty() == true) {
                ScreenState.EmptyContent
            } else {
                ScreenState.Content
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

            updateSnackbarMessage(messageId)

            if (result.isSuccess) {
                removeTaskFromList(id)
            }
        }
    }

    fun updateSnackbarMessage(value: Int?) {
        _snackbarMessageId.update {
            value
        }
    }

    fun updateScreenState(value: ScreenOverlayState?) {
        _screenOverlayState.update {
            value
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
