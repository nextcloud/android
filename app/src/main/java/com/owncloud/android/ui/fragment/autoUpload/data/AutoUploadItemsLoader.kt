/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.data

import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.utils.extensions.sortAutoUploadFolderItems
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.MediaProvider
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.SyncedFolderUtils
import java.io.File

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
        return items.sortAutoUploadFolderItems().filterNotNull()
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
}
