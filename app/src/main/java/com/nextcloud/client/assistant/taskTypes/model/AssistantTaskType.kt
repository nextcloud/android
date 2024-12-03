/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.taskTypes.model

import com.owncloud.android.lib.resources.assistant.model.TaskIds
import com.owncloud.android.lib.resources.assistant.model.TaskTypes

data class AssistantTaskType(
    val id: String?,
    val name: String?,
    val description: String?
)

fun TaskTypes.toAssistantTaskTypeList(): List<AssistantTaskType> {
    return arrayListOf<AssistantTaskType>().apply {
        add(AssistantTaskType(TaskIds.GenerateText.id, types.generateText.name, types.generateText.description))
        add(AssistantTaskType(TaskIds.ExtractTopics.id, types.extractTopics.name, types.extractTopics.description))
        add(AssistantTaskType(TaskIds.GenerateHeadline.id, types.generateHeadline.name, types.generateHeadline.description))
        add(AssistantTaskType(TaskIds.Summarize.id, types.summarize.name, types.summarize.description))
    }
}
