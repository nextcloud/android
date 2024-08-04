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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.extensions.statusData
import com.owncloud.android.lib.resources.assistant.model.Task

@Composable
fun TaskStatus(task: Task, foregroundColor: Color) {
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
            colorFilter = ColorFilter.tint(foregroundColor),
            contentDescription = "status icon"
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(text = stringResource(id = descriptionId), color = foregroundColor)

        /*
        Spacer(modifier = Modifier.weight(1f))

        Text(text = task.completionDateRepresentation(), color = foregroundColor)

        Spacer(modifier = Modifier.width(6.dp))
         */
    }
}
