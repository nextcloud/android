/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.assistant.taskDetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.utils.extensions.getRandomString
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutput

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(task: Task, showTaskActions: () -> Unit, dismiss: () -> Unit) {
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
            }

            item {
                InputOutputCard(task)
            }
        }
    }
}

@Composable
fun InputOutputCard(task: Task) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
    ) {
        TitleDescriptionBox(
            title = stringResource(R.string.assistant_task_detail_screen_input_button_title),
            description = task.input?.input ?: ""
        )

        Spacer(modifier = Modifier.height(16.dp))

        TitleDescriptionBox(
            title = stringResource(R.string.assistant_task_detail_screen_output_button_title),
            description = task.output?.output ?: stringResource(R.string.assistant_screen_task_output_empty_text)
        )
    }
}

@Composable
private fun TitleDescriptionBox(title: String, description: String?) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = colorResource(R.color.text_color)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(color = colorResource(R.color.task_container), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = description ?: "",
            color = colorResource(R.color.text_color)
        )
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
        showTaskActions = {
        }
    ) {
    }
}
