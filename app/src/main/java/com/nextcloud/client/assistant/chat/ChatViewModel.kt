/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepository
import com.nextcloud.utils.TimeConstants.MILLIS_PER_SECOND
import com.nextcloud.utils.extensions.isHuman
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(private val remoteRepository: AssistantRemoteRepository) : ViewModel() {

    companion object {
        private const val POLLING_INTERVAL_MS = 4_000L
    }

    private val _uiState = MutableStateFlow<ChatUIState>(ChatUIState.Empty)
    val uiState: StateFlow<ChatUIState> = _uiState

    private val _sessionTitle = MutableStateFlow<String?>(null)
    val sessionTitle: StateFlow<String?> = _sessionTitle

    private val _sessionId = MutableStateFlow<Long?>(null)
    val sessionId: StateFlow<Long?> = _sessionId

    private var currentMessages: List<ChatMessage> = emptyList()
    private var currentChatTaskId: String? = null
    private var pollingJob: Job? = null

    fun selectConversation(sessionId: Long) {
        _sessionId.update { sessionId }
        currentChatTaskId = null

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { ChatUIState.Loading }

            fetchAllMessages(sessionId)

            val session = remoteRepository.checkSession(sessionId.toString())
            currentChatTaskId = session?.taskId?.toString()

            val lastMessageIsHuman = currentMessages.lastOrNull()?.isHuman() == true

            _sessionTitle.update {
                session?.sessionTitle
            }

            when {
                currentChatTaskId != null && currentChatTaskId != "0" -> startPolling(sessionId)
                lastMessageIsHuman -> _uiState.update { ChatUIState.RetryAvailable(currentMessages) }
                else -> _uiState.update {
                    if (currentMessages.isEmpty()) {
                        ChatUIState.Empty
                    } else {
                        ChatUIState.Content(currentMessages)
                    }
                }
            }
        }
    }

    fun startPolling(sessionId: Long) {
        stopPolling()
        _uiState.update { ChatUIState.Thinking(currentMessages) }

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                fetchNewChatMessage(sessionId)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchAllMessages(sessionId: Long) {
        val messages = remoteRepository.fetchChatMessages(sessionId)
        if (messages != null) {
            currentMessages = messages
            _uiState.update {
                if (messages.isEmpty()) ChatUIState.Empty else ChatUIState.Content(messages)
            }
        } else {
            _uiState.update { ChatUIState.Error(currentMessages, ChatErrorType.LoadMessages) }
        }
    }

    private suspend fun fetchNewChatMessage(sessionId: Long) {
        val taskId = currentChatTaskId ?: return
        val newMessage = remoteRepository.checkGeneration(taskId, sessionId.toString()) ?: return

        val alreadyExists = currentMessages.any {
            it.id == newMessage.id ||
                (it.timestamp == newMessage.timestamp && it.content == newMessage.content)
        }

        if (!alreadyExists && !newMessage.isHuman()) {
            currentMessages = currentMessages + newMessage
            stopPolling()
            _uiState.update { ChatUIState.Content(currentMessages) }
        }
    }

    fun sendMessage(content: String, sessionId: Long) {
        _uiState.update { ChatUIState.Sending(currentMessages) }
        viewModelScope.launch(Dispatchers.IO) {
            sendMessageInternal(content, sessionId)
        }
    }

    private suspend fun sendMessageInternal(content: String, sessionId: Long) {
        val request = ChatMessageRequest(
            sessionId = sessionId.toString(),
            role = "human",
            content = content,
            timestamp = System.currentTimeMillis() / MILLIS_PER_SECOND,
            firstHumanMessage = currentMessages.isEmpty()
        )

        val sentMessage = remoteRepository.sendChatMessage(request)
        if (sentMessage != null) {
            currentMessages = currentMessages + sentMessage
            stopPolling()
            generateSession(sessionId)
            startPolling(sessionId)
        } else {
            _uiState.update { ChatUIState.Error(currentMessages, ChatErrorType.SendMessage) }
        }
    }

    private suspend fun generateSession(sessionId: Long) {
        remoteRepository.generateSession(sessionId.toString())?.let {
            currentChatTaskId = it.taskId.toString()
        }
    }

    fun retryResponseGeneration() {
        val sessionId = _sessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            generateSession(sessionId)
            startPolling(sessionId)
        }
    }

    fun startNewConversation(content: String) {
        if (_sessionId.value != null) {
            sendMessage(content, _sessionId.value!!)
            return
        }

        _uiState.update { ChatUIState.Sending(currentMessages) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = remoteRepository.createConversation(content) ?: run {
                _uiState.update { ChatUIState.Error(currentMessages, ChatErrorType.SendMessage) }
                return@launch
            }

            val newSessionId = result.session.id
            _sessionId.update { newSessionId }
            currentChatTaskId = null
            sendMessageInternal(content, newSessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
