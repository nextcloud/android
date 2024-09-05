/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.annotation.SuppressLint
import com.nextcloud.utils.date.DateFormatPattern
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
fun Date.currentDateRepresentation(formatPattern: DateFormatPattern): String {
    return SimpleDateFormat(formatPattern.pattern).format(this)
}
