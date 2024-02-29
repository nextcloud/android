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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.client.assistant.component.AddTaskAlertDialog
import com.nextcloud.client.assistant.component.CenterText
import com.nextcloud.client.assistant.component.TaskTypesRow
import com.nextcloud.client.assistant.component.TaskView
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, floatingActionButton: FloatingActionButton) {
    // TODO hide sort group, search bar
    // TODO top bar, back button causes crash
    // TODO generate list according to selection (selectedTask).
    val activity = LocalContext.current as Activity
    val loading by viewModel.loading.collectAsState()
    val selectedTaskType by viewModel.selectedTaskType.collectAsState()
    val taskList by viewModel.taskList.collectAsState()
    val isTaskCreated by viewModel.isTaskCreated.collectAsState()
    val isTaskDeleted by viewModel.isTaskDeleted.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    var showAddTaskAlertDialog by remember {
        mutableStateOf(false)
    }

    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            delay(1500)
            viewModel.getTaskList(onCompleted = {
                pullRefreshState.endRefresh()
            })
        }
    }

    floatingActionButton.setOnClickListener {
        showAddTaskAlertDialog = true
    }

    Box(Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
        if (loading || pullRefreshState.isRefreshing) {
            CenterText(text = stringResource(id = R.string.assistant_screen_loading))
        } else {
            val tasks = taskList?.resultData?.tasks ?: return
            val types = taskTypes?.resultData?.types ?: return
            AssistantContent(tasks, types, selectedTaskType, viewModel)
        }

        if (pullRefreshState.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(progress = { pullRefreshState.progress }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (isTaskCreated) {
        DisplayUtils.showSnackMessage(
            activity,
            stringResource(id = R.string.assistant_screen_task_create_success_message)
        )
    }

    if (isTaskDeleted) {
        DisplayUtils.showSnackMessage(
            activity,
            stringResource(id = R.string.assistant_screen_task_delete_success_message)
        )
    }

    if (showAddTaskAlertDialog) {
        selectedTaskType?.let {
            AddTaskAlertDialog(viewModel, it) {
                showAddTaskAlertDialog = false
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantContent(
    taskList: List<Task>,
    taskTypes: List<TaskType>,
    selectedTask: TaskType?,
    viewModel: AssistantViewModel,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        stickyHeader {
            TaskTypesRow(selectedTask, data = taskTypes) { task ->
                viewModel.selectTask(task)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(taskList) {
            if (taskList.isEmpty()) {
                CenterText(text = stringResource(id = R.string.assistant_screen_no_task_available_text))
            } else {
                TaskView(task = it, deleteTask = { viewModel.deleteTask(it.id) } )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
