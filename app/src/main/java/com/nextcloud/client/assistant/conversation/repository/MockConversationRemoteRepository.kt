/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.conversation.repository

import com.owncloud.android.lib.resources.assistant.chat.model.Conversation
import com.owncloud.android.lib.resources.assistant.chat.model.CreateConversation

class MockConversationRemoteRepository: ConversationRemoteRepository {
    override suspend fun fetchConversationList(): List<Conversation>? {
        return null
    }

    override suspend fun createConversation(
        title: String?,
        timestamp: Long
    ): CreateConversation? {
        return null
    }

    override suspend fun deleteConversation(sessionId: String): Boolean {
        return true
    }
}
