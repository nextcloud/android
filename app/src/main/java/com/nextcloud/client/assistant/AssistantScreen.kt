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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.component.AddTaskAlertDialog
import com.nextcloud.client.assistant.component.CenterText
import com.nextcloud.client.assistant.extensions.getInputTitle
import com.nextcloud.client.assistant.model.ScreenOverlayState
import com.nextcloud.client.assistant.model.ScreenState
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

private const val PULL_TO_REFRESH_DELAY = 1500L

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, capability: OCCapability, activity: Activity) {
    val messageId by viewModel.snackbarMessageId.collectAsState()
    val screenOverlayState by viewModel.screenOverlayState.collectAsState()

    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val filteredTaskList by viewModel.filteredTaskList.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messageId) {
        messageId?.let {
            snackbarHostState.showSnackbar(activity.getString(it))
            viewModel.updateSnackbarMessage(null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startTaskListPolling()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTaskListPolling()
        }
    }

    Scaffold(
        modifier = Modifier.pullToRefresh(
            false,
            pullRefreshState,
            onRefresh = {
                scope.launch {
                    delay(PULL_TO_REFRESH_DELAY)
                    viewModel.fetchTaskList()
                }
            }
        ),
        topBar = {
            taskTypes?.let {
                TaskTypesRow(selectedTaskType, data = it) { task ->
                    viewModel.selectTaskType(task)
                }
            }
        },
        floatingActionButton = {
            if (!taskTypes.isNullOrEmpty()) {
                AddTaskButton(
                    selectedTaskType,
                    viewModel
                )
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        when (screenState) {
            is ScreenState.EmptyContent -> {
                val state = (screenState as ScreenState.EmptyContent)
                EmptyContent(
                    paddingValues,
                    state.iconId,
                    state.descriptionId
                )
            }

            ScreenState.Content -> {
                AssistantContent(
                    paddingValues,
                    filteredTaskList ?: listOf(),
                    viewModel,
                    capability
                )
            }

            else -> EmptyContent(
                paddingValues,
                R.drawable.spinner_inner,
                R.string.assistant_screen_loading
            )
        }

        LinearProgressIndicator(
            progress = { pullRefreshState.distanceFraction },
            modifier = Modifier.fillMaxWidth()
        )

        OverlayState(screenOverlayState, activity, viewModel)
    }
}

@Composable
private fun AddTaskButton(selectedTaskType: TaskTypeData?, viewModel: AssistantViewModel) {
    FloatingActionButton(
        onClick = {
            selectedTaskType?.let {
                val newState = ScreenOverlayState.AddTask(it, "")
                viewModel.updateTaskListScreenState(newState)
            }
        }
    ) {
        Icon(Icons.Filled.Add, "Add Task Icon")
    }
}

@Suppress("LongMethod")
@Composable
private fun OverlayState(state: ScreenOverlayState?, activity: Activity, viewModel: AssistantViewModel) {
    when (state) {
        is ScreenOverlayState.AddTask -> {
            AddTaskAlertDialog(
                title = state.taskType.name,
                description = state.taskType.description,
                defaultInput = state.input,
                addTask = { input ->
                    state.taskType.let { taskType ->
                        viewModel.createTask(input = input, taskType = taskType)
                    }
                },
                dismiss = {
                    viewModel.updateTaskListScreenState(null)
                }
            )
        }

        is ScreenOverlayState.DeleteTask -> {
            SimpleAlertDialog(
                title = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_title),
                description = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_description),
                dismiss = { viewModel.updateTaskListScreenState(null) },
                onComplete = { viewModel.deleteTask(state.id) }
            )
        }

        is ScreenOverlayState.TaskActions -> {
            val actions = state.getActions(activity, onEditCompleted = { addTask ->
                viewModel.updateTaskListScreenState(addTask)
            }, onDeleteCompleted = { deleteTask ->
                viewModel.updateTaskListScreenState(deleteTask)
            })

            MoreActionsBottomSheet(
                title = state.task.getInputTitle(),
                actions = actions,
                dismiss = { viewModel.updateTaskListScreenState(null) }
            )
        }

        else -> Unit
    }
}

@Composable
private fun AssistantContent(
    paddingValues: PaddingValues,
    taskList: List<Task>,
    viewModel: AssistantViewModel,
    capability: OCCapability
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(12.dp)
    ) {
        items(taskList, key = { it.id }) { task ->
            TaskView(
                task,
                capability,
                showTaskActions = {
                    val newState = ScreenOverlayState.TaskActions(task)
                    viewModel.updateTaskListScreenState(newState)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyContent(paddingValues: PaddingValues, iconId: Int?, descriptionId: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        iconId?.let {
            Image(
                painter = painterResource(id = iconId),
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(color = colorResource(R.color.text_color)),
                contentDescription = "status icon"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        CenterText(text = stringResource(id = descriptionId))
    }
}

@Suppress("MagicNumber")
@Composable
@Preview
private fun AssistantScreenPreview() {
    MaterialTheme(
        content = {
            AssistantScreen(
                viewModel = getMockViewModel(false),
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
                viewModel = getMockViewModel(true),
                activity = ComposeActivity(),
                capability = OCCapability().apply {
                    versionMayor = 30
                }
            )
        }
    )
}

private fun getMockViewModel(giveEmptyTasks: Boolean): AssistantViewModel {
    val mockLocalRepository = MockAssistantLocalRepository()
    val mockRemoteRepository = MockAssistantRemoteRepository(giveEmptyTasks)
    return AssistantViewModel(remoteRepository = mockRemoteRepository, localRepository = mockLocalRepository)
}
