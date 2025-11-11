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
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
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
        private const val POLLING_INTERVAL_MS = 15_000L
    }

    private val _screenState = MutableStateFlow<AssistantScreenState?>(null)
    val screenState: StateFlow<AssistantScreenState?> = _screenState

    private val _screenOverlayState = MutableStateFlow<ScreenOverlayState?>(null)
    val screenOverlayState: StateFlow<ScreenOverlayState?> = _screenOverlayState

    private val _sessionId = MutableStateFlow<Long?>(null)
    val sessionId: StateFlow<Long?> = _sessionId

    private val _snackbarMessageId = MutableStateFlow<Int?>(null)
    val snackbarMessageId: StateFlow<Int?> = _snackbarMessageId

    private val _selectedTaskType = MutableStateFlow<TaskTypeData?>(null)
    val selectedTaskType: StateFlow<TaskTypeData?> = _selectedTaskType

    private val _taskTypes = MutableStateFlow<List<TaskTypeData>?>(null)
    val taskTypes: StateFlow<List<TaskTypeData>?> = _taskTypes

    private var taskList: List<Task>? = null

    private val _filteredTaskList = MutableStateFlow<List<Task>?>(null)
    val filteredTaskList: StateFlow<List<Task>?> = _filteredTaskList

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private var pollingJob: Job? = null

    init {
        fetchTaskTypes()
    }

    // region task polling
    fun startPolling(sessionId: Long?) {
        stopPolling()

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    Log_OC.d(TAG, "Polling list...")
                    if (sessionId != null) {
                        pollChatMessages(sessionId)
                    } else {
                        pollTaskList()
                    }
                    delay(POLLING_INTERVAL_MS)
                }
            } finally {
                Log_OC.d(TAG, "Polling coroutine cancelled")
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    // endregion

    private suspend fun pollTaskList() {
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

    private fun pollChatMessages(sessionId: Long) {
        val result = remoteRepository.fetchChatMessages(sessionId)
        if (result != null) {
            _chatMessages.update {
                result
            }
        }
    }

    fun sendChatMessage(content: String, sessionId: Long) {
        val timestamp = System.currentTimeMillis().div(1000)
        val firstHumanMessage = _chatMessages.value.isEmpty()
        val request =
            ChatMessageRequest(
                sessionId = sessionId.toString(),
                role = "human",
                content = content,
                timestamp = timestamp,
                firstHumanMessage = firstHumanMessage
            )

        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.sendChatMessage(request)
            if (result != null) {
                fetchChatMessages(sessionId)
            } else {
                _snackbarMessageId.update {
                    R.string.assistant_screen_chat_create_error
                }
            }
        }
    }

    fun initSessionId(value: Long) {
        _sessionId.update {
            value
        }
    }

    fun fetchChatMessages(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.fetchChatMessages(sessionId)
            if (result != null) {
                _screenState.update {
                    AssistantScreenState.ChatContent
                }

                _chatMessages.update {
                    result
                }
            } else {
                _snackbarMessageId.update {
                    R.string.assistant_screen_chat_fetch_error
                }
            }
        }
    }

    fun createConversation(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.createConversation(title)
            if (result != null) {
                sendChatMessage(content = title, sessionId = result.session.id)
            }
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
                    AssistantScreenState.emptyTaskTypes()
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
                updateScreenState()
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

            updateScreenState()
        }
    }

    private fun updateScreenState() {
        val isChat = _selectedTaskType.value?.isChat ?: false

        _screenState.update {
            if (isChat) {
                AssistantScreenState.ChatContent
            } else {
                if (_filteredTaskList.value?.isEmpty() == true) {
                    AssistantScreenState.emptyTaskList()
                } else {
                    AssistantScreenState.TaskContent
                }
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

    fun updateScreenState(value: ScreenOverlayState?) {
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
