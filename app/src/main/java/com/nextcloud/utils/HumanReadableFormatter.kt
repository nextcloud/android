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

    @JvmStatic
    fun bytesToHumanReadable(bytes: Long): String {
        if (bytes < 0) {
            return MainApp.string(R.string.common_pending)
        }

        val unitIndex = ((63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10)
            .coerceIn(0, ByteUnit.entries.lastIndex)
        val unit = ByteUnit.entries[unitIndex]
        val size = bytes.toDouble() / (1L shl (10 * unitIndex))

        return "${String.format(Locale.ROOT, "%.${unit.decimals}f", size)} ${unit.suffix}"
    }

    @JvmStatic
    fun unixTimeToHumanReadable(milliseconds: Long): String =
        DateFormat.getDateTimeInstance().format(Date(milliseconds))
}
