/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.component.AddTaskAlertDialog
import com.nextcloud.client.assistant.component.CenterText
import com.nextcloud.client.assistant.component.TaskTypesRow
import com.nextcloud.client.assistant.component.TaskView
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.delay

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel) {
    val activity = LocalContext.current as Activity
    val loading by viewModel.loading.collectAsState()
    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val filteredTaskList by viewModel.filteredTaskList.collectAsState()
    val isTaskCreated by viewModel.isTaskCreated.collectAsState()
    val isTaskDeleted by viewModel.isTaskDeleted.collectAsState()
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
            viewModel.getTaskList(onCompleted = {
                pullRefreshState.endRefresh()
            })
        }
    }

    Box(Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
        if (loading || pullRefreshState.isRefreshing) {
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

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                if (selectedTaskType?.id != null) {
                    showAddTaskAlertDialog = true
                } else {
                    DisplayUtils.showSnackMessage(
                        activity,
                        activity.getString(R.string.assistant_screen_select_different_task_type_to_add)
                    )
                }
            }
        ) {
            Icon(Icons.Filled.Add, "Add Task Icon")
        }
    }

    CheckTaskAdd(isTaskCreated, activity, viewModel)
    CheckTaskDeletion(isTaskDeleted, activity, viewModel)

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
        selectedTaskType?.let {
            AddTaskAlertDialog(viewModel, it) {
                showAddTaskAlertDialog = false
            }
        }
    }
}

@Composable
private fun CheckTaskAdd(isTaskCreated: Boolean?, activity: Activity, viewModel: AssistantViewModel) {
    isTaskCreated?.let {
        val messageId = if (it) {
            R.string.assistant_screen_task_create_success_message
        } else {
            R.string.assistant_screen_task_create_fail_message
        }

        DisplayUtils.showSnackMessage(
            activity,
            stringResource(id = messageId)
        )

        viewModel.resetTaskAddState()
    }
}

@Composable
private fun CheckTaskDeletion(isTaskDeleted: Boolean?, activity: Activity, viewModel: AssistantViewModel) {
    isTaskDeleted?.let {
        val messageId = if (it) {
            R.string.assistant_screen_task_delete_success_message
        } else {
            R.string.assistant_screen_task_delete_fail_message
        }

        DisplayUtils.showSnackMessage(
            activity,
            stringResource(id = messageId)
        )

        viewModel.resetTaskDeletionState()
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TaskTypesRow(selectedTaskType, data = taskTypes) { task ->
            viewModel.selectTaskType(task)
        }

        Spacer(modifier = Modifier.height(8.dp))

        CenterText(
            text = stringResource(
                id = R.string.assistant_screen_no_task_available_text,
                selectedTaskType?.name ?: ""
            )
        )
    }
}
