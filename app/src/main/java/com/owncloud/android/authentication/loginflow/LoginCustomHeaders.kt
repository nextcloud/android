/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.loginflow

import android.os.Bundle
import androidx.core.net.toUri
import com.nextcloud.common.CustomHeaders

/**
 * Custom headers entered during login. They are registered for the origin the user typed, never for a URL that was
 * returned by a server, and only stored in the account if the account ends up on that origin.
 *
 * The registration is provisional: replacing or discarding it unregisters the origin and lets
 * [restoreAccountHeaders] register the headers of existing accounts again, which may share that origin.
 */
class LoginCustomHeaders(private val restoreAccountHeaders: () -> Unit) {
    private var origin: String? = null
    private var headersJson: String? = null

    /**
     * @return false if headers were entered but the server address is not https
     */
    fun capture(typedBaseUrl: String, headers: Map<String, String>): Boolean {
        discard()
        if (headers.isEmpty()) {
            return true
        }
        val baseUri = typedBaseUrl.toUri()
        if (!CustomHeaders.register(baseUri, headers)) {
            return false
        }
        origin = CustomHeaders.originOf(baseUri)
        headersJson = CustomHeaders.toJson(headers)
        return true
    }

    fun discard() {
        val previousOrigin = origin ?: return
        origin = null
        headersJson = null
        CustomHeaders.unregister(previousOrigin)
        restoreAccountHeaders()
    }

    fun jsonFor(accountBaseUrl: String): String? {
        val accountOrigin = CustomHeaders.originOf(accountBaseUrl.toUri())
        return headersJson.takeIf { origin != null && origin == accountOrigin }
    }

    fun save(outState: Bundle) {
        outState.putString(KEY_ORIGIN, origin)
        outState.putString(KEY_HEADERS, headersJson)
    }

    fun restore(savedState: Bundle) {
        val savedOrigin = savedState.getString(KEY_ORIGIN) ?: return
        val headers = CustomHeaders.fromJson(savedState.getString(KEY_HEADERS)) ?: return
        capture(savedOrigin, headers)
    }

    companion object {
        private const val KEY_ORIGIN = "LOGIN_CUSTOM_HEADERS_ORIGIN"
        private const val KEY_HEADERS = "LOGIN_CUSTOM_HEADERS"
    }
}
