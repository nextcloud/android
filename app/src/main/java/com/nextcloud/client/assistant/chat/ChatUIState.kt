/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.chat

import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage

sealed class ChatUIState {
    data object Loading : ChatUIState()
    data object Empty : ChatUIState()
    data class Content(val messages: List<ChatMessage>) : ChatUIState()
    data class Sending(val messages: List<ChatMessage>) : ChatUIState()
    data class Thinking(val messages: List<ChatMessage>) : ChatUIState()
    data class RetryAvailable(val messages: List<ChatMessage>) : ChatUIState()
    data class Error(val messages: List<ChatMessage>, val errorType: ChatErrorType) : ChatUIState()
}

fun ChatUIState.messages(): List<ChatMessage> = when (this) {
    is ChatUIState.Content -> messages
    is ChatUIState.Sending -> messages
    is ChatUIState.Thinking -> messages
    is ChatUIState.RetryAvailable -> messages
    is ChatUIState.Error -> messages
    is ChatUIState.Loading, is ChatUIState.Empty -> emptyList()
}
