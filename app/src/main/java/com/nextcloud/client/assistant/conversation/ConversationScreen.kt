/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

@file:Suppress("TopLevelPropertyNaming", "MagicNumber")

package com.nextcloud.client.assistant.conversation

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.conversation.model.ConversationScreenState
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.chat.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ConversationScreen(viewModel: ConversationViewModel, close: () -> Unit, openChat: (Long) -> Unit) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsState()
    val errorMessageId by viewModel.errorMessageId.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.fetchConversations()
    }

    LaunchedEffect(errorMessageId) {
        errorMessageId?.let {
            snackbarHostState.showSnackbar(context.getString(it))
        }
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        close()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "close conversations list"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.createConversation(null)
            }) {
                Icon(Icons.Filled.Add, "Floating action button.")
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay
    ) { innerPadding ->
        when (screenState) {
            is ConversationScreenState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ConversationScreenState.EmptyContent -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.conversation_screen_empty_content_title))
                }
            }

            else -> {
                ConversationList(
                    viewModel = viewModel,
                    conversations = conversations,
                    modifier = Modifier.padding(innerPadding),
                    openChat = openChat
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    viewModel: ConversationViewModel,
    conversations: List<Conversation>,
    modifier: Modifier = Modifier,
    openChat: (Long) -> Unit
) {
    var selectedConversationId by remember { mutableLongStateOf(-1L) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        items(conversations) { conversation ->
            ConversationListItem(
                conversation = conversation,
                onClick = {
                    openChat(conversation.id)
                },
                onLongPressed = {
                    selectedConversationId = conversation.id
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (selectedConversationId != -1L) {
        val currentId = selectedConversationId

        val bottomSheetAction = listOf(
            Triple(
                R.drawable.ic_delete,
                R.string.conversation_screen_delete_button_title
            ) {
                val sessionId: String = currentId.toString()
                viewModel.deleteConversation(sessionId)
                selectedConversationId = -1L
            }
        )

        MoreActionsBottomSheet(
            actions = bottomSheetAction,
            dismiss = { selectedConversationId = -1L }
        )
    }
}

@Composable
private fun ConversationListItem(conversation: Conversation, onClick: () -> Unit, onLongPressed: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPressed
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conversation.titleRepresentation(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorResource(R.color.text_color),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview
@Composable
private fun ConversationListPreview() {
    Column {
        ConversationListItem(Conversation(1L, "User1", "Who is Al Pacino?", 1762847286L, "", null), {
        }, {
        })

        Spacer(modifier = Modifier.height(8.dp))

        ConversationListItem(Conversation(2L, "User1", "What is JetpackCompose?", 1761847286L, "", null), {
        }, {
        })

        Spacer(modifier = Modifier.height(8.dp))
    }
}
