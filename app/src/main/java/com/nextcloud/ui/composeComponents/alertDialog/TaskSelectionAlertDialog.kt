/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.composeComponents.alertDialog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSelectionAlertDialog(
    taskTypes: List<TaskTypeData>,
    onDismiss: () -> Unit,
    onConfirm: (TaskTypeData) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            LazyColumn(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                items(taskTypes) { task ->
                    TextButton(
                        onClick = {
                            onConfirm(task)
                            onDismiss()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
