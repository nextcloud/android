/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.taskTypes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.owncloud.android.lib.resources.assistant.model.TaskType

@Composable
fun TaskTypesRow(selectedTaskType: TaskType?, data: List<TaskType>?, selectTaskType: (TaskType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        data?.forEach { taskType ->
            taskType.name?.let { taskTypeName ->
                FilledTonalButton(
                    onClick = { selectTaskType(taskType) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTaskType?.id == taskType.id) {
                            Color.Unspecified
                        } else {
                            Color.Gray
                        }
                    )
                ) {
                    Text(text = taskTypeName)
                }

                Spacer(modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}
