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

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
import com.nextcloud.ui.composeComponents.bottomSheet.MoreActionsBottomSheet
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("ResourceAsColor")
@Composable
fun TaskView(
    task: Task,
    showDeleteTaskAlertDialog: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMoreActionsBottomSheet by remember { mutableStateOf(false) }

    // TODO Check color
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(R.color.primary))
            .combinedClickable(onClick = {
                expanded = !expanded
            }, onLongClick = {
                showMoreActionsBottomSheet = true
            })
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.assistant_screen_task_view_input),
            modifier = Modifier.padding(4.dp),
            color = Color.White
        )

        task.input?.let {
            Text(
                text = it,
                modifier = Modifier.padding(4.dp),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.assistant_screen_task_view_output),
            color = Color.White,
            modifier = Modifier
                .padding(4.dp)
        )

        task.output?.let {
            Text(
                text = if (expanded) it else it.take(100) + "...",
                color = Color.White,
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .padding(4.dp)
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
