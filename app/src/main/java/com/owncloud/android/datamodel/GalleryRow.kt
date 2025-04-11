/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

data class GalleryRow(val files: List<OCFile>, val defaultHeight: Int, val defaultWidth: Int) {
    fun getMaxHeight(): Float {
        return files.map { it.imageDimension?.height ?: defaultHeight.toFloat() }.maxOrNull() ?: 0f
    }

    fun calculateHashCode(): Long {
        return files.sumOf { it.hashCode() }.toLong()
    }
}
