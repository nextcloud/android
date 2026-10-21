/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.nextcloud.utils.date.DateFormatPattern
import com.nextcloud.utils.text.DisplayTextFormatter

data class GalleryItems(val date: Long, val rows: List<GalleryRow>) {
    override fun toString(): String {
        val month = DisplayTextFormatter.formatDate(date, DateFormatPattern.FullMonth)
        val year = DisplayTextFormatter.formatDate(date, DateFormatPattern.Year)
        return "$month/$year with $rows rows"
    }
}
