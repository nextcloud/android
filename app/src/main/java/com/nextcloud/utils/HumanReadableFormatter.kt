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

    private const val BITS_PER_UNIT = 10

    @JvmStatic
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) {
            return MainApp.string(R.string.common_pending)
        }

        val highestSetBit = Long.SIZE_BITS - 1 - bytes.countLeadingZeroBits()
        val unitIndex = (highestSetBit / BITS_PER_UNIT).coerceIn(0, ByteUnit.entries.lastIndex)
        val unit = ByteUnit.entries[unitIndex]
        val size = bytes.toDouble() / (1L shl (BITS_PER_UNIT * unitIndex))

        val pattern = "%.${unit.decimals}f"
        val formattedSize = String.format(Locale.ROOT, pattern, size)
        return "$formattedSize ${unit.suffix}"
    }

    @JvmStatic
    fun formatDateTime(milliseconds: Long): String = DateFormat.getDateTimeInstance().format(Date(milliseconds))
}
