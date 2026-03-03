/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.composeComponents.alertDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

@Composable
fun TaskSelectionAlertDialog(taskTypes: List<TaskTypeData>, onDismiss: () -> Unit, onConfirm: (TaskTypeData) -> Unit) {
    var tempSelectedTask by remember {
        mutableStateOf(taskTypes.firstOrNull())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.assistant_screen_select_task_type_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(taskTypes) { task ->
                    TaskTypeItem(
                        task = task,
                        isSelected = task.id == tempSelectedTask?.id,
                        onClick = { tempSelectedTask = task }
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    tempSelectedTask?.let { onConfirm(it) }
                }
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun TaskTypeItem(task: TaskTypeData, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = task.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
