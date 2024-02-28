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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.client.assistant.component.AddTaskAlertDialog
import com.nextcloud.client.assistant.component.TaskTypesRow
import com.nextcloud.client.assistant.component.TaskView
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.TaskType
import com.owncloud.android.utils.DisplayUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, floatingActionButton: FloatingActionButton) {
    // TODO hide sort group, floating action and search bar
    val selectedTask by viewModel.selectedTask.collectAsState()
    val taskList by viewModel.taskList.collectAsState()
    val isTaskCreated by viewModel.isTaskCreated.collectAsState()
    val taskTypes by viewModel.taskTypes.collectAsState()
    var showAddTaskAlertDialog by remember {
        mutableStateOf(false)
    }

    floatingActionButton.setOnClickListener {
        showAddTaskAlertDialog = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        stickyHeader {
            taskTypes?.let { taskTypes ->
                taskTypes.resultData?.types.let {
                    TaskTypesRow(selectedTask, data = it) { task->
                        viewModel.selectTask(task)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(taskList?.resultData?.tasks ?: listOf()) {
            TaskView(task = it)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (isTaskCreated) {
        DisplayUtils.showSnackMessage(
            LocalContext.current as Activity,
            stringResource(id = R.string.assistant_screen_task_create_success_message)
        )
    }

    if (showAddTaskAlertDialog) {
        selectedTask?.let {
            AddTaskAlertDialog(viewModel, it) {
                showAddTaskAlertDialog = false
            }
        }
    }
}
