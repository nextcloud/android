/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.files

import android.net.Uri
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager

/**
 * This component parses and matches deep links.
 * Result is returned to the UI for further processing.
 *
 * TODO: This is intermediate refactoring step; this component should be moved into
 *       [com.nextcloud.client.mixins.ActivityMixin] and handle UI callbacks as well
 */
@Suppress("ForbiddenComment")
class DeepLinkHandler(
    private val userAccountManager: UserAccountManager
) {

    /**
     * Provide parsed link arguments and context information required
     * to launch it.
     */
    data class Match(val users: List<User>, val fileId: String)

    companion object {
        val DEEP_LINK_PATTERN = Regex("""(.*?)(/index\.php)?/f/([0-9]+)$""")
        val BASE_URL_GROUP_INDEX = 1
        val INDEX_PATH_GROUP_INDEX = 2
        val FILE_ID_GROUP_INDEX = 3

        fun isDeepLinkTypeIsNavigation(deepLinkUrl: String): Boolean =
            DeepLinkConstants.navigationPaths.any { deepLinkUrl.endsWith(it) }
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
        val match = DEEP_LINK_PATTERN.matchEntire(uri.toString())
        if (match != null) {
            val baseServerUrl = match.groupValues[BASE_URL_GROUP_INDEX]
            val fileId = match.groupValues[FILE_ID_GROUP_INDEX]
            return Match(users = getMatchingUsers(baseServerUrl), fileId = fileId)
        } else {
            return null
        }
    }

    private fun getMatchingUsers(serverBaseUrl: String): List<User> =
        userAccountManager.allUsers.filter { it.server.uri.toString() == serverBaseUrl }
}
