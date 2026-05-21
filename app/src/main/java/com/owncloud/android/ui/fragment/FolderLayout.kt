/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile

sealed class FolderLayout(val key: String) {
    data object Shared : FolderLayout("shared_folder_layout")
    data object Favorites : FolderLayout("favorite_folder_layout")
    data object AllFiles : FolderLayout("all_files_folder_layout")
    data class Child(val folder: OCFile) : FolderLayout("folder_layout")

    fun getPrefKey(user: User): String {
        if (this is Child) {
            return key
        }

        return user.accountName + "_" + key
    }

    companion object {
        fun get(folder: OCFile?, searchType: SearchType): FolderLayout {
            if (folder != null && folder.isFolder && !folder.isRootDirectory) {
                return Child(folder)
            }

            return when (searchType) {
                SearchType.SHARED_FILTER -> Shared
                SearchType.FAVORITE_SEARCH -> Favorites
                else -> AllFiles
            }
        }
    }
}
