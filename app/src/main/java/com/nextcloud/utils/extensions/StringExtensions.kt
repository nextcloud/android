/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.utils.extensions

fun String.getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    val result = (1..length)
        .map { allowedChars.random() }
        .joinToString("")

    return this + result
}

fun String.removeFileExtension(): String {
    val dotIndex = lastIndexOf('.')
    return if (dotIndex != -1) {
        substring(0, dotIndex)
    } else {
        this
    }
}

fun String.truncateWithEllipsis(limit: Int) = take(limit) + if (length > limit) StringConstants.THREE_DOT else ""

object StringConstants {
    const val SLASH = "/"
    const val DOT = "."
    const val SPACE = " "
    const val THREE_DOT = "..."
}

fun String.getContentOfPublicKey(): String {
    return replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\s+".toRegex(), "")
}
