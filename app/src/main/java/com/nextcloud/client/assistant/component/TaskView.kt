/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.assistant.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
fun TaskView(
    task: Task,
    showDeleteTaskAlertDialog: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMoreActionsBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .combinedClickable(onClick = {
                expanded = !expanded
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
                text = if (expanded) it else it.take(100) + "...",
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            )
        }

        if ((task.output?.length ?: 0) >= 100) {
            Text(
                text = if (!expanded) {
                    stringResource(id = R.string.assistant_screen_task_view_show_more)
                } else {
                    stringResource(id = R.string.assistant_screen_task_view_show_less)
                },
                textAlign = TextAlign.End,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

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
    }
}

@Preview
@Composable
private fun TaskViewPreview() {
    val output =
        "Lorem Ipsum is simply dummy text of the printing and " +
            "typesetting industry. Lorem Ipsum has been the " +
            "industry's standard dummy text ever since the 1500s, " +
            "when an unknown printer took a galley of type and " +
            "scrambled it to make a type specimen book. " +
            "It has survived not only five centuries, but also " +
            "the leap into electronic typesetting, remaining" +
            " essentially unchanged. It wLorem Ipsum is simply dummy" +
            " text of the printing and typesetting industry. " +
            "Lorem Ipsum has been the industry's standard dummy " +
            "text ever since the 1500s, when an unknown printer took a" +
            " galley of type and scrambled it to make a type specimen book. " +
            "It has survived not only five centuries, but also the leap " +
            "into electronic typesetting, remaining essentially unchanged."

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
