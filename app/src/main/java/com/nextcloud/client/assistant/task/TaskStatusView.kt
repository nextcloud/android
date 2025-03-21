/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.assistant.task

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.extensions.getModifiedAtRepresentation
import com.nextcloud.client.assistant.extensions.getStatusIcon
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskInput
import com.owncloud.android.lib.resources.assistant.v2.model.TaskOutput
import com.owncloud.android.lib.resources.status.OCCapability
import java.util.concurrent.TimeUnit

@Composable
fun TaskStatusView(task: Task, capability: OCCapability) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconId = task.getStatusIcon(capability)
        val description = task.getModifiedAtRepresentation(context)

        Image(
            painter = painterResource(id = iconId),
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(color = colorResource(R.color.text_color)),
            contentDescription = "status icon"
        )

        description?.let {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = description, color = colorResource(R.color.text_color))
        }
    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
@Preview
private fun TaskStatusViewPreview() {
    val currentTime = System.currentTimeMillis() / 1000

    val tasks = listOf(
        Task(
            id = 1L,
            type = "type1",
            status = "STATUS_RUNNING",
            userId = "user1",
            appId = "app1",
            input = TaskInput("input1"),
            output = TaskOutput("output1"),
            scheduledAt = currentTime.toInt(),
            lastUpdated = currentTime.toInt()
        ),

        Task(
            id = 2L,
            type = "type2",
            status = "STATUS_SUCCESSFUL",
            userId = "user2",
            appId = "app2",
            input = TaskInput("input2"),
            output = TaskOutput("output2"),
            lastUpdated = (currentTime - TimeUnit.MINUTES.toSeconds(5)).toInt()
        ),

        Task(
            id = 3L,
            type = "type3",
            status = "STATUS_RUNNING",
            userId = "user3",
            appId = "app3",
            input = TaskInput("input3"),
            output = TaskOutput("output3"),
            lastUpdated = (currentTime - TimeUnit.HOURS.toSeconds(5)).toInt()
        ),

        Task(
            id = 4L,
            type = "type4",
            status = "STATUS_SUCCESSFUL",
            userId = "user4",
            appId = "app4",
            input = TaskInput("input4"),
            output = TaskOutput("output4"),
            lastUpdated = (currentTime - TimeUnit.DAYS.toSeconds(5)).toInt()
        ),

        Task(
            id = 5L,
            type = "type5",
            status = "STATUS_SUCCESSFUL",
            userId = "user5",
            appId = "app5",
            input = TaskInput("input5"),
            output = TaskOutput("output5"),
            lastUpdated = (currentTime - TimeUnit.DAYS.toSeconds(60)).toInt()
        ),

        Task(
            id = 6L,
            type = "type7",
            status = "STATUS_UNKNOWN",
            userId = "user7",
            appId = "app7",
            input = TaskInput("input7"),
            output = TaskOutput("output7"),
            scheduledAt = null,
            lastUpdated = null
        )
    )

    LazyColumn {
        items(tasks) {
            TaskStatusView(
                it,
                OCCapability().apply {
                    versionMayor = 30
                }
            )
        }
    }
}
