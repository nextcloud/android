/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.common.SearchResultEntry
import java.text.SimpleDateFormat
import java.util.Locale

fun SearchResultEntry.parseDateTimeToMillis(): Long {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val date = sdf.parse(subline)
    return date?.time ?: 0
}
