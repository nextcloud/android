/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

enum class OCFileNavState {
    Root,
    SubDirOfRoot,
    SubDirOfDir;

    companion object {
        fun getNavState(path: String?): OCFileNavState? {
            if (path.isNullOrEmpty()) {
                return null
            }

            val pathSeparatorCount = path.count { it == OCFile.PATH_SEPARATOR_CHAR }
            return if (pathSeparatorCount == 0) {
                null
            } else if (pathSeparatorCount < 2) {
                Root
            } else if (pathSeparatorCount == 2) {
                SubDirOfRoot
            } else {
                SubDirOfDir
            }
        }
    }
}
