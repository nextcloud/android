/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EnforcedServerPolicy {
    private val gson = Gson()

    fun parse(enforcedServersJson: String): List<EnforcedServer> =
        gson.fromJson(enforcedServersJson, object : TypeToken<List<EnforcedServer>>() {}.type)

    fun isAllowed(server: String, webViewLoginUrl: String, enforcedServersJson: String): Boolean {
        if (webViewLoginUrl.isNotEmpty() && webViewLoginUrl.startsWith(server)) {
            return true
        }

        if (enforcedServersJson.isEmpty()) {
            return false
        }

        return parse(enforcedServersJson).any { it.url.startsWith(server) }
    }
}
