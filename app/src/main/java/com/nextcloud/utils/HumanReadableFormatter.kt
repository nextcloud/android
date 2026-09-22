/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.MainApp
import com.owncloud.android.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

object HumanReadableFormatter {

    private const val BYTE_SIZE_DIVIDER = 1024.0

    @JvmStatic
    fun bytesToHumanReadable(bytes: Long): String {
        if (bytes < 0) {
            return MainApp.string(R.string.common_pending)
        }

        var size = bytes.toDouble()
        var unitIndex = 0
        while (size > BYTE_SIZE_DIVIDER && unitIndex < ByteUnit.entries.lastIndex) {
            size /= BYTE_SIZE_DIVIDER
            unitIndex++
        }

        val unit = ByteUnit.entries[unitIndex]
        return "${String.format(Locale.ROOT, "%.${unit.decimals}f", size)} ${unit.suffix}"
    }

    @JvmStatic
    fun unixTimeToHumanReadable(milliseconds: Long): String =
        DateFormat.getDateTimeInstance().format(Date(milliseconds))
}
