/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.authentication

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EnforcedServer(val name: String? = null, val url: String? = null) {
    companion object {
        // Branded setup.xml values of enforce_servers must be standard JSON. Lenient parsing and nullable fields
        // keep loose input working, but single quotes and # comments are not parsed and crash the login screen.
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowComments = true
        }

        @JvmStatic
        fun fromJson(value: String): List<EnforcedServer> = json.decodeFromString(value)
    }
}
