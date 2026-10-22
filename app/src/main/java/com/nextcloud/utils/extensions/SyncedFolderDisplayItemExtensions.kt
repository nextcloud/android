/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import java.util.Locale

private val prioritizedFolders = listOf("Camera", "Screenshots")

fun List<SyncedFolderDisplayItem?>.sortAutoUploadFolderItems(): List<SyncedFolderDisplayItem?> =
    sortedWith(::compareItems)

private fun compareItems(f1: SyncedFolderDisplayItem?, f2: SyncedFolderDisplayItem?): Int = when {
    f1 == null && f2 == null -> 0
    f1 == null -> -1
    f2 == null -> 1
    f1.isEnabled && f2.isEnabled -> compareEnabledItems(f1.folderName, f2.folderName)
    else -> compareMixedItems(f1, f2)
}

private fun compareEnabledItems(name1: String?, name2: String?): Int = when {
    name1 == null -> -1
    name2 == null -> 1
    else -> compareNames(name1, name2)
}

private fun compareMixedItems(f1: SyncedFolderDisplayItem, f2: SyncedFolderDisplayItem): Int {
    val name1 = f1.folderName
    val name2 = f2.folderName
    return when {
        name1 == null && name2 == null -> 0
        f1.isEnabled -> -1
        f2.isEnabled -> 1
        name1 == null -> -1
        name2 == null -> 1
        else -> comparePrioritizedNames(name1, name2)
    }
}

private fun comparePrioritizedNames(name1: String, name2: String): Int {
    val prioritizedFolder = prioritizedFolders.firstOrNull { it == name1 || it == name2 }
    return when {
        prioritizedFolder == null -> compareNames(name1, name2)
        prioritizedFolder == name1 && prioritizedFolder == name2 -> 0
        prioritizedFolder == name1 -> -1
        else -> 1
    }
}

private fun compareNames(name1: String, name2: String): Int =
    name1.lowercase(Locale.getDefault()).compareTo(name2.lowercase(Locale.getDefault()))
