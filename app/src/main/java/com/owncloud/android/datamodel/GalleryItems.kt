/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.owncloud.android.utils.DisplayUtils

data class GalleryItems(val date: Long, val rows: List<GalleryRow>) {
    override fun toString(): String {
        val month = DisplayUtils.getDateByPattern(
            date,
            DisplayUtils.MONTH_PATTERN
        )
        val year = DisplayUtils.getDateByPattern(
            date,
            DisplayUtils.YEAR_PATTERN
        )
        return "$month/$year with $rows rows"
    }
}
