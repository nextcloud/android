/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist

internal enum class FileListViewType {
    FOOTER,
    HEADER,
    LIST_ITEM,
    GRID_ITEM;

    companion object {
        fun fromOrdinal(ordinal: Int): FileListViewType = entries.getOrElse(ordinal) { LIST_ITEM }
    }
}
