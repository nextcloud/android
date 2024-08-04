/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

enum class Level(val tag: String) {
    UNKNOWN("U"),
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARNING("W"),
    ERROR("E"),
    ASSERT("A");

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
