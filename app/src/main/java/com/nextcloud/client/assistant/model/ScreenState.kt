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

    data class EmptyContent(val iconId: Int?, val descriptionId: Int) : ScreenState()

    companion object {
        fun emptyTaskTypes(): ScreenState = EmptyContent(
            descriptionId = R.string.assistant_screen_task_list_empty_warning,
            iconId = null
        )

        fun emptyTaskList(): ScreenState = EmptyContent(
            descriptionId = R.string.assistant_screen_create_a_new_task_from_bottom_right_text,
            iconId = null
        )
    }
}
