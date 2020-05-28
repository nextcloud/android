package com.nextcloud.client.files

import android.content.Context
import android.net.Uri
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import java.util.regex.Pattern


class DeepLinkHandler(
    private val context: Context,
    private val userAccountManager: UserAccountManager
) {

    data class Match(val users: List<User>, val fileId: String)

    companion object {
        val DEEP_LINK_PATTERN = Regex("""(.*?)(/index\.php)?/f/([0-9]+)$""")
        val BASE_URL_GROUP_INDEX = 1
        val INDEX_PATH_GROUP_INDEX = 2
        val FILE_ID_GROUP_INDEX = 3
    }

    fun parseDeepLink(uri: Uri): Match? {
        val match = DEEP_LINK_PATTERN.matchEntire(uri.toString())
        if (match != null) {
            val baseServerUrl = match.groupValues[BASE_URL_GROUP_INDEX]
            val fielId = match.groupValues[FILE_ID_GROUP_INDEX]
            return Match(users = getUsers(baseServerUrl), fileId = fielId)
        } else {
            return null
        }
    }

    private fun getUsers(serverBaseUrl: String): List<User> =
        userAccountManager.allUsers.filter { it.server.uri.toString() == serverBaseUrl }
}
