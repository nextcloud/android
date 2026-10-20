/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.autoUpload.data

import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.MediaProvider
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.SyncedFolderUtils
import java.io.File
import java.util.Locale

class AutoUploadItemsLoader(
    private val syncedFolderProvider: SyncedFolderProvider,
    private val itemFactory: AutoUploadDisplayItemFactory
) {

    fun load(
        activity: AppCompatActivity,
        perFolderMediaItemLimit: Int,
        currentAccountName: String,
        newItemAccountName: String
    ): List<SyncedFolderDisplayItem> {
        val mediaFolders = getMediaFolders(activity, perFolderMediaItemLimit)
        val syncedFolders = getAutoUploadFoldersAndDeleteObsolete(currentAccountName)
        val items = mergeFolderData(syncedFolders, mediaFolders, newItemAccountName)
        return sortAutoUploadFolderItems(items).filterNotNull()
    }

    private fun getMediaFolders(activity: AppCompatActivity, itemLimit: Int): List<MediaFolder> {
        val contentResolver = activity.contentResolver
        return MediaProvider.getImageFolders(contentResolver, itemLimit, activity, false) +
            MediaProvider.getVideoFolders(contentResolver, itemLimit, activity, false)
    }

    private fun getAutoUploadFoldersAndDeleteObsolete(accountName: String): List<SyncedFolder> {
        val (obsolete, active) = syncedFolderProvider.syncedFolders
            .filter { it.account == accountName }
            .partition { !File(it.localPath).exists() && !it.isEnabled }

        obsolete.forEach { syncedFolderProvider.deleteSyncedFolder(it.id) }
        return active
    }

    private fun mergeFolderData(
        syncedFolders: List<SyncedFolder>,
        mediaFolders: List<MediaFolder>,
        accountName: String
    ): List<SyncedFolderDisplayItem> {
        val syncedFoldersByKey = syncedFolders.associateByTo(HashMap()) { folderKey(it.localPath, it.type) }

        val mediaItems = mediaFolders.mapNotNull { mediaFolder ->
            val syncedFolder = syncedFoldersByKey.remove(folderKey(mediaFolder.absolutePath, mediaFolder.type))
            createDisplayItem(syncedFolder, mediaFolder, accountName)
        }
        val remainingItems = syncedFoldersByKey.values.map { itemFactory.createAutoUploadFolderWithoutMediaFolder(it) }

        return mediaItems + remainingItems
    }

    private fun createDisplayItem(
        syncedFolder: SyncedFolder?,
        mediaFolder: MediaFolder,
        accountName: String
    ): SyncedFolderDisplayItem? = when {
        syncedFolder == null -> createQualifyingMediaFolderItem(mediaFolder, accountName)

        !SyncedFolderUtils.isQualifyingMediaFolder(syncedFolder) -> null

        syncedFolder.type == MediaFolderType.CUSTOM -> itemFactory.createAutoUploadFolderWithoutMediaFolder(
            syncedFolder
        )

        else -> itemFactory.createAutoUploadFolder(syncedFolder, mediaFolder)
    }

    private fun createQualifyingMediaFolderItem(
        mediaFolder: MediaFolder,
        accountName: String
    ): SyncedFolderDisplayItem? {
        if (!SyncedFolderUtils.isQualifyingMediaFolder(mediaFolder)) {
            return null
        }
        return itemFactory.createAutoUploadFolderFromMediaFolder(mediaFolder, accountName)
    }

    private fun folderKey(path: String?, type: MediaFolderType?): String = "$path-$type"

    companion object {
        private val PRIORITIZED_FOLDERS = arrayOf("Camera", "Screenshots")

        @JvmStatic
        fun sortAutoUploadFolderItems(
            syncFolderItemList: List<SyncedFolderDisplayItem?>
        ): List<SyncedFolderDisplayItem?> = syncFolderItemList.sortedWith(::compareItems)

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
            val prioritizedFolder = PRIORITIZED_FOLDERS.firstOrNull { it == name1 || it == name2 }
            return when {
                prioritizedFolder == null -> compareNames(name1, name2)
                prioritizedFolder == name1 && prioritizedFolder == name2 -> 0
                prioritizedFolder == name1 -> -1
                else -> 1
            }
        }

        private fun compareNames(name1: String, name2: String): Int =
            name1.lowercase(Locale.getDefault()).compareTo(name2.lowercase(Locale.getDefault()))
    }
}
