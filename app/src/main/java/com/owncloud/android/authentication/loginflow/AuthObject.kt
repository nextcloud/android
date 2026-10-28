/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.authentication.loginflow

import com.nextcloud.utils.serialization.AppJson
import kotlinx.serialization.Serializable

@Serializable
data class AuthObject(val poll: Poll, val login: String) {
    companion object {
        @JvmStatic
        fun fromJson(value: String): AuthObject = AppJson.instance.decodeFromString(value)

        @JvmStatic
        fun toJson(value: AuthObject): String = AppJson.instance.encodeToString(value)
    }
}

@Serializable
data class Poll(val token: String, val endpoint: String)
