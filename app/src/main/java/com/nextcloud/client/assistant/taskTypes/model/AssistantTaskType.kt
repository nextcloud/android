/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.taskTypes.model

import com.owncloud.android.lib.resources.assistant.model.TaskTypes

data class AssistantTaskType(
    val id: String?,
    val name: String?,
    val description: String?
)

fun TaskTypes.toAssistantTaskTypeList(): List<AssistantTaskType> {
    return arrayListOf<AssistantTaskType>().apply {
        add(AssistantTaskType("core:text2text", types.coreText2text.name, types.coreText2text.description))
        add(AssistantTaskType("core:text2text:topics", types.coreText2textTopics.name, types.coreText2textTopics.description))
        add(AssistantTaskType("core:text2text:headline", types.coreText2textHeadline.name, types.coreText2textHeadline.description))
        add(AssistantTaskType("core:text2text:summary", types.coreText2textSummary.name, types.coreText2textSummary.description))
        add(AssistantTaskType("core:text2text:translate", types.coreText2textTranslate.name, types.coreText2textTranslate.description))
        add(AssistantTaskType("core:text2image", types.coreText2image.name, types.coreText2image.description))
        add(AssistantTaskType("core:audio2text", types.coreAudio2text.name, types.coreAudio2text.description))
        add(AssistantTaskType("core:contextwrite", types.coreContextwrite.name, types.coreContextwrite.description))
        add(AssistantTaskType("context_chat:context_chat", types.contextChatContextChat.name, types.contextChatContextChat.description))
    }
}
