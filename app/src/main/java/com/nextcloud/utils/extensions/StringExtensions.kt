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

/**
 * Checks if two nullable strings are both valid (non-null, non-empty, non-blank) and equal.
 *
 * It returns `true` only when both strings meet all the following criteria:
 * - Neither string is null
 * - Neither string is empty ("")
 * - Neither string contains only whitespace characters (spaces, tabs, newlines, etc.)
 * - Both strings are equal ignoring case differences
 *
 * @param other The other nullable string to compare with this string
 * @return `true` if both strings are valid and exactly equal, `false` otherwise
 */
fun String?.isNotBlankAndEquals(other: String?): Boolean = this != null &&
    other != null &&
    this.isNotBlank() &&
    other.isNotBlank() &&
    this.equals(other, ignoreCase = true)

fun String.truncateWithEllipsis(limit: Int) = take(limit) + if (length > limit) StringConstants.THREE_DOT else ""

object StringConstants {
    const val SLASH = "/"
    const val DOT = "."
    const val SPACE = " "
    const val THREE_DOT = "..."
    const val TEMP = "tmp"
}
