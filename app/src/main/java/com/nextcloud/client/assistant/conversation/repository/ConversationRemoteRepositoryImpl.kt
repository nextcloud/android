/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.conversation.repository

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.resources.assistant.chat.CreateConversationRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.DeleteConversationRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.GetConversationListRemoteOperation
import com.owncloud.android.lib.resources.assistant.chat.model.Conversation
import com.owncloud.android.lib.resources.assistant.chat.model.CreateConversation

class ConversationRemoteRepositoryImpl(private val client: NextcloudClient): ConversationRemoteRepository {
    override suspend fun fetchConversationList(): List<Conversation>? {
        val result = GetConversationListRemoteOperation().execute(client)
        return if (result.isSuccess) {
             result.resultData
        } else {
            null
        }
    }

    override suspend fun createConversation(
        title: String?,
        timestamp: Long
    ): CreateConversation? {
        val result = CreateConversationRemoteOperation(title, timestamp).execute(client)
        return if (result.isSuccess) {
            result.resultData
        } else {
            null
        }
    }

    override suspend fun deleteConversation(sessionId: String): Boolean {
        val result = DeleteConversationRemoteOperation(sessionId).execute(client)
        return result.isSuccess
    }
}
