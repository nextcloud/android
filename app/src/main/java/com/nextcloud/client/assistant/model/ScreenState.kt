/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.model

import com.owncloud.android.R

sealed class ScreenState {
    data object Loading : ScreenState()

    data object Content : ScreenState()

    data class EmptyContent(val iconId: Int?, val titleId: Int?, val descriptionId: Int) : ScreenState()

    companion object {
        fun emptyTaskTypes(): ScreenState = EmptyContent(
            titleId = null,
            descriptionId = R.string.assistant_screen_task_list_empty_warning,
            iconId = null
        )

        fun emptyTaskList(): ScreenState = EmptyContent(
            iconId = R.drawable.ic_assistant,
            titleId = R.string.assistant_screen_chat_title,
            descriptionId = R.string.assistant_screen_chat_description,
        )
    }
}
