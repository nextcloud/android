/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

@file:Suppress("TopLevelPropertyNaming", "MagicNumber")

package com.nextcloud.client.assistant.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.conversation.model.ConversationScreenState
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.chat.model.Conversation

private val BUTTON_HEIGHT = 32.dp

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
        bottomBar = {
            CreateConversationButton(onClick = {
                viewModel.createConversation(null)
            })
        }
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
                    conversations = conversations,
                    onDeleteClick = { viewModel.deleteConversation(it.id.toString()) },
                    modifier = Modifier.padding(innerPadding),
                    openChat = openChat
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<Conversation>,
    onDeleteClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier,
    openChat: (Long) -> Unit
) {
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
                onDeleteClick = { onDeleteClick(conversation) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConversationListItem(conversation: Conversation, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(BUTTON_HEIGHT)
            ) {
                Text(
                    text = conversation.titleRepresentation(),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onDeleteClick
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete conversation"
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateConversationButton(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(BUTTON_HEIGHT)
            ) {
                Text(
                    text = stringResource(R.string.conversation_screen_create_button_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add conversation"
                )
            }
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

        CreateConversationButton { }
    }
}
