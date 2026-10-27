/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.account

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object AccountNameUtils {
    private const val SEPARATOR = '@'
    private const val PATH_SEPARATOR = '/'
    private const val HOST_LABEL_SEPARATOR = '.'
    private const val INDEX_PHP_PATH = "/index.php"
    private const val HTTPS_SCHEME = "https"
    private const val HTTPS_PORT = 443

    @JvmStatic
    fun build(user: String, server: String): String = "$user$SEPARATOR$server"

    @JvmStatic
    fun getUserPart(accountName: String): String = accountName.substringBeforeLast(SEPARATOR)

    @JvmStatic
    fun getServerPart(accountName: String): String = accountName.substringAfterLast(SEPARATOR, "")

    @JvmStatic
    fun isSameServer(firstServerUrl: String, secondServerUrl: String): Boolean =
        firstServerUrl.toComparableServerUrl() == secondServerUrl.toComparableServerUrl()

    private fun String.toComparableServerUrl(): String {
        val url = toHttpUrlOrNull() ?: return this
        val port = if (url.port == HttpUrl.defaultPort(url.scheme)) HTTPS_PORT else url.port
        val path = url.encodedPath
            .trimEnd(PATH_SEPARATOR)
            .removeSuffix(INDEX_PHP_PATH)
            .trimEnd(PATH_SEPARATOR)
            .ifEmpty { PATH_SEPARATOR.toString() }

        return HttpUrl.Builder()
            .scheme(HTTPS_SCHEME)
            .host(url.host.trimEnd(HOST_LABEL_SEPARATOR))
            .port(port)
            .encodedPath(path)
            .build()
            .toString()
    }
}
