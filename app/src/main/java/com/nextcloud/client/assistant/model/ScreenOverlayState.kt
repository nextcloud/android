/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.model

import com.nextcloud.client.assistant.taskTypes.model.AssistantTaskType
import com.owncloud.android.lib.resources.assistant.model.Task

sealed class ScreenOverlayState {
    data class DeleteTask(val id: Long): ScreenOverlayState()
    data class AddTask(val taskType: AssistantTaskType, val input: String): ScreenOverlayState()
    data class TaskActions(val task: Task): ScreenOverlayState()
}
