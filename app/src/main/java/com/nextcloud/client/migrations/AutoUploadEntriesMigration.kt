/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.migrations

import android.content.Context
import android.view.WindowManager.BadTokenException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.R
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.MediaProvider
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen

object AutoUploadEntriesMigration {

    private val TAG: String = AutoUploadEntriesMigration::class.java.simpleName
    private const val MEDIA_FOLDER_ITEM_LIMIT = 1

    fun updateToAutoUpload(context: Context) {
        val preferences = AppPreferencesImpl.fromContext(context)
        if (!preferences.instantPictureUploadEnabled() && !preferences.instantVideoUploadEnabled()) {
            return
        }

        preferences.removeLegacyPreferences()

        try {
            showAutoUploadAlertDialog(context)
        } catch (e: BadTokenException) {
            Log_OC.i(TAG, "Error showing Auto Upload Update dialog, so skipping it: " + e.message)
        }
    }

    private fun showAutoUploadAlertDialog(context: Context) {
        MaterialAlertDialogBuilder(context, R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.drawer_synced_folders)
            .setMessage(R.string.synced_folders_new_info)
            .setPositiveButton(R.string.drawer_open) { dialog, _ ->
                val folderSyncIntent = NavigatorActivity.intent(context, NavigatorScreen.AutoUpload)
                dialog.dismiss()
                context.startActivity(folderSyncIntent)
            }
            .setNegativeButton(R.string.drawer_close) { dialog, _ -> dialog.dismiss() }
            .setIcon(R.drawable.nav_synced_folders)
            .create()
            .show()
    }

    fun cleanOldEntries(context: Context, clock: Clock) {
        val preferences = AppPreferencesImpl.fromContext(context)
        if (preferences.isLegacyClean) {
            return
        }

        val syncedFolderProvider = SyncedFolderProvider(context.contentResolver, preferences, clock)
        val newestIds = syncedFolderProvider.syncedFolders
            .groupBy { it.account to it.localPath }
            .values
            .map { duplicates -> duplicates.maxOf { it.id } }

        if (newestIds.isEmpty()) {
            preferences.isLegacyClean = true
            return
        }

        val deletedCount = syncedFolderProvider.deleteSyncedFoldersNotInList(newestIds.toMutableList<Long?>())
        if (deletedCount > 0) {
            preferences.isLegacyClean = true
        }
    }

    fun updateAutoUploadEntries(context: Context, clock: Clock) {
        val preferences = AppPreferencesImpl.fromContext(context)
        if (preferences.isAutoUploadPathsUpdateEnabled) {
            return
        }

        SyncedFolderProvider(context.contentResolver, preferences, clock).updateAutoUploadPaths(context)
    }

    fun splitOutAutoUploadEntries(context: Context, clock: Clock) {
        val preferences = AppPreferencesImpl.fromContext(context)
        if (preferences.isAutoUploadSplitEntriesEnabled) {
            return
        }

        Log_OC.i(TAG, "Migrate synced_folders records for image/video split")
        val contentResolver = context.contentResolver
        val syncedFolderProvider = SyncedFolderProvider(contentResolver, preferences, clock)
        val imageMediaFolders = MediaProvider.getImageFolders(contentResolver, MEDIA_FOLDER_ITEM_LIMIT, null, true)
        val videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, MEDIA_FOLDER_ITEM_LIMIT, null, true)
        val syncedFolders = syncedFolderProvider.syncedFolders

        syncedFolders.forEach { syncedFolder ->
            Log_OC.i(TAG, "Migration check for synced_folders record: ${syncedFolder.id} - ${syncedFolder.localPath}")
            syncedFolderProvider.storeCopyIfMediaFolder(syncedFolder, imageMediaFolders, MediaFolderType.IMAGE, "image")
            syncedFolderProvider.storeCopyIfMediaFolder(syncedFolder, videoMediaFolders, MediaFolderType.VIDEO, "video")
        }

        syncedFolders.forEach { syncedFolder ->
            Log_OC.i(TAG, "Removing legacy synced_folders record: ${syncedFolder.id}")
            syncedFolderProvider.deleteSyncedFolder(syncedFolder.id)
        }

        preferences.isAutoUploadSplitEntriesEnabled = true
    }

    private fun SyncedFolderProvider.storeCopyIfMediaFolder(
        syncedFolder: SyncedFolder,
        mediaFolders: List<MediaFolder>,
        mediaType: MediaFolderType,
        mediaTypeLabel: String
    ) {
        val isMediaFolder = mediaFolders.any { it.absolutePath != null && it.absolutePath == syncedFolder.localPath }
        if (!isMediaFolder) {
            return
        }

        val newSyncedFolder = (syncedFolder.clone() as SyncedFolder).apply { setType(mediaType) }
        val primaryKey = storeSyncedFolder(newSyncedFolder)
        Log_OC.i(TAG, "Migrated $mediaTypeLabel synced_folders record: $primaryKey - ${newSyncedFolder.localPath}")
    }
}
