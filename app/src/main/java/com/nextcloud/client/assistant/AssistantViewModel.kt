/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.model.ScreenOverlayState
import com.nextcloud.client.assistant.model.ScreenState
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepository
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepository
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val accountName: String,
    private val remoteRepository: AssistantRemoteRepository,
    private val localRepository: AssistantLocalRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AssistantViewModel"
        private const val TASK_LIST_POLLING_INTERVAL_MS = 15_000L
    }

    private val _screenState = MutableStateFlow<ScreenState?>(null)
    val screenState: StateFlow<ScreenState?> = _screenState

    private val _screenOverlayState = MutableStateFlow<ScreenOverlayState?>(null)
    val screenOverlayState: StateFlow<ScreenOverlayState?> = _screenOverlayState

    private val _snackbarMessageId = MutableStateFlow<Int?>(null)
    val snackbarMessageId: StateFlow<Int?> = _snackbarMessageId

    private val _selectedTaskType = MutableStateFlow<TaskTypeData?>(null)
    val selectedTaskType: StateFlow<TaskTypeData?> = _selectedTaskType

    private val _taskTypes = MutableStateFlow<List<TaskTypeData>?>(null)
    val taskTypes: StateFlow<List<TaskTypeData>?> = _taskTypes

    private var taskList: List<Task>? = null

    private val _filteredTaskList = MutableStateFlow<List<Task>?>(null)
    val filteredTaskList: StateFlow<List<Task>?> = _filteredTaskList

    private var taskPollingJob: Job? = null

    init {
        fetchTaskTypes()
    }

    // region task polling
    fun startTaskListPolling() {
        stopTaskListPolling()

        taskPollingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    Log_OC.d(TAG, "Polling task list...")
                    fetchTaskListSuspending()
                    delay(TASK_LIST_POLLING_INTERVAL_MS)
                }
            } finally {
                Log_OC.d(TAG, "Polling coroutine cancelled")
            }
        }
    }

    fun stopTaskListPolling() {
        taskPollingJob?.cancel()
        taskPollingJob = null
    }
    // endregion

    private suspend fun fetchTaskListSuspending() {
        val cachedTasks = localRepository.getCachedTasks(accountName)
        if (cachedTasks.isNotEmpty()) {
            _filteredTaskList.value = cachedTasks.sortedByDescending { it.id }
        }

        val taskType = _selectedTaskType.value?.id ?: return
        val result = remoteRepository.getTaskList(taskType)
        if (result != null) {
            taskList = result
            _filteredTaskList.value = taskList?.sortedByDescending { it.id }
            localRepository.cacheTasks(result, accountName)
        }
    }

    @Suppress("MagicNumber")
    fun createTask(input: String, taskType: TaskTypeData) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.createTask(input, taskType)

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

    fun selectTaskType(task: TaskTypeData) {
        _selectedTaskType.update {
            task
        }

        fetchTaskList()
    }

    private fun fetchTaskTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val taskTypesResult = remoteRepository.getTaskTypes()
            if (taskTypesResult == null || taskTypesResult.isEmpty()) {
                _screenState.update {
                    ScreenState.emptyTaskTypes()
                }
                return@launch
            }

            _taskTypes.update {
                taskTypesResult
            }

            selectTaskType(taskTypesResult.first())
        }
    }

    fun fetchTaskList() {
        viewModelScope.launch(Dispatchers.IO) {
            // Try cached data first
            val cachedTasks = localRepository.getCachedTasks(accountName)
            if (cachedTasks.isNotEmpty()) {
                _filteredTaskList.update {
                    cachedTasks.sortedByDescending { it.id }
                }
                updateTaskListScreenState()
            }

            val taskType = _selectedTaskType.value?.id ?: return@launch
            val result = remoteRepository.getTaskList(taskType)
            if (result != null) {
                taskList = result
                _filteredTaskList.update {
                    taskList?.sortedByDescending { task ->
                        task.id
                    }
                }

                localRepository.cacheTasks(result, accountName)
                updateSnackbarMessage(null)
            } else {
                updateSnackbarMessage(R.string.assistant_screen_task_list_error_state_message)
            }

            updateTaskListScreenState()
        }
    }

    private fun updateTaskListScreenState() {
        _screenState.update {
            if (_filteredTaskList.value?.isEmpty() == true) {
                ScreenState.emptyTaskList()
            } else {
                ScreenState.Content
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.deleteTask(id)

            val messageId = if (result.isSuccess) {
                R.string.assistant_screen_task_delete_success_message
            } else {
                R.string.assistant_screen_task_delete_fail_message
            }

            updateSnackbarMessage(messageId)

            if (result.isSuccess) {
                removeTaskFromList(id)
                localRepository.deleteTask(id, accountName)
            }
        }
    }

    fun updateSnackbarMessage(value: Int?) {
        _snackbarMessageId.update {
            value
        }
    }

    fun updateTaskListScreenState(value: ScreenOverlayState?) {
        _screenOverlayState.update {
            value
        }
    }

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.filter { it.id != id }
        }
    }
}
