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
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
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

    private val _screenState = MutableStateFlow<AssistantScreenState?>(null)
    val screenState: StateFlow<AssistantScreenState?> = _screenState

    private val _screenOverlayState = MutableStateFlow<ScreenOverlayState?>(null)
    val screenOverlayState: StateFlow<ScreenOverlayState?> = _screenOverlayState

    private val _sessionId = MutableStateFlow(sessionIdArg)
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
        observeScreenState()
        fetchTaskTypes()
    }

    // region task polling
    fun startPolling(sessionId: Long?) {
        stopPolling()

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val isChat = (_selectedTaskType.value?.isChat == true)

                    if (isChat && sessionId != null) {
                        Log_OC.d(TAG, "Polling chat messages, sessionId: $sessionId")
                        pollChatMessages(sessionId)
                    } else if (!isChat) {
                        Log_OC.d(TAG, "Polling task list")
                        pollTaskList()
                    }

                    delay(POLLING_INTERVAL_MS)
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

    private fun observeScreenState() {
        viewModelScope.launch {
            combine(
                _selectedTaskType,
                _chatMessages,
                _filteredTaskList
            ) { selectedTask, chats, tasks ->
                val isChat = selectedTask?.isChat == true

                when {
                    selectedTask == null -> AssistantScreenState.Loading
                    isChat && chats.isEmpty() -> AssistantScreenState.emptyChatList()
                    isChat -> AssistantScreenState.ChatContent
                    !isChat && (tasks == null || tasks.isEmpty()) -> AssistantScreenState.emptyTaskList()
                    else -> AssistantScreenState.TaskContent
                }
            }.collect { newState ->
                _screenState.value = newState
            }
        }
    }

    fun sendChatMessage(content: String, sessionId: Long) {
        val timestamp = System.currentTimeMillis().div(MILLIS_PER_SECOND)
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
                updateSnackbarMessage(R.string.assistant_screen_chat_create_error)
            }
        }
    }

    fun fetchChatMessages(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.fetchChatMessages(sessionId)
            if (result != null) {
                _chatMessages.update {
                    result
                }
            } else {
                updateSnackbarMessage(R.string.assistant_screen_chat_fetch_error)
            }
        }
    }

    fun createConversation(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.createConversation(title)
            if (result != null) {
                initSessionId(result.session.id)
                sendChatMessage(content = title, sessionId = result.session.id)
            }
        }
    }

    fun initSessionId(value: Long) {
        Log_OC.d(TAG, "session id updated: $value")

        _sessionId.update {
            value
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
        Log_OC.d(TAG, "task type changed: ${task.name}, session id: ${_sessionId.value}")

        updateTaskType(task)

        if (task.isChat) {
            val sessionId = _sessionId.value ?: return
            fetchChatMessages(sessionId)
        } else {
            fetchTaskList()
        }
    }

    private fun fetchTaskTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            val taskTypesResult = remoteRepository.getTaskTypes()
            if (taskTypesResult.isNullOrEmpty()) {
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

    private fun updateTaskType(value: TaskTypeData) {
        _selectedTaskType.update {
            value
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

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.filter { it.id != id }
        }
    }
}
