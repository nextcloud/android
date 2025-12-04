/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import com.owncloud.android.datamodel.OCFile

enum class SharedListNavState {
    Root, SubDirOfRoot, SubDirOfDir;

    companion object {
        fun getNavState(file: OCFile?): SharedListNavState? {
            val path = file?.remotePath.orEmpty()
            val pathSeparatorCount = path.count { it == OCFile.PATH_SEPARATOR_CHAR }
            return if (pathSeparatorCount == 2) {
                SubDirOfRoot
            } else if (pathSeparatorCount > 2) {
                SubDirOfDir
            } else {
                null
            }
        }
    }
}
