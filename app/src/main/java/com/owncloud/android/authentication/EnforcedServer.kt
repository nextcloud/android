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
data class EnforcedServer(val name: String, val url: String) {
    companion object {
        // Branded setup.xml values of enforce_servers must be standard JSON with both name and url set.
        // Lenient parsing keeps loose input working, but single quotes, # comments and missing or null fields
        // are not parsed and crash the login screen.
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
