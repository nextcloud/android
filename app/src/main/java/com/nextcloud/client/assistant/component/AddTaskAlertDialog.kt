/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.nextcloud.ui.composeComponents.alertDialog.SimpleAlertDialog
import com.owncloud.android.R

@Composable
fun AddTaskAlertDialog(title: String?, description: String?, addTask: (String) -> Unit, dismiss: () -> Unit) {
    var input by remember {
        mutableStateOf("")
    }

    SimpleAlertDialog(
        title = title ?: "",
        description = description ?: "",
        dismiss = { dismiss() },
        onComplete = {
            addTask(input)
        },
        content = {
            TextField(
                placeholder = {
                    Text(
                        text = stringResource(
                            id = R.string.assistant_screen_create_task_alert_dialog_input_field_placeholder
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                value = input,
                onValueChange = {
                    input = it
                }
            )
        }
    )
}

@Composable
@Preview
private fun AddTaskAlertDialogPreview() {
    AddTaskAlertDialog(title = "Title", description = "Description", addTask = { }, dismiss = {})
}
