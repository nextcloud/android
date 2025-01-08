/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.model

import android.app.Activity
import com.nextcloud.client.assistant.extensions.getInput
import com.nextcloud.client.assistant.extensions.getInputAndOutput
import com.nextcloud.utils.extensions.showShareIntent
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.utils.ClipboardUtil

sealed class ScreenOverlayState {
    data class DeleteTask(val id: Long) : ScreenOverlayState()
    data class AddTask(val taskType: TaskTypeData, val input: String) : ScreenOverlayState()
    data class TaskActions(val task: Task) : ScreenOverlayState() {
        private fun getInputAndOutput(): String = task.getInputAndOutput()
        private fun getInput(): String? = task.getInput()

        private fun getCopyToClipboardAction(activity: Activity): Triple<Int, Int, () -> Unit> = Triple(
            R.drawable.ic_content_copy,
            R.string.common_copy
        ) {
            ClipboardUtil.copyToClipboard(activity, getInputAndOutput(), showToast = false)
        }

        private fun getShareAction(activity: Activity): Triple<Int, Int, () -> Unit> = Triple(
            R.drawable.ic_share,
            R.string.common_share
        ) {
            activity.showShareIntent(getInputAndOutput())
        }

        private fun getEditAction(activity: Activity, onComplete: (AddTask) -> Unit): Triple<Int, Int, () -> Unit> =
            Triple(
                R.drawable.ic_edit,
                R.string.action_edit
            ) {
                val taskType = TaskTypeData(
                    task.type,
                    activity.getString(R.string.assistant_screen_add_task_alert_dialog_title),
                    null,
                    null,
                    null
                )
                val newState = AddTask(taskType, getInput() ?: "")
                onComplete(newState)
            }

        private fun getDeleteAction(onComplete: (DeleteTask) -> Unit): Triple<Int, Int, () -> Unit> = Triple(
            R.drawable.ic_delete,
            R.string.assistant_screen_task_more_actions_bottom_sheet_delete_action
        ) {
            val newState = DeleteTask(task.id)
            onComplete(newState)
        }

        fun getActions(
            activity: Activity,
            onEditCompleted: (AddTask) -> Unit,
            onDeleteCompleted: (DeleteTask) -> Unit
        ): List<Triple<Int, Int, () -> Unit>> = listOf(
            getShareAction(activity),
            getCopyToClipboardAction(activity),
            getEditAction(activity, onComplete = {
                onEditCompleted(it)
            }),
            getDeleteAction(onComplete = {
                onDeleteCompleted(it)
            })
        )
    }
}
