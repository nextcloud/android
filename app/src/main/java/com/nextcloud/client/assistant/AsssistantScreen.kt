/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.component.AddTaskAlertDialog
import com.nextcloud.client.assistant.component.CenterText
import com.nextcloud.client.assistant.repository.AssistantMockRepository
import com.nextcloud.client.assistant.task.TaskView
import com.nextcloud.client.assistant.taskTypes.TaskTypesRow
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, activity: Activity) {
    val messageState by viewModel.messageState.collectAsState()
    val alertDialogState by viewModel.screenState.collectAsState()

    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val filteredTaskList by viewModel.filteredTaskList.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    @Suppress("MagicNumber")
    Box(
        modifier = Modifier.pullToRefresh(isRefreshing, pullRefreshState, onRefresh = {
            scope.launch {
                delay(1500)
                viewModel.fetchTaskList()
            }
        })
    ) {
        if (messageState == AssistantViewModel.MessageState.Loading || isRefreshing) {
            CenterText(text = stringResource(id = R.string.assistant_screen_loading))
        } else {
            if (filteredTaskList.isNullOrEmpty()) {
                EmptyTaskList(selectedTaskType, taskTypes, viewModel)
            } else {
                AssistantContent(
                    filteredTaskList!!,
                    taskTypes,
                    selectedTaskType,
                    viewModel
                )
            }
        }

        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { pullRefreshState.distanceFraction },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (selectedTaskType?.name != stringResource(id = R.string.assistant_screen_all_task_type)) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    selectedTaskType?.let {
                        viewModel.showAddTaskAlertDialog(it)
                    }
                }
            ) {
                Icon(Icons.Filled.Add, "Add Task Icon")
            }
        }
    }

    HandleMessageState(messageState, activity, viewModel)
    ScreenState(alertDialogState, viewModel)
}

@Composable
private fun ScreenState(state: AssistantViewModel.ScreenState?, viewModel: AssistantViewModel) {
    when(state) {
        is AssistantViewModel.ScreenState.AddTask -> {
            AddTaskAlertDialog(
                title =  state.taskType.name,
                description =  state.taskType.description,
                addTask = { input ->
                    state.taskType.id?.let {
                        viewModel.createTask(input = input, type = it)
                    }
                },
                dismiss = {
                    viewModel.resetAlertDialogState()
                }
            )
        }

        is AssistantViewModel.ScreenState.DeleteTask -> {
            SimpleAlertDialog(
                title = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_title),
                description = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_description),
                dismiss = { viewModel.resetAlertDialogState() },
                onComplete = { viewModel.deleteTask(state.id) }
            )
        }

        is AssistantViewModel.ScreenState.TaskActions -> {
            val bottomSheetAction = listOf(
                Triple(
                    R.drawable.ic_delete,
                    R.string.assistant_screen_task_more_actions_bottom_sheet_delete_action
                ) {

                }
            )

            MoreActionsBottomSheet(
                title = state.task.input,
                actions = bottomSheetAction,
                dismiss = { viewModel.resetAlertDialogState() }
            )
        }

        else -> Unit
    }
}

@Composable
private fun HandleMessageState(state: AssistantViewModel.MessageState?, activity: Activity, viewModel: AssistantViewModel) {
    val messageStateId: Int? = when (state) {
        is AssistantViewModel.MessageState.Error -> {
            state.messageId
        }

        is AssistantViewModel.MessageState.TaskCreated -> {
            state.messageId
        }

        is AssistantViewModel.MessageState.TaskDeleted -> {
            state.messageId
        }

        else -> {
            null
        }
    }

    messageStateId?.let {
        DisplayUtils.showSnackMessage(
            activity,
            stringResource(id = messageStateId)
        )

        viewModel.resetMessageState()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantContent(
    taskList: List<Task>,
    taskTypes: List<TaskType>?,
    selectedTaskType: TaskType?,
    viewModel: AssistantViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        stickyHeader {
            TaskTypesRow(selectedTaskType, data = taskTypes) { task ->
                viewModel.selectTaskType(task)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(taskList) { task ->
            TaskView(task,
                showDeleteTaskAlertDialog = { viewModel.showDeleteTaskAlertDialog(task.id) },
                showTaskActions = { viewModel.showTaskActionsBottomSheet(task) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyTaskList(selectedTaskType: TaskType?, taskTypes: List<TaskType>?, viewModel: AssistantViewModel) {
    val text = if (selectedTaskType?.name == stringResource(id = R.string.assistant_screen_all_task_type)) {
        stringResource(id = R.string.assistant_screen_no_task_available_for_all_task_filter_text)
    } else {
        stringResource(
            id = R.string.assistant_screen_no_task_available_text,
            selectedTaskType?.name ?: ""
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TaskTypesRow(selectedTaskType, data = taskTypes) { task ->
            viewModel.selectTaskType(task)
        }

        Spacer(modifier = Modifier.height(8.dp))

        CenterText(text = text)
    }
}

@Composable
@Preview
private fun AssistantScreenPreview() {
    val mockRepository = AssistantMockRepository()
    MaterialTheme(
        content = {
            AssistantScreen(
                viewModel = AssistantViewModel(
                    repository = mockRepository,
                    context = WeakReference(LocalContext.current)
                ),
                activity = ComposeActivity()
            )
        }
    )
}

@Composable
@Preview
private fun AssistantEmptyScreenPreview() {
    val mockRepository = AssistantMockRepository(giveEmptyTasks = true)
    MaterialTheme(
        content = {
            AssistantScreen(
                viewModel = AssistantViewModel(
                    repository = mockRepository,
                    context = WeakReference(LocalContext.current)
                ),
                activity = ComposeActivity()
            )
        }
    )
}
