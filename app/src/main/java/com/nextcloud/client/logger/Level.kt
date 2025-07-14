/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

import com.owncloud.android.R

enum class Level(val tag: String) {
    UNKNOWN("U"),
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARNING("W"),
    ERROR("E"),
    ASSERT("A");

    fun getColor(): Int = when (this) {
        UNKNOWN -> R.color.log_level_unknown
        VERBOSE -> R.color.log_level_verbose
        DEBUG -> R.color.log_level_debug
        INFO -> R.color.log_level_info
        WARNING -> R.color.log_level_warning
        ASSERT -> R.color.log_level_assert
        ERROR -> R.color.log_level_error
    }

    companion object {
        @JvmStatic
        fun fromTag(tag: String): Level = when (tag) {
            "V" -> VERBOSE
            "D" -> DEBUG
            "I" -> INFO
            "W" -> WARNING
            "E" -> ERROR
            "A" -> ASSERT
            else -> UNKNOWN
        }
    }
}
