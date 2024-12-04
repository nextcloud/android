/*
 * Nextcloud Android client application
 *
 * SPDX-FileCopyrightText: 2023-2024 Nextcloud GmbH and Nextcloud
 * contributors
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: MIT
 */

package com.nextcloud.client.assistant.extensions

import android.content.Context
import com.nextcloud.utils.date.DateFormatPattern
import com.nextcloud.utils.date.DateFormatter
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.Task
import java.util.concurrent.TimeUnit

fun Task.getInput(): String? = input?.input

fun Task.getOutput(): String? = output?.output

@Suppress("MagicNumber")
fun Task.getInputTitle(): String {
    val maxTitleLength = 20
    val title = getInput() ?: ""

    return if (title.length > maxTitleLength) {
        title.take(maxTitleLength) + "..."
    } else {
        title
    }
}

@Suppress("MagicNumber")
fun Task.getStatusIcon(): Int {
    return when (status) {
        "STATUS_UNKNOWN" -> {
            R.drawable.ic_unknown
        }
        "STATUS_SCHEDULED" -> {
            R.drawable.ic_clock
        }
        "STATUS_RUNNING" -> {
            R.drawable.ic_modification_desc
        }
        "STATUS_SUCCESSFUL" -> {
            R.drawable.ic_check_circle_outline
        }
        "STATUS_FAILED" -> {
            R.drawable.image_fail
        }
        else -> {
            R.drawable.ic_unknown
        }
    }
}

@Suppress("MagicNumber")
fun Task.getModifiedAtRepresentation(context: Context): String? {
    if (lastUpdated == null) {
        return null
    }

    val modifiedAt = lastUpdated!!.toLong()
    val currentTime = System.currentTimeMillis() / 1000
    val timeDifference = (currentTime - modifiedAt).toInt()
    val timeDifferenceInMinutes = (timeDifference / 60)
    val timeDifferenceInHours = (timeDifference / 3600)

    return when {
        timeDifference < 0 -> {
            context.getString(R.string.common_now)
        }

        timeDifference < TimeUnit.MINUTES.toSeconds(1) -> {
            context.resources.getQuantityString(R.plurals.time_seconds_ago, timeDifference, timeDifference)
        }

        timeDifference < TimeUnit.HOURS.toSeconds(1) -> {
            context.resources.getQuantityString(
                R.plurals.time_minutes_ago,
                timeDifferenceInMinutes,
                timeDifferenceInMinutes
            )
        }

        timeDifference < TimeUnit.DAYS.toSeconds(1) -> {
            context.resources.getQuantityString(
                R.plurals.time_hours_ago,
                timeDifferenceInHours,
                timeDifferenceInHours
            )
        }

        else -> {
            DateFormatter.timestampToDateRepresentation(modifiedAt, DateFormatPattern.MonthWithDate)
        }
    }
}
