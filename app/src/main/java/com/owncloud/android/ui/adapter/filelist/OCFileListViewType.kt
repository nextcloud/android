/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist

internal enum class OCFileListViewType {
    FOOTER,
    ITEM,
    IMAGE,
    HEADER;

    companion object {
        fun fromOrdinal(ordinal: Int): OCFileListViewType = entries.getOrElse(ordinal) { ITEM }
    }
}
