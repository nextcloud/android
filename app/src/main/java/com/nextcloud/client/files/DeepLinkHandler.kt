/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.files

import android.net.Uri
import com.nextcloud.client.account.User

/**
 * This component parses and matches deep links.
 * Result is returned to the UI for further processing.
 *
 * TODO: This is intermediate refactoring step; this component should be moved into
 *       [com.nextcloud.client.mixins.ActivityMixin] and handle UI callbacks as well
 */
@Suppress("ForbiddenComment")
class DeepLinkHandler(
    private val users: List<User>
) {

    /**
     * Provide parsed link arguments and context information required
     * to launch it.
     */
    data class Match(val users: List<User>, val path: String, val fileExtension: String) {
        fun getFilePath(): String {
            return "/$path/$fileExtension"
        }
    }

    companion object {
        val DEEP_LINK_PATTERN_S = Regex("""(.*?)/index\.php/s/([a-zA-Z0-9]+)/(.*)$""")
        val DEEP_LINK_PATTERN_F = Regex("""(.*?)/index\.php/f/([a-zA-Z0-9]+)/(.*)$""")

        val BASE_URL_GROUP_INDEX = 1
        val PATH_GROUP_INDEX = 2
        val FILE_EXTENSION_GROUP_INDEX = 3
    }

    /**
     * Parse deep link and return a match result.
     * Matching result may depend on environmental factors, such
     * as app version or registered users.
     *
     * @param uri Deep link as arrived in incoming [android.content.Intent]
     * @return deep link match result with all context data required for further processing; null if link does not match
     */
    fun parseDeepLink(uri: Uri): Match? {
        val matchResult = getMatchResult(uri)

        return if (matchResult != null) {
            val baseServerUrl = matchResult.groupValues[BASE_URL_GROUP_INDEX]
            val path = matchResult.groupValues[PATH_GROUP_INDEX]
            val fileExtension = matchResult.groupValues[FILE_EXTENSION_GROUP_INDEX]
            Match(users = getMatchingUsers(baseServerUrl), path = path, fileExtension = fileExtension)
        } else {
            null
        }
    }

    private fun getMatchResult(uri: Uri): MatchResult? {
        return DEEP_LINK_PATTERN_S.matchEntire(uri.toString()) ?: DEEP_LINK_PATTERN_F.matchEntire(uri.toString())
    }

    private fun getMatchingUsers(serverBaseUrl: String): List<User> =
        users.filter { serverBaseUrl.contains(it.server.uri.toString())  }
}
