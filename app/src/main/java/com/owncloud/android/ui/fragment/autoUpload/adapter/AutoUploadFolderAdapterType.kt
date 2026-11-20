/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.adapter

enum class AutoUploadFolderAdapterType {
    ITEM,
    HEADER,
    FOOTER,
    EMPTY;

    val viewType: Int
        get() = ordinal

    companion object {
        fun fromViewType(viewType: Int): AutoUploadFolderAdapterType = entries[viewType]
    }
}
