/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.model

import android.app.Activity
import com.nextcloud.client.assistant.extensions.getInputAndOutput
import com.nextcloud.utils.extensions.showShareIntent
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.utils.ClipboardUtil

sealed class ScreenOverlayState {
    data class DeleteTask(val id: Long) : ScreenOverlayState()
    data class TaskActions(val task: Task) : ScreenOverlayState() {
        private fun getInputAndOutput(): String = task.getInputAndOutput()

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

        private fun getDeleteAction(onComplete: (DeleteTask) -> Unit): Triple<Int, Int, () -> Unit> = Triple(
            R.drawable.ic_delete,
            R.string.assistant_screen_task_more_actions_bottom_sheet_delete_action
        ) {
            val newState = DeleteTask(task.id)
            onComplete(newState)
        }

        fun getActions(
            activity: Activity,
            onDeleteCompleted: (DeleteTask) -> Unit
        ): List<Triple<Int, Int, () -> Unit>> = listOf(
            getShareAction(activity),
            getCopyToClipboardAction(activity),
            getDeleteAction(onComplete = {
                onDeleteCompleted(it)
            })
        )
    }
}
