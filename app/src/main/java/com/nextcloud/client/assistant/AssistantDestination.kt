/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.assistant

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.nextcloud.client.assistant.chat.ChatViewModel
import com.nextcloud.client.assistant.conversation.ConversationViewModel
import com.nextcloud.client.assistant.conversation.repository.ConversationRemoteRepositoryImpl
import com.nextcloud.client.assistant.repository.local.AssistantLocalRepositoryImpl
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepositoryImpl
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.compose.ComposeViewModel
import com.nextcloud.ui.compose.navigation.ComposeDestination
import com.owncloud.android.lib.resources.status.OCCapability

@Composable
fun AssistantDestination(
    screen: ComposeDestination.AssistantScreen,
    composeViewModel: ComposeViewModel,
    accountName: String,
    nextcloudClient: NextcloudClient?,
    capability: OCCapability?
) {
    val activity = LocalActivity.current ?: return
    val client = nextcloudClient ?: return
    val ocCapability = capability ?: return
    val remoteRepository = AssistantRemoteRepositoryImpl(client, ocCapability)

    AssistantScreen(
        composeViewModel = composeViewModel,
        viewModel = AssistantViewModel(
            accountName = accountName,
            remoteRepository = remoteRepository,
            localRepository = AssistantLocalRepositoryImpl(NextcloudDatabase.instance().assistantDao()),
            sessionIdArg = screen.sessionId
        ),
        chatViewModel = ChatViewModel(remoteRepository),
        conversationViewModel = ConversationViewModel(
            remoteRepository = ConversationRemoteRepositoryImpl(client)
        ),
        activity = activity,
        capability = ocCapability
    )
}
