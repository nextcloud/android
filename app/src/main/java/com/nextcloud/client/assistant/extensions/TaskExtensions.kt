/*
 * Nextcloud Android client application
 *
 * SPDX-FileCopyrightText: 2023-2024 Nextcloud GmbH and Nextcloud
 * contributors
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: MIT
 */

package com.nextcloud.client.assistant.extensions

import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task

@Suppress("MagicNumber")
fun Task.statusData(): Pair<Int, Int> {
    return when (status) {
        0L -> {
            Pair(R.drawable.ic_unknown, R.string.assistant_screen_unknown_task_status_text)
        }
        1L -> {
            Pair(R.drawable.ic_clock, R.string.assistant_screen_scheduled_task_status_text)
        }
        2L -> {
            Pair(R.drawable.ic_modification_desc, R.string.assistant_screen_running_task_text)
        }
        3L -> {
            Pair(R.drawable.ic_info, R.string.assistant_screen_successful_task_text)
        }
        4L -> {
            Pair(R.drawable.image_fail, R.string.assistant_screen_failed_task_text)
        }
        else -> {
            Pair(R.drawable.ic_unknown, R.string.assistant_screen_unknown_task_status_text)
        }
    }
}

// TODO add
fun Task.completionDateRepresentation(): String {
    return completionExpectedAt ?: "TODO IMPLEMENT IT"
}
