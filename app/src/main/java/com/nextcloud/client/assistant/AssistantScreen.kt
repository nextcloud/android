/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.chat.ChatContent
import com.nextcloud.client.assistant.conversation.ConversationScreen
import com.nextcloud.client.assistant.conversation.ConversationViewModel
import com.nextcloud.client.assistant.conversation.repository.MockConversationRemoteRepository
import com.nextcloud.client.assistant.extensions.getInputTitle
import com.nextcloud.client.assistant.model.AssistantScreenState
import com.nextcloud.client.assistant.model.ScreenOverlayState
import com.nextcloud.client.assistant.repository.local.MockAssistantLocalRepository
import com.nextcloud.client.assistant.repository.remote.MockAssistantRemoteRepository
import com.nextcloud.client.assistant.task.TaskView
import com.nextcloud.client.assistant.taskTypes.TaskTypesRow
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.status.OCCapability
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CHAT_INPUT_DELAY = 100L
private const val PULL_TO_REFRESH_DELAY = 1500L

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    conversationViewModel: ConversationViewModel,
    capability: OCCapability,
    activity: Activity
) {
    val sessionId by viewModel.sessionId.collectAsState()
    val messageId by viewModel.snackbarMessageId.collectAsState()
    val screenOverlayState by viewModel.screenOverlayState.collectAsState()
    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val filteredTaskList by viewModel.filteredTaskList.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })

    LaunchedEffect(messageId) {
        messageId?.let {
            snackbarHostState.showSnackbar(activity.getString(it))
            viewModel.updateSnackbarMessage(null)
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.startPolling(sessionId)

        sessionId?.let {
            viewModel.fetchChatMessages(it)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    HorizontalPager(
        state = pagerState
    ) { page ->
        when (page) {
            0 -> {
                ConversationScreen(viewModel = conversationViewModel, close = {
                    scope.launch {
                        pagerState.scrollToPage(1)
                    }
                }, openChat = { newSessionId ->
                    viewModel.initSessionId(newSessionId)
                    taskTypes?.find { it.isChat }?.let { chatTaskType ->
                        viewModel.selectTaskType(chatTaskType)
                    }
                    scope.launch {
                        pagerState.scrollToPage(1)
                    }
                })
            }
            1 -> {
                Scaffold(
                    modifier = Modifier.pullToRefresh(
                        false,
                        pullRefreshState,
                        onRefresh = {
                            scope.launch {
                                delay(PULL_TO_REFRESH_DELAY)

                                val newSessionId = sessionId
                                if (newSessionId != null) {
                                    viewModel.fetchChatMessages(newSessionId)
                                } else {
                                    viewModel.fetchTaskList()
                                }
                            }
                        }
                    ),
                    topBar = {
                        taskTypes?.let {
                            TaskTypesRow(selectedTaskType, data = it, selectTaskType = { task ->
                                viewModel.selectTaskType(task)
                            }, navigateToConversationList = {
                                scope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            })
                        }
                    },
                    bottomBar = {
                        if (!taskTypes.isNullOrEmpty()) {
                            ChatInputBar(
                                sessionId,
                                selectedTaskType,
                                viewModel
                            )
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(snackbarHostState)
                    }
                ) { paddingValues ->
                    when (screenState) {
                        is AssistantScreenState.EmptyContent -> {
                            val state = (screenState as AssistantScreenState.EmptyContent)
                            EmptyContent(
                                paddingValues,
                                iconId = state.iconId,
                                descriptionId = state.descriptionId,
                                titleId = state.titleId
                            )
                        }

                        AssistantScreenState.TaskContent -> {
                            TaskContent(
                                paddingValues,
                                filteredTaskList ?: listOf(),
                                viewModel,
                                capability
                            )
                        }

                        AssistantScreenState.ChatContent -> {
                            ChatContent(
                                viewModel = viewModel,
                                modifier = Modifier.padding(paddingValues)
                            )
                        }

                        else -> EmptyContent(
                            paddingValues,
                            iconId = R.drawable.spinner_inner,
                            titleId = null,
                            descriptionId = R.string.common_loading
                        )
                    }

                    LinearProgressIndicator(
                        progress = { pullRefreshState.distanceFraction },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OverlayState(screenOverlayState, activity, viewModel)
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ChatInputBar(sessionId: Long?, selectedTaskType: TaskTypeData?, viewModel: AssistantViewModel) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.assistant_output_generation_warning_text),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = colorResource(R.color.text_color)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text(selectedTaskType?.description ?: "") },
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (text.isBlank()) {
                            return@IconButton
                        }

                        val taskType = selectedTaskType ?: return@IconButton
                        if (taskType.isChat) {
                            if (sessionId != null) {
                                viewModel.sendChatMessage(content = text, sessionId)
                            } else {
                                viewModel.createConversation(text)
                            }
                        } else {
                            viewModel.createTask(input = text, taskType = taskType)
                        }

                        scope.launch {
                            delay(CHAT_INPUT_DELAY)
                            text = ""
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = "Send message",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun OverlayState(state: ScreenOverlayState?, activity: Activity, viewModel: AssistantViewModel) {
    when (state) {
        is ScreenOverlayState.DeleteTask -> {
            SimpleAlertDialog(
                title = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_title),
                description = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_description),
                dismiss = { viewModel.updateScreenOverlayState(null) },
                onComplete = { viewModel.deleteTask(state.id) }
            )
        }

        is ScreenOverlayState.TaskActions -> {
            val actions = state.getActions(activity, onDeleteCompleted = { deleteTask ->
                viewModel.updateScreenOverlayState(deleteTask)
            })

            MoreActionsBottomSheet(
                title = state.task.getInputTitle(),
                actions = actions,
                dismiss = { viewModel.updateScreenOverlayState(null) }
            )
        }

        else -> Unit
    }
}

@Composable
private fun TaskContent(
    paddingValues: PaddingValues,
    taskList: List<Task>,
    viewModel: AssistantViewModel,
    capability: OCCapability
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(taskList, key = { it.id }) { task ->
            TaskView(
                task,
                capability,
                showTaskActions = {
                    val newState = ScreenOverlayState.TaskActions(task)
                    viewModel.updateScreenOverlayState(newState)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyContent(paddingValues: PaddingValues, iconId: Int?, descriptionId: Int?, titleId: Int?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        iconId?.let {
            Image(
                painter = painterResource(id = iconId),
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(color = colorResource(R.color.text_color)),
                contentDescription = "empty content icon"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        titleId?.let {
            Text(
                text = stringResource(titleId),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                textAlign = TextAlign.Center,
                color = colorResource(R.color.text_color)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        descriptionId?.let {
            Text(
                text = stringResource(descriptionId),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                textAlign = TextAlign.Center,
                color = colorResource(R.color.text_color)
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
@Preview
private fun AssistantScreenPreview() {
    MaterialTheme(
        content = {
            AssistantScreen(
                conversationViewModel = getMockConversationViewModel(),
                viewModel = getMockAssistantViewModel(false),
                activity = ComposeActivity(),
                capability = OCCapability().apply {
                    versionMayor = 30
                }
            )
        }
    )
}

@Suppress("MagicNumber")
@Composable
@Preview
private fun AssistantEmptyScreenPreview() {
    MaterialTheme(
        content = {
            AssistantScreen(
                conversationViewModel = getMockConversationViewModel(),
                viewModel = getMockAssistantViewModel(true),
                activity = ComposeActivity(),
                capability = OCCapability().apply {
                    versionMayor = 30
                }
            )
        }
    )
}

private fun getMockConversationViewModel(): ConversationViewModel {
    val mockRemoteRepository = MockConversationRemoteRepository()
    return ConversationViewModel(
        remoteRepository = mockRemoteRepository
    )
}

private fun getMockAssistantViewModel(giveEmptyTasks: Boolean): AssistantViewModel {
    val mockLocalRepository = MockAssistantLocalRepository()
    val mockRemoteRepository = MockAssistantRemoteRepository(giveEmptyTasks)
    return AssistantViewModel(
        accountName = "test:localhost",
        remoteRepository = mockRemoteRepository,
        localRepository = mockLocalRepository,
        sessionIdArg = null
    )
}
