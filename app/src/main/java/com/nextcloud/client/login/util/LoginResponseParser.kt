/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.util

import com.nextcloud.client.login.model.LoginSession
import com.nextcloud.client.login.model.LoginSessionResponse
import com.nextcloud.client.login.model.LoginResponse
import com.owncloud.android.authentication.LoginUrlInfo
import kotlinx.serialization.json.Json

object LoginResponseParser {

    private const val POLL_PATH_SUFFIX = "/poll"

    private val json = Json { ignoreUnknownKeys = true }

    fun parseSession(requestUrl: String, body: String): LoginSession? {
        val response = runCatching { json.decodeFromString<LoginSessionResponse>(body) }.getOrNull()
        val loginUrl = response?.login.orEmpty()
        val token = response?.poll?.token.orEmpty()
        val endpoint = response?.poll?.endpoint?.takeIf { it.isNotEmpty() } ?: (requestUrl + POLL_PATH_SUFFIX)

        return if (loginUrl.isEmpty() || token.isEmpty()) {
            null
        } else {
            LoginSession(loginUrl, endpoint, token)
        }
    }

    fun parseCredentials(response: LoginResponse): LoginUrlInfo? {
        if (response.body.isEmpty()) {
            return null
        }

        val credentials = runCatching { json.decodeFromString<LoginUrlInfo>(response.body) }.getOrNull()
        return credentials?.takeIf { it.isValid(response.status) }
    }
}
