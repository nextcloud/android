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
import com.nextcloud.utils.extensions.isHuman
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.assistant.v2.model.TranslationLanguage
import com.owncloud.android.lib.resources.assistant.v2.model.TranslationRequest
import com.owncloud.android.lib.resources.assistant.v2.model.toTranslationModel
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

    private val _isTranslationTaskCreated = MutableStateFlow(false)
    val isTranslationTaskCreated: StateFlow<Boolean> = _isTranslationTaskCreated

    private val _translationTaskOutput = MutableStateFlow("")
    val translationTaskOutput: StateFlow<String> = _translationTaskOutput

    private val selectedTask = MutableStateFlow<Task?>(null)

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

                    if (taskType.isChat() && sessionId != null) {
                        Log_OC.d(TAG, "Polling chat messages, sessionId: $sessionId")

                        if (currentChatTaskId == null) {
                            remoteRepository.generateSession(sessionId.toString())?.let {
                                currentChatTaskId = it.taskId.toString()
                            }
                        }

                        fetchNewChatMessage(sessionId)
                    } else if (!taskType.isChat()) {
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
                selectedTask,
                _selectedTaskType,
                _chatMessages,
                _filteredTaskList
            ) { selectedTask, selectedTaskType, chats, tasks ->
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

    // region translation
    fun translate(textToTranslate: String, originLanguage: TranslationLanguage, targetLanguage: TranslationLanguage) {
        viewModelScope.launch(Dispatchers.IO) {
            val taskType = _selectedTaskType.value
            if (taskType == null) {
                _snackbarMessageId.update {
                    R.string.assistant_screen_select_task
                }
                return@launch
            }

            val model = taskType.toTranslationModel()

            if (model == null) {
                _snackbarMessageId.update {
                    R.string.translation_screen_error_message
                }
                return@launch
            }

            val input = TranslationRequest(
                input = textToTranslate,
                originLanguage = originLanguage.code,
                targetLanguage = targetLanguage.code,
                maxTokens = model.maxTokens,
                model = model.model
            )

            val result = remoteRepository.translate(input, taskType)
            if (result.isSuccess) {
                _isTranslationTaskCreated.update { true }

                val selectedTaskId = selectedTask.value?.id ?: return@launch

                pollTranslationResult(
                    taskType = taskType,
                    selectedTaskId = selectedTaskId
                )

                _isTranslationTaskCreated.update { false }
            }
        }
    }

    private suspend fun pollTranslationResult(
        taskType: TaskTypeData,
        selectedTaskId: Long,
        maxRetries: Int = 3,
    ) {
        val taskTypeId = taskType.id ?: return

        repeat(maxRetries) { attempt ->
            val translationTasks = remoteRepository.getTaskList(taskTypeId)
            val translationResult = translationTasks
                ?.find { it.id == selectedTaskId }
                ?.output
                ?.output

            if (!translationResult.isNullOrBlank()) {
                _translationTaskOutput.update { translationResult }
                return
            }

            Log_OC.d(TAG, "Translation not ready yet (attempt ${attempt + 1}/$maxRetries)")

            if (attempt < maxRetries - 1) {
                delay(POLLING_INTERVAL_MS)
            }
        }

        Log_OC.w(TAG, "Translation polling finished but result is still empty")
        updateSnackbarMessage(R.string.translation_screen_task_processing)
        onTranslationScreenDismissed()
    }
    // endregion


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

        // clear task list immediately when task type change
        if (_selectedTaskType.value != task) {
            _filteredTaskList.update {
                listOf()
            }
        }

        updateTaskType(task)

        if (!task.isChat()) {
            fetchTaskList()
            return
        }

        // only task chat type needs to be handled differently
        val sessionId = _sessionId.value ?: return
        if (_chatMessages.value.isEmpty()) {
            fetchChatMessages(sessionId)
        } else {
            fetchNewChatMessage(sessionId)
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

    fun updateTranslationTaskCreation(value: Boolean) {
        _isTranslationTaskCreated.update {
            value
        }
    }

    fun onTranslationScreenDismissed() {
        updateTranslationTaskCreation(false)
        updateTranslationTaskState(false)
        selectTask(null)
    }

    private fun removeTaskFromList(id: Long) {
        _filteredTaskList.update { currentList ->
            currentList?.filter { it.id != id }
        }
    }
}
