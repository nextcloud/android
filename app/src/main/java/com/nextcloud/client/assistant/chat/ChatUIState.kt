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

    fun messages(): List<ChatMessage> = when (this) {
        is Content -> messages
        is Sending -> messages
        is Thinking -> messages
        is RetryAvailable -> messages
        is Error -> messages
        is Loading, is Empty -> emptyList()
    }

    fun canSend(): Boolean = (this == Empty || this is Content)
}
