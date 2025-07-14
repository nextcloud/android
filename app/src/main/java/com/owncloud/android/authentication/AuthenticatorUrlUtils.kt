/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication

import java.net.URI

/**
 * Helper class for authenticator-URL related logic.
 */
object AuthenticatorUrlUtils {

    private const val REMOTE_PHP_PATH = "/remote.php/dav"

    fun normalizeUrlSuffix(url: String): String {
        var normalizedUrl = url
        if (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length - 1)
        }
        return trimUrlWebdav(normalizedUrl)
    }

    fun trimWebdavSuffix(url: String): String {
        var trimmedUrl = url
        while (trimmedUrl.endsWith("/")) {
            trimmedUrl = trimmedUrl.substring(0, url.length - 1)
        }
        val pos = trimmedUrl.lastIndexOf(REMOTE_PHP_PATH)
        if (pos >= 0) {
            trimmedUrl = trimmedUrl.substring(0, pos)
        }
        return trimmedUrl
    }

    private fun trimUrlWebdav(url: String): String = if (url.lowercase().endsWith(REMOTE_PHP_PATH)) {
        url.substring(0, url.length - REMOTE_PHP_PATH.length)
    } else {
        url
    }

    fun stripIndexPhpOrAppsFiles(url: String): String {
        var strippedUrl = url
        if (strippedUrl.endsWith("/index.php")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.lastIndexOf("/index.php"))
        } else if (strippedUrl.contains("/index.php/apps/")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.lastIndexOf("/index.php/apps/"))
        }
        return strippedUrl
    }

    fun normalizeScheme(url: String): String = if (url.matches("[a-zA-Z][a-zA-Z0-9+.-]+://.+".toRegex())) {
        val uri = URI.create(url)
        val lcScheme = uri.scheme.lowercase()
        String.format("%s:%s", lcScheme, uri.rawSchemeSpecificPart)
    } else {
        url
    }
}
