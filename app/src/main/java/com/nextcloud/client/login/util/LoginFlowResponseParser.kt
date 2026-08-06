/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.util

import com.nextcloud.client.login.model.LoginFlowSession
import com.nextcloud.client.login.model.LoginFlowSessionResponse
import com.nextcloud.client.login.model.LoginFlowResponse
import com.owncloud.android.authentication.LoginUrlInfo
import kotlinx.serialization.json.Json

object LoginFlowResponseParser {

    private const val POLL_PATH_SUFFIX = "/poll"

    private val json = Json { ignoreUnknownKeys = true }

    fun parseSession(requestUrl: String, body: String): LoginFlowSession? {
        val response = runCatching { json.decodeFromString<LoginFlowSessionResponse>(body) }.getOrNull()
        val loginUrl = response?.login.orEmpty()
        val token = response?.poll?.token.orEmpty()
        val endpoint = response?.poll?.endpoint?.takeIf { it.isNotEmpty() } ?: (requestUrl + POLL_PATH_SUFFIX)

        return if (loginUrl.isEmpty() || token.isEmpty()) {
            null
        } else {
            LoginFlowSession(loginUrl, endpoint, token)
        }
    }

    fun parseCredentials(response: LoginFlowResponse): LoginUrlInfo? {
        if (response.body.isEmpty()) {
            return null
        }

        val credentials = runCatching { json.decodeFromString<LoginUrlInfo>(response.body) }.getOrNull()
        return credentials?.takeIf { it.isValid(response.status) }
    }
}
