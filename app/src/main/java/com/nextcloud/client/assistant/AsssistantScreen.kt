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
import androidx.compose.material3.pulltorefresh.PullToRefreshState
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
import com.nextcloud.client.assistant.model.ScreenOverlayState
import com.nextcloud.client.assistant.model.ScreenState
import com.nextcloud.client.assistant.repository.AssistantMockRepository
import com.nextcloud.client.assistant.task.TaskView
import com.nextcloud.client.assistant.taskTypes.TaskTypesRow
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.nextcloud.utils.extensions.showShareIntent
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, activity: Activity) {
    val messageId by viewModel.snackbarMessageId.collectAsState()
    val screenOverlayState by viewModel.screenOverlayState.collectAsState()

    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val filteredTaskList by viewModel.filteredTaskList.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    @Suppress("MagicNumber")
    Box(
        modifier = Modifier.pullToRefresh(
            screenState == ScreenState.Refreshing,
            pullRefreshState,
            onRefresh = {
            scope.launch {
                delay(1500)
                viewModel.fetchTaskList()
            }
        })
    ) {
        ShowScreenState(screenState, selectedTaskType, taskTypes, viewModel, filteredTaskList)

        ShowLinearProgressIndicator(screenState, pullRefreshState)

        if (selectedTaskType?.name != stringResource(id = R.string.assistant_screen_all_task_type)) {
            AddFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                selectedTaskType,
                viewModel
            )
        }
    }

    showSnackBarMessage(messageId, activity, viewModel)
    ShowOverlayState(screenOverlayState, activity, viewModel)
}

@Composable
private fun ShowScreenState(
    screenState: ScreenState?,
    selectedTaskType: TaskType?,
    taskTypes: List<TaskType>?,
    viewModel: AssistantViewModel,
    filteredTaskList: List<Task>?
) {
    when(screenState) {
        ScreenState.Refreshing -> {
            CenterText(text = stringResource(id = R.string.assistant_screen_loading))
        }

        ScreenState.EmptyContent -> {
            EmptyTaskList(selectedTaskType, taskTypes, viewModel)
        }

        ScreenState.Content -> {
            AssistantContent(
                filteredTaskList!!,
                taskTypes,
                selectedTaskType,
                viewModel
            )
        }

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowLinearProgressIndicator(screenState: ScreenState?, pullToRefreshState: PullToRefreshState) {
    if (screenState == ScreenState.Refreshing) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(
            progress = { pullToRefreshState.distanceFraction },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddFloatingActionButton(modifier: Modifier, selectedTaskType: TaskType?, viewModel: AssistantViewModel) {
    FloatingActionButton(
        modifier = modifier,
        onClick = {
            selectedTaskType?.let {
                val newState = ScreenOverlayState.AddTask(it, "")
                viewModel.updateScreenState(newState)
            }
        }
    ) {
        Icon(Icons.Filled.Add, "Add Task Icon")
    }
}

private fun showSnackBarMessage(messageId: Int?, activity: Activity, viewModel: AssistantViewModel) {
    messageId?.let {
        DisplayUtils.showSnackMessage(
            activity,
            activity.getString(it)
        )

        viewModel.updateSnackbarMessage(null)
    }
}

@Composable
private fun ShowOverlayState(
    state: ScreenOverlayState?,
    activity: Activity,
    viewModel: AssistantViewModel
) {
    when(state) {
        is ScreenOverlayState.AddTask -> {
            AddTaskAlertDialog(
                title =  state.taskType.name,
                description =  state.taskType.description,
                defaultInput = state.input,
                addTask = { input ->
                    state.taskType.id?.let { taskTypeId ->
                        viewModel.createTask(input = input, type = taskTypeId)
                    }
                },
                dismiss = {
                    viewModel.updateScreenState(null)
                }
            )
        }

        is ScreenOverlayState.DeleteTask -> {
            SimpleAlertDialog(
                title = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_title),
                description = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_description),
                dismiss = { viewModel.updateScreenState(null) },
                onComplete = { viewModel.deleteTask(state.id) }
            )
        }

        is ScreenOverlayState.TaskActions -> {
            val bottomSheetAction = listOf(
                Triple(
                    R.drawable.ic_share,
                    R.string.common_share
                ) {
                    activity.showShareIntent(state.task.output)
                },
                Triple(
                    R.drawable.ic_content_copy,
                    R.string.common_copy
                ) {
                    ClipboardUtil.copyToClipboard(activity, state.task.output)
                },
                Triple(
                    R.drawable.ic_edit,
                    R.string.action_edit
                ) {
                    val taskType = TaskType(
                        state.task.type,
                        activity.getString(R.string.assistant_screen_add_task_alert_dialog_title),
                        null
                    )
                    val newState =
                        ScreenOverlayState.AddTask(taskType, state.task.input ?: "")
                    viewModel.updateScreenState(newState)
                }
            )

            MoreActionsBottomSheet(
                title = state.task.input,
                actions = bottomSheetAction,
                dismiss = { viewModel.updateScreenState(null) }
            )
        }

        else -> Unit
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
                showDeleteTaskAlertDialog = {
                    val newState = ScreenOverlayState.DeleteTask(task.id)
                    viewModel.updateScreenState(newState)
                },
                showTaskActions = {
                    val newState = ScreenOverlayState.TaskActions(task)
                    viewModel.updateScreenState(newState)
                }
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
