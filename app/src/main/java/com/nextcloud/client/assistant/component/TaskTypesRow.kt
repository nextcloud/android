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
