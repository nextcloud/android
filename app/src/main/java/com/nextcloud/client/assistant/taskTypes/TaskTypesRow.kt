/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.taskTypes

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData

@SuppressLint("ResourceType")
@Composable
fun TaskTypesRow(
    selectedTaskType: TaskTypeData?,
    data: List<TaskTypeData>,
    selectTaskType: (TaskTypeData) -> Unit,
    navigateToConversationList: () -> Unit
) {
    if (data.isEmpty()) {
        return
    }

    val selectedTabIndex = data.indexOfFirst { it.id == selectedTaskType?.id }.takeIf { it >= 0 } ?: 0

    Row(
        modifier = Modifier.background(color = colorResource(R.color.actionbar_color)),
        horizontalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.width(11.dp))

        IconButton(
            onClick = { navigateToConversationList() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_open),
                contentDescription = "open conversation list button",
                tint = colorResource(R.color.text_color)
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            containerColor = colorResource(R.color.actionbar_color),
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = colorResource(R.color.primary)
                )
            }
        ) {
            data.forEach { taskType ->
                Tab(
                    selected = selectedTaskType?.id == taskType.id,
                    onClick = { selectTaskType(taskType) },
                    selectedContentColor = colorResource(R.color.text_color),
                    unselectedContentColor = colorResource(R.color.disabled_text),
                    text = { Text(text = taskType.name) }
                )
            }
        }
    }
}

@Composable
@Preview
private fun TaskTypesRowPreview() {
    val selectedTaskType = TaskTypeData("1", "Free text to text prompt", "", emptyMap(), emptyMap())
    val taskTypes = listOf(
        TaskTypeData("1", "Free text to text prompt", "", emptyMap(), emptyMap()),
        TaskTypeData("2", "Extract topics", "", emptyMap(), emptyMap()),
        TaskTypeData("3", "Generate Headline", "", emptyMap(), emptyMap()),
        TaskTypeData("4", "Summarize", "", emptyMap(), emptyMap())
    )

    TaskTypesRow(selectedTaskType, taskTypes, {
    }, {
    })
}
