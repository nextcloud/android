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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.component.AddTaskAlertDialog
import com.nextcloud.client.assistant.component.CenterText
import com.nextcloud.client.assistant.taskTypes.TaskTypesRow
import com.nextcloud.client.assistant.task.TaskView
import com.nextcloud.client.assistant.repository.AssistantMockRepository
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, activity: Activity) {
    val state by viewModel.state.collectAsState()
    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val filteredTaskList by viewModel.filteredTaskList.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    var showAddTaskAlertDialog by remember { mutableStateOf(false) }
    var showDeleteTaskAlertDialog by remember { mutableStateOf(false) }
    var taskIdToDeleted: Long? by remember {
        mutableStateOf(null)
    }
    val pullRefreshState = rememberPullToRefreshState()

    @Suppress("MagicNumber")
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            delay(1500)
            viewModel.fetchTaskList(onCompleted = {
                pullRefreshState.endRefresh()
            })
        }
    }

    Box(Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
        if (state == AssistantViewModel.State.Loading || pullRefreshState.isRefreshing) {
            CenterText(text = stringResource(id = R.string.assistant_screen_loading))
        } else {
            if (filteredTaskList.isNullOrEmpty()) {
                EmptyTaskList(selectedTaskType, taskTypes, viewModel)
            } else {
                AssistantContent(
                    filteredTaskList!!,
                    taskTypes,
                    selectedTaskType,
                    viewModel,
                    showDeleteTaskAlertDialog = { taskId ->
                        taskIdToDeleted = taskId
                        showDeleteTaskAlertDialog = true
                    }
                )
            }
        }

        if (pullRefreshState.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(progress = { pullRefreshState.progress }, modifier = Modifier.fillMaxWidth())
        }

        if (selectedTaskType?.name != stringResource(id = R.string.assistant_screen_all_task_type)) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    showAddTaskAlertDialog = true
                }
            ) {
                Icon(Icons.Filled.Add, "Add Task Icon")
            }
        }
    }

    ScreenState(state, activity, viewModel)

    if (showDeleteTaskAlertDialog) {
        taskIdToDeleted?.let { id ->
            SimpleAlertDialog(
                title = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_title),
                description = stringResource(id = R.string.assistant_screen_delete_task_alert_dialog_description),
                dismiss = { showDeleteTaskAlertDialog = false },
                onComplete = { viewModel.deleteTask(id) }
            )
        }
    }

    if (showAddTaskAlertDialog) {
        selectedTaskType?.let { taskType ->
            AddTaskAlertDialog(
                title = taskType.name,
                description = taskType.description,
                addTask = { input ->
                    taskType.id?.let {
                        viewModel.createTask(input = input, type = it)
                    }
                },
                dismiss = {
                    showAddTaskAlertDialog = false
                }
            )
        }
    }
}

@Composable
private fun ScreenState(state: AssistantViewModel.State, activity: Activity, viewModel: AssistantViewModel) {
    val messageId: Int? = when (state) {
        is AssistantViewModel.State.Error -> {
            state.messageId
        }

        is AssistantViewModel.State.TaskCreated -> {
            state.messageId
        }

        is AssistantViewModel.State.TaskDeleted -> {
            state.messageId
        }

        else -> {
            null
        }
    }

    messageId?.let {
        DisplayUtils.showSnackMessage(
            activity,
            stringResource(id = messageId)
        )

        viewModel.resetState()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantContent(
    taskList: List<Task>,
    taskTypes: List<TaskType>?,
    selectedTaskType: TaskType?,
    viewModel: AssistantViewModel,
    showDeleteTaskAlertDialog: (Long) -> Unit
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
            TaskView(task, showDeleteTaskAlertDialog = { showDeleteTaskAlertDialog(task.id) })
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
