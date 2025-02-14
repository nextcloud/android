/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem

/**
 * Returns a filtered list of folders that:
 *  - Are explicitly enabled.
 *  - Do not have an enabled parent folder.
 *
 * This prevents subfolders from being included if their parent is already enabled.
 *
 * **Example:**
 * ```
 * Given enabled folders: [ "/storage/emulated/0/DCIM/my_folder" ]
 *
 * ✅ /storage/emulated/0/DCIM/my_folder      (Included - explicitly enabled)
 * ❌ /storage/emulated/0/DCIM/my_folder/B    (Excluded - child of an enabled folder)
 * ✅ /storage/emulated/0/DCIM/my_folder_2    (Included - no enabled parent)
 * ```
 *
 * @receiver List of [SyncedFolderDisplayItem] representing synced folders.
 * @return A filtered list containing only enabled folders or those without an enabled parent.
 */
fun List<SyncedFolderDisplayItem>.filterEnabledOrWithoutParentInEnabledSet(): List<SyncedFolderDisplayItem> {
    val enabledFolders = filter { it.isEnabled }.map { it.localPath }.toSet()

    return filter { folder ->
        val hasEnabledParent = enabledFolders.any { parentPath ->
            folder.localPath.startsWith("$parentPath/")
        }
        folder.isEnabled || !hasEnabledParent
    }
}

/**
 * Returns a filtered list of folders that:
 *  - Are explicitly enabled.
 *  - Have an enabled parent folder.
 *
 * This ensures that only subfolders of an already enabled folder are included.
 *
 * **Example:**
 * ```
 * Given enabled folders: [ "/storage/emulated/0/DCIM/my_folder" ]
 *
 * ❌ /storage/emulated/0/DCIM/my_folder      (Excluded - explicitly enabled but not a subfolder)
 * ✅ /storage/emulated/0/DCIM/my_folder/B    (Included - child of an enabled folder)
 * ❌ /storage/emulated/0/DCIM/my_folder_2    (Excluded - no enabled parent)
 * ```
 *
 * @receiver List of [SyncedFolder] representing synced folders.
 * @return A filtered list containing only enabled subfolders of an already enabled parent.
 */
fun List<SyncedFolder>.filterEnabledSubfoldersWithEnabledParent(): List<SyncedFolder> {
    val enabledFolders = filter { it.isEnabled }.map { it.localPath }.toSet()

    return filter { folder ->
        val hasEnabledParent = enabledFolders.any { parentPath ->
            folder.localPath.startsWith("$parentPath/")
        }
        folder.isEnabled && hasEnabledParent
    }
}
