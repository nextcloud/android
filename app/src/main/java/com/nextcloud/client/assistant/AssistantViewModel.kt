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
import com.nextcloud.client.assistant.model.AssistantScreenState
import com.nextcloud.client.assistant.model.ScreenOverlayState
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepository
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepository
import com.nextcloud.utils.TimeConstants.MILLIS_PER_SECOND
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class AssistantViewModel(
    private val accountName: String,
    private val remoteRepository: AssistantRemoteRepository,
    private val localRepository: AssistantLocalRepository,
    sessionIdArg: Long?
) : ViewModel() {

    companion object {
        private const val TAG = "AssistantViewModel"
        private const val POLLING_INTERVAL_MS = 15_000L
    }

    private val _inputBarText = MutableStateFlow("")
    val inputBarText: StateFlow<String> = _inputBarText

    private val _screenState = MutableStateFlow<AssistantScreenState?>(null)
    val screenState: StateFlow<AssistantScreenState?> = _screenState

    private val _screenOverlayState = MutableStateFlow<ScreenOverlayState?>(null)
    val screenOverlayState: StateFlow<ScreenOverlayState?> = _screenOverlayState

    private val _sessionId = MutableStateFlow(sessionIdArg)
    val sessionId: StateFlow<Long?> = _sessionId

    private val _snackbarMessageId = MutableStateFlow<Int?>(null)
    val snackbarMessageId: StateFlow<Int?> = _snackbarMessageId

    private val _isTranslationTask = MutableStateFlow(false)
    val isTranslationTask: StateFlow<Boolean> = _isTranslationTask

    private val selectedTask = MutableStateFlow<Task?>(null)

    private val _selectedTaskType = MutableStateFlow<TaskTypeData?>(null)
    val selectedTaskType: StateFlow<TaskTypeData?> = _selectedTaskType

    private val _taskTypes = MutableStateFlow<List<TaskTypeData>?>(null)
    val taskTypes: StateFlow<List<TaskTypeData>?> = _taskTypes

    private var taskList: List<Task>? = null

    private val _filteredTaskList = MutableStateFlow<List<Task>?>(null)
    val filteredTaskList: StateFlow<List<Task>?> = _filteredTaskList

    private var pollingJob: Job? = null

    init {
        observeScreenState()
        fetchTaskTypes()
    }

    // region task polling
    fun startPolling(sessionId: Long?) {
        stopPolling()

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    delay(POLLING_INTERVAL_MS)
                    val taskType = _selectedTaskType.value ?: continue
                    if (!taskType.isChat()) {
                        Log_OC.d(TAG, "Polling task list")
                        pollTaskList()
                    }
                }
            } finally {
                Log_OC.d(TAG, "Polling cancelled, sessionId: $sessionId")
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    // endregion

    private suspend fun pollTaskList() {
        val taskType = _selectedTaskType.value?.id ?: return

        val cachedTasks = localRepository.getCachedTasks(accountName, taskType)
        if (cachedTasks.isNotEmpty()) {
            _filteredTaskList.value = cachedTasks.sortedByDescending { it.id }
        }

        val result = remoteRepository.getTaskList(taskType)
        if (result != null) {
            taskList = result
            _filteredTaskList.value = taskList?.sortedByDescending { it.id }
            localRepository.cacheTasks(result, accountName)
        }
    }

    private fun observeScreenState() {
        viewModelScope.launch {
            combine(
                selectedTask,
                _selectedTaskType,
                _filteredTaskList
            ) { selectedTask, selectedTaskType, tasks ->
                val isChat = selectedTaskType?.isChat() == true
                val isTranslation =
                    selectedTaskType?.isTranslate() == true && selectedTask?.isTranslate() == true

                when {
                    selectedTaskType == null -> AssistantScreenState.Loading
                    isTranslation -> AssistantScreenState.Translation(selectedTask)
                    isChat && chats.isEmpty() -> AssistantScreenState.emptyChatList()
                    isChat -> AssistantScreenState.ChatContent
                    !isChat && (tasks == null || tasks.isEmpty()) -> AssistantScreenState.emptyTaskList()
                    else -> {
                        if (!_isTranslationTask.value) {
                            AssistantScreenState.TaskContent
                        } else {
                            _screenState.value
                        }
                    }
                }
            }.collect { newState ->
                _screenState.value = newState
            }
        }
    }

    // region task
    fun createTask(input: String, taskType: TaskTypeData) = viewModelScope.launch(Dispatchers.IO) {
        val result = remoteRepository.createTask(input, taskType)
        val message = if (result.isSuccess) {
            R.string.assistant_screen_task_create_success_message
        } else {
            R.string.assistant_screen_task_create_fail_message
        }

        updateSnackbarMessage(message)
        delay(MILLIS_PER_SECOND * 2L)
        fetchTaskList()
    }

    fun selectTaskType(task: TaskTypeData) {
        Log_OC.d(TAG, "Task type changed: ${task.name}, session id: ${_sessionId.value}")

        // clear task list immediately when task type change
        if (_selectedTaskType.value != task) {
            _filteredTaskList.update {
                listOf()
            }
        }

        updateTaskType(task)

        if (!task.isChat()) {
            fetchTaskList()
        }
    }

    private fun fetchTaskTypes() = viewModelScope.launch(Dispatchers.IO) {
        val result = remoteRepository.fetchTaskTypes()
        if (result.isNullOrEmpty()) {
            _screenState.value = AssistantScreenState.emptyTaskTypes()
            return@launch
        }

        _taskTypes.update {
            result
        }
        selectTaskType(result.first())
    }

    fun fetchTaskList() = viewModelScope.launch(Dispatchers.IO) {
        val taskType = _selectedTaskType.value ?: return@launch

        val cached = localRepository.getCachedTasks(accountName, taskType.name)
        if (cached.isNotEmpty()) {
            _filteredTaskList.update {
                cached.sortedByDescending { it.id }
            }
        }

        taskType.id?.let { typeId ->
            remoteRepository.getTaskList(typeId)?.let { result ->
                taskList = result
                _filteredTaskList.value = result.sortedByDescending { it.id }
                localRepository.cacheTasks(result, accountName)
                updateSnackbarMessage(null)
            } ?: updateSnackbarMessage(R.string.assistant_screen_task_list_error_state_message)
        }
    }

    fun deleteTask(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val result = remoteRepository.deleteTask(id)
        val message = if (result.isSuccess) {
            R.string.assistant_screen_task_delete_success_message
        } else {
            R.string.assistant_screen_task_delete_fail_message
        }

        updateSnackbarMessage(message)

        val taskType = _selectedTaskType.value ?: return@launch
        if (result.isSuccess) {
            removeTaskFromList(id)
            localRepository.deleteTask(id, accountName, taskType.name)
        }
    }
    // endregion

    private fun updateTaskType(value: TaskTypeData) {
        _selectedTaskType.update {
            value
        }
    }

    fun selectTask(task: Task?) {
        selectedTask.update {
            task
        }
    }

    fun updateSnackbarMessage(value: Int?) {
        _snackbarMessageId.update {
            value
        }
    }

    fun updateScreenOverlayState(value: ScreenOverlayState?) {
        _screenOverlayState.update {
            value
        }
    }

    fun updateInputBarText(value: String) {
        _inputBarText.update {
            value
        }
    }

    fun updateScreenState(state: AssistantScreenState) {
        _screenState.update {
            state
        }
    }

    fun updateTranslationTaskState(value: Boolean) {
        _isTranslationTask.update {
            value
        }
    }

    fun onTranslationScreenDismissed() {
        updateInputBarText("")
        updateTranslationTaskState(false)
        selectTask(null)
    }

    fun getRemoteRepository(): AssistantRemoteRepository = remoteRepository

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.filter { it.id != id }
        }
    }
}
