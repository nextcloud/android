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

    private val _isAssistantAnswering = MutableStateFlow(false)
    val isAssistantAnswering: StateFlow<Boolean> = _isAssistantAnswering

    private var pollingJob: Job? = null
    private var currentChatTaskId: String? = null

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

                    if (taskType.isChat && sessionId != null) {
                        Log_OC.d(TAG, "Polling chat messages, sessionId: $sessionId")

                        if (currentChatTaskId == null) {
                            remoteRepository.generateSession(sessionId.toString())?.let {
                                currentChatTaskId = it.taskId.toString()
                            }
                        }

                        fetchNewChatMessage(sessionId)
                    } else if (!taskType.isChat) {
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

    fun fetchNewChatMessage(sessionId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val taskId = currentChatTaskId ?: return@launch
        val newMessage = remoteRepository.checkGeneration(taskId, sessionId.toString()) ?: return@launch

        _chatMessages.update { current ->
            val messageExists = current.any {
                it.id == newMessage.id ||
                    (it.timestamp == newMessage.timestamp && it.content == newMessage.content)
            }

            if (messageExists) {
                current
            } else {
                if (!newMessage.isHuman()) {
                    _isAssistantAnswering.update {
                        false
                    }
                }
                current + newMessage
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

    // region chat
    fun sendChatMessage(content: String, sessionId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val request = ChatMessageRequest(
            sessionId = sessionId.toString(),
            role = "human",
            content = content,
            timestamp = System.currentTimeMillis() / MILLIS_PER_SECOND,
            firstHumanMessage = _chatMessages.value.isEmpty()
        )

        remoteRepository.sendChatMessage(request)?.let { newMessage ->
            _chatMessages.update { messages ->
                messages + newMessage
            }
            _isAssistantAnswering.update {
                true
            }
        } ?: updateSnackbarMessage(R.string.assistant_screen_chat_create_error)
    }

    fun fetchChatMessages(sessionId: Long) = viewModelScope.launch(Dispatchers.IO) {
        remoteRepository.fetchChatMessages(sessionId)?.let { messageList ->
            _chatMessages.update {
                messageList
            }
        } ?: updateSnackbarMessage(R.string.assistant_screen_chat_fetch_error)
    }

    fun createConversation(title: String) = viewModelScope.launch(Dispatchers.IO) {
        remoteRepository.createConversation(title)?.let { result ->
            initSessionId(result.session.id)
            sendChatMessage(title, result.session.id)
        }
    }

    fun initSessionId(value: Long) {
        Log_OC.d(TAG, "session id updated: $value")
        currentChatTaskId = null
        _sessionId.update { value }
    }
    // endregion

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
        updateTaskType(task)

        val sessionId = _sessionId.value ?: return
        if (task.isChat) {
            if (_chatMessages.value.isEmpty()) {
                fetchChatMessages(sessionId)
            } else {
                fetchNewChatMessage(sessionId)
            }
        } else {
            fetchTaskList()
        }
    }

    private fun fetchTaskTypes() = viewModelScope.launch(Dispatchers.IO) {
        val result = remoteRepository.getTaskTypes()
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
        val cached = localRepository.getCachedTasks(accountName)
        if (cached.isNotEmpty()) {
            _filteredTaskList.value = cached.sortedByDescending { it.id }
        }

        _selectedTaskType.value?.id?.let { typeId ->
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
        if (result.isSuccess) {
            removeTaskFromList(id)
            localRepository.deleteTask(id, accountName)
        }
    }
    // endregion

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
