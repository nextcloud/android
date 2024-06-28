/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.task

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.taskDetail.TaskDetailBottomSheet
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.nextcloud.utils.extensions.getRandomString
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
fun TaskView(task: Task, showDeleteTaskAlertDialog: (Long) -> Unit) {
    var showTaskDetailBottomSheet by remember { mutableStateOf(false) }
    var showMoreActionsBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .combinedClickable(onClick = {
                showTaskDetailBottomSheet = true
            }, onLongClick = {
                showMoreActionsBottomSheet = true
            })
            .padding(start = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        task.input?.let {
            Text(
                text = it,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        task.output?.let {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))

            Text(
                text = it.take(100),
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .height(100.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            )
        }

        TaskStatus(task, foregroundColor = Color.White)

        if (showMoreActionsBottomSheet) {
            val bottomSheetAction = listOf(
                Triple(
                    R.drawable.ic_delete,
                    R.string.assistant_screen_task_more_actions_bottom_sheet_delete_action
                ) {
                    showDeleteTaskAlertDialog(task.id)
                }
            )

            MoreActionsBottomSheet(
                title = task.input,
                actions = bottomSheetAction,
                dismiss = { showMoreActionsBottomSheet = false }
            )
        }

        if (showTaskDetailBottomSheet) {
            TaskDetailBottomSheet(task) {
                showTaskDetailBottomSheet = false
            }
        }
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun TaskViewPreview() {
    val output = "Lorem".getRandomString(100)

    TaskView(
        task = Task(
            1,
            "Free Prompt",
            0,
            "1",
            "1",
            "Give me text",
            output,
            "",
            ""
        )
    ) {
    }
}
