/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.assistant.taskDetail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.task.TaskStatusView
import com.nextcloud.utils.extensions.getRandomString
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutput
import com.owncloud.android.lib.resources.status.OCCapability

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(task: Task, capability: OCCapability, showTaskActions: () -> Unit, dismiss: () -> Unit) {
    var showInput by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.padding(top = 32.dp),
        containerColor = colorResource(R.color.bg_default),
        onDismissRequest = { dismiss() },
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = showTaskActions) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More button",
                            tint = colorResource(R.color.text_color)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorResource(id = R.color.light_grey),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    TextInputSelectButton(
                        Modifier.weight(1f),
                        R.string.assistant_task_detail_screen_input_button_title,
                        showInput,
                        onClick = {
                            showInput = true
                        }
                    )

                    TextInputSelectButton(
                        Modifier.weight(1f),
                        R.string.assistant_task_detail_screen_output_button_title,
                        !showInput,
                        onClick = {
                            showInput = false
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (showInput) {
                            task.input?.input ?: ""
                        } else {
                            task.output?.output ?: stringResource(R.string.assistant_screen_task_output_empty_text)
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                    )
                }

                TaskStatusView(task, foregroundColor = colorResource(R.color.text_color), capability)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TextInputSelectButton(modifier: Modifier, titleId: Int, highlightCondition: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = if (highlightCondition) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        },
        modifier = modifier
            .widthIn(min = 0.dp, max = 200.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(text = stringResource(id = titleId), color = MaterialTheme.colorScheme.surface)
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun TaskDetailScreenPreview() {
    TaskDetailBottomSheet(
        task = Task(
            1,
            "Free Prompt",
            null,
            "1",
            "1",
            TaskInput("Give me text".getRandomString(100)),
            TaskOutput("output".getRandomString(300)),
            1707692337,
            1707692337,
            1707692337,
            1707692337,
            1707692337
        ),
        OCCapability().apply {
            versionMayor = 30
        },
        showTaskActions = {
        }
    ) {
    }
}
