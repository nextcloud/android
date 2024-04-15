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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.extensions.statusData
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.nextcloud.utils.extensions.getRandomString
import com.nextcloud.utils.extensions.splitIntoChunks
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
fun TaskView(
    task: Task,
    showDeleteTaskAlertDialog: (Long) -> Unit
) {
    var loadedChunkSize by remember { mutableIntStateOf(1) }
    val taskOutputChunks = task.output?.splitIntoChunks(100)
    var showMoreActionsBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .combinedClickable(onClick = {
                if (taskOutputChunks?.size != loadedChunkSize) {
                    loadedChunkSize += 1
                } else {
                    loadedChunkSize = 1
                }
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

        taskOutputChunks?.take(loadedChunkSize).let {
            it?.joinToString("")?.let { output ->
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))

                Text(
                    text = output,
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (iconId, descriptionId) = task.statusData()

            Image(
                painter = painterResource(id = iconId),
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(Color.White),
                contentDescription = "status icon"
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(text = stringResource(id = descriptionId), color = Color.White)

            Spacer(modifier = Modifier.weight(1f))

            if ((task.output?.length ?: 0) >= 100) {
                Image(
                    painter = painterResource(
                        id = if (loadedChunkSize != taskOutputChunks?.size) R.drawable.ic_expand_more else R.drawable.ic_expand_less
                    ),
                    contentDescription = "expand content icon",
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
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
