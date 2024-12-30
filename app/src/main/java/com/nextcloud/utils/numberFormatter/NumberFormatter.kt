/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.numberFormatter

import java.text.NumberFormat
import java.util.Locale

object NumberFormatter {

    @Suppress("MagicNumber")
    fun getPercentageText(percent: Int): String {
        val formatter = NumberFormat.getPercentInstance(Locale.getDefault())
        formatter.maximumFractionDigits = 0
        return formatter.format(percent / 100.0)
    }
}
