/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component.filename.model

import com.nextcloud.utils.view.LocaleDirection
import com.owncloud.android.utils.FileStorageUtils

sealed interface FilenameState {
    data class Single(val name: String) : FilenameState
    data class Split(val name: String, val extension: String) : FilenameState

    companion object {
        fun of(filename: String, isFolder: Boolean): FilenameState {
            if (!FileStorageUtils.containsBidiControlCharacters(filename)) {
                return Single(filename)
            }

            val (name, extension) = FileStorageUtils.getFilenameAndExtension(
                filename,
                isFolder,
                LocaleDirection.isRtl
            )

            // Folders and names without a dot have no extension, so only the name is shown.
            if (extension.isNullOrEmpty()) {
                return Single(name.orEmpty())
            }

            return Split(name.orEmpty(), extension)
        }
    }
}
