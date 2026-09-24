/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.text

import java.net.IDN

object LinkFormatter {

    private val WEB_SCHEMES = listOf("http://", "https://")
    private const val HANDLE_PREFIX = "@"
    private const val DOMAIN_LABEL_SEPARATOR = '.'
    private const val HOST_PREFIX = "//"
    private const val USER_INFO_SEPARATOR = "@"
    private const val PATH_SEPARATOR = '/'

    @JvmStatic
    fun removeScheme(url: String?): String {
        val link = url.orEmpty()
        val scheme = WEB_SCHEMES.firstOrNull { link.startsWith(it, ignoreCase = true) }.orEmpty()
        return link.drop(scheme.length).trim()
    }

    @JvmStatic
    fun formatHandle(handle: String?): String {
        val trimmed = handle?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.startsWith(HANDLE_PREFIX)) {
            return trimmed
        }
        return HANDLE_PREFIX + trimmed
    }

    @JvmStatic
    fun toAsciiDomain(url: String): String = convertHost(url, IDN::toASCII)

    @JvmStatic
    fun toUnicodeDomain(url: String): String = convertHost(url, IDN::toUnicode)

    private fun convertHost(url: String, convert: (String) -> String): String {
        val leadingDots = url.takeWhile { it == DOMAIN_LABEL_SEPARATOR }
        val link = url.drop(leadingDots.length)

        val hostStart = when {
            HOST_PREFIX in link -> link.indexOf(HOST_PREFIX) + HOST_PREFIX.length
            USER_INFO_SEPARATOR in link -> link.indexOf(USER_INFO_SEPARATOR) + USER_INFO_SEPARATOR.length
            else -> 0
        }
        val hostEnd = link.indexOf(PATH_SEPARATOR, hostStart).takeIf { it >= 0 } ?: link.length
        val host = link.substring(hostStart, hostEnd)

        return leadingDots + link.replaceRange(hostStart, hostEnd, convert(host))
    }
}
