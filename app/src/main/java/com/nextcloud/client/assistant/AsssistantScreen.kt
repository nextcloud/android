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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.operations.assistant.model.OcsType
import com.nextcloud.ui.composeComponents.SimpleAlertDialog
import com.owncloud.android.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, floatingActionButton: FloatingActionButton) {
    // TODO hide sort group, floating action and search bar
    val taskTypes by viewModel.taskTypes.collectAsState()
    var selectedTaskType: String? by remember {
        mutableStateOf(null)
    }
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
            taskTypes?.let { it ->
                TaskTypesRow(selectedTaskType, data = it.ocs.data.types) { taskId ->
                    selectedTaskType = taskId
                }
            }
        }

        items(taskTypes?.ocs?.data?.types ?: listOf()) {
            Text(text = it.toString())
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
private fun AddTaskAlertDialog(viewModel: AssistantViewModel, type: String, dismiss: () -> Unit) {
    var input by remember {
        mutableStateOf("")
    }

    // TODO add to UI LIB
    SimpleAlertDialog(
        backgroundColor = Color.White,
        textColor = Color.Black,
        titleId = R.string.about_title,
        description = stringResource(id = R.string.about_title),
        dismiss = { dismiss() },
        onComplete = { viewModel.createTask(input = input, type = type) },
        content = {
            TextField(
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.samples),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                value = input,
                onValueChange = {
                    input = it
                },
                singleLine = true
            )
        }
    )
}

@Composable
private fun TaskTypesRow(selectedTaskType: String?, data: List<OcsType>, selectTaskType: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        data.forEach {
            FilledTonalButton(
                onClick = { selectTaskType(it.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTaskType == it.id) {
                        Color.Unspecified
                    } else {
                        Color.Gray
                    }
                )
            ) {
                Text(text = it.name)
            }

            Spacer(modifier = Modifier.padding(end = 8.dp))
        }
    }
}
