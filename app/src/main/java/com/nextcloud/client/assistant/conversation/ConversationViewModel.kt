/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.conversation

import androidx.lifecycle.ViewModel
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepository
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.conversation.model.ConversationScreenState
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.chat.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val remoteRepository: ConversationRemoteRepository
) : ViewModel() {
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
                _errorMessageId.update {
                    R.string.assistant_screen_conversation_list_fetch_error
                }
            }
        }
    }

    fun createConversation(title: String?, timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val newConversation = remoteRepository.createConversation(title, timestamp)
            if (newConversation != null) {
                _conversations.update {
                    it + newConversation
                }
            } else {
                _errorMessageId.update {
                    R.string.assistant_screen_conversation_create_error
                }
            }
        }
    }

    fun deleteConversation(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = remoteRepository.deleteConversation(sessionId)
            if (success) {
                // TODO: CHECK IF ITS WORKING...
                val updatedList = _conversations.value.filterNot { it.id == sessionId.toLong() }
                _conversations.update {
                    updatedList
                }
            } else {
                _errorMessageId.update {
                    R.string.assistant_screen_conversation_delete_error
                }
            }
        }
    }
}
