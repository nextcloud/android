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

package com.nextcloud.client.assistant.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.TaskType

@Composable
fun AddTaskAlertDialog(viewModel: AssistantViewModel, taskType: TaskType, dismiss: () -> Unit) {
    var input by remember {
        mutableStateOf("")
    }

    SimpleAlertDialog(
        backgroundColor = Color.White,
        textColor = Color.Black,
        title = taskType.name ?: "",
        description = taskType.description,
        dismiss = { dismiss() },
        onComplete = {
            taskType.id?.let {
                viewModel.createTask(input = input, type = it)
            }
        },
        content = {
            TextField(
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.assistant_screen_create_task_alert_dialog_input_field_placeholder),
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
