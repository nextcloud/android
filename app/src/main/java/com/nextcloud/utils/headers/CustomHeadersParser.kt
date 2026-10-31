/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import com.nextcloud.common.CustomHeaders

object CustomHeadersParser {
    private const val SEPARATOR = ':'
    private const val MAX_HEADERS = 10
    private const val LINE_SEPARATOR = "\n"

    fun format(headers: Map<String, String>): String =
        headers.entries.joinToString(LINE_SEPARATOR) { (name, value) -> "$name$SEPARATOR $value" }

    fun parse(text: String): CustomHeadersParseResult {
        val headers = linkedMapOf<String, String>()

        text.lines().forEachIndexed { index, line ->
            if (line.isBlank()) {
                return@forEachIndexed
            }

            val separatorIndex = line.indexOf(SEPARATOR)
            val name = line.substring(0, maxOf(separatorIndex, 0)).trim()
            val value = line.substring(separatorIndex + 1).trim()
            val duplicate = headers.keys.any { it.equals(name, ignoreCase = true) }

            if (separatorIndex < 0 || duplicate || !CustomHeaders.isValid(name, value)) {
                return CustomHeadersParseResult.Invalid(index + 1)
            }
            headers[name] = value
        }

        if (headers.size > MAX_HEADERS) {
            return CustomHeadersParseResult.Invalid(MAX_HEADERS + 1)
        }
        return CustomHeadersParseResult.Valid(headers)
    }
}
