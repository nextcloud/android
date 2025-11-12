/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.conversation.model.ConversationScreenState
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepository
import com.nextcloud.utils.TimeConstants.MILLIS_PER_SECOND
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.chat.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationViewModel(private val remoteRepository: ConversationRemoteRepository) : ViewModel() {
    private val _errorMessageId = MutableStateFlow<Int?>(null)
    val errorMessageId: StateFlow<Int?> = _errorMessageId

    private val _screenState = MutableStateFlow<ConversationScreenState?>(null)
    val screenState: StateFlow<ConversationScreenState?> = _screenState

    private val _conversations = MutableStateFlow<List<Conversation>>(listOf())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    fun fetchConversations() {
        _screenState.update {
            ConversationScreenState.Loading
        }

        viewModelScope.launch(Dispatchers.IO) {
            val conversations = remoteRepository.fetchConversationList()
            if (conversations != null) {
                if (conversations.isEmpty()) {
                    _screenState.update {
                        ConversationScreenState.emptyConversationList()
                    }
                } else {
                    _screenState.update {
                        null
                    }
                    _conversations.update {
                        conversations
                    }
                }
            } else {
                _screenState.update {
                    null
                }
                _errorMessageId.update {
                    R.string.conversation_screen_fetch_error_title
                }
            }
        }
    }

    fun createConversation(title: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis().div(MILLIS_PER_SECOND)
            val newConversation = remoteRepository.createConversation(title, timestamp)
            if (newConversation != null) {
                _conversations.update {
                    listOf(newConversation.session) + it
                }
            } else {
                _errorMessageId.update {
                    R.string.conversation_screen_create_error_title
                }
            }
        }
    }

    fun deleteConversation(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = remoteRepository.deleteConversation(sessionId)
            if (success) {
                val updatedList = _conversations.value.filterNot { it.id == sessionId.toLong() }
                _conversations.update {
                    updatedList
                }
            } else {
                _errorMessageId.update {
                    R.string.conversation_screen_delete_error_title
                }
            }
        }
    }
}
