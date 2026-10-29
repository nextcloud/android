/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.loginurl

import java.net.URLDecoder

object LoginDataUrlParser {
    private const val ELEMENT_SEPARATOR = "&"
    private const val KEY_VALUE_SEPARATOR = ":"
    private const val MAX_ELEMENTS = 3
    private const val USER_KEY = "user$KEY_VALUE_SEPARATOR"
    private const val PASSWORD_KEY = "password$KEY_VALUE_SEPARATOR"
    private const val SERVER_KEY = "server$KEY_VALUE_SEPARATOR"

    /**
     * Parses a login URI of the format `<prefix>server:xxx&user:xxx&password:xxx`, where every element is
     * optional.
     *
     * @param prefix     URI beginning, e.g. cloud://login/
     * @param dataString the complete URI
     * @throws IllegalArgumentException when the URI is shorter than [prefix] or has more than three elements
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun parse(prefix: String, dataString: String): LoginUrlInfo {
        require(dataString.length >= prefix.length) { "Invalid login URL detected" }

        val values = splitElements(dataString.substring(prefix.length))
        require(values.size in 1..MAX_ELEMENTS) {
            "Illegal number of login URL elements detected: ${values.size}"
        }

        val loginUrlInfo = LoginUrlInfo("", "", "")
        for (value in values) {
            when {
                value.startsWith(USER_KEY) -> loginUrlInfo.loginName = decodeValue(value, USER_KEY)
                value.startsWith(PASSWORD_KEY) -> loginUrlInfo.appPassword = decodeValue(value, PASSWORD_KEY)
                value.startsWith(SERVER_KEY) -> loginUrlInfo.server = decodeValue(value, SERVER_KEY)
            }
        }
        return loginUrlInfo
    }

    private fun splitElements(data: String): List<String> {
        val elements = data.split(ELEMENT_SEPARATOR)
        if (elements.size == 1) {
            return elements
        }
        return elements.dropLastWhile { it.isEmpty() }
    }

    @Suppress("DEPRECATION")
    private fun decodeValue(value: String, key: String): String = URLDecoder.decode(value.substring(key.length))
}
