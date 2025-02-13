/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.SyncedFolderDisplayItem

fun List<SyncedFolderDisplayItem>.getEnabledOrWithoutEnabledParent(): List<SyncedFolderDisplayItem> {
    val enabledFolders = filter { it.isEnabled }.map { it.localPath }.toSet()

    return filter { folder ->
        val hasEnabledParent = enabledFolders.any { parentPath ->
            folder.localPath.startsWith("$parentPath/")
        }
        folder.isEnabled || !hasEnabledParent
    }
}
