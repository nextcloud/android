/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.model

import com.owncloud.android.lib.resources.assistant.model.Task
import com.owncloud.android.lib.resources.assistant.model.TaskTypeData

sealed class ScreenOverlayState {
    data class DeleteTask(val id: Long) : ScreenOverlayState()
    data class AddTask(val taskType: TaskTypeData, val input: String) : ScreenOverlayState()
    data class TaskActions(val task: Task) : ScreenOverlayState()
}
