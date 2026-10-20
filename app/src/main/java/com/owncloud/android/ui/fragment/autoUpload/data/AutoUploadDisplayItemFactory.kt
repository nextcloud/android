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

import com.nextcloud.client.core.Clock
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.utils.SyncedFolderUtils
import java.io.File

class AutoUploadDisplayItemFactory(
    private val clock: Clock,
    private val itemsDisplayedPerFolder: Int,
    private val instantUploadPath: String
) {

    fun createAutoUploadFolderWithoutMediaFolder(syncedFolder: SyncedFolder): SyncedFolderDisplayItem {
        val localFolder = File(syncedFolder.localPath)
        val files = SyncedFolderUtils.getFileList(localFolder)
        return createFromAutoUploadFolder(
            syncedFolder,
            filePaths = getDisplayFilePaths(files),
            folderName = localFolder.name,
            numberOfFiles = files.size.toLong(),
            type = syncedFolder.type
        )
    }

    fun createAutoUploadFolder(syncedFolder: SyncedFolder, mediaFolder: MediaFolder): SyncedFolderDisplayItem =
        createFromAutoUploadFolder(
            syncedFolder,
            filePaths = mediaFolder.filePaths,
            folderName = mediaFolder.folderName,
            numberOfFiles = mediaFolder.numberOfFiles,
            type = mediaFolder.type
        )

    fun createAutoUploadFolderFromMediaFolder(mediaFolder: MediaFolder, accountName: String): SyncedFolderDisplayItem =
        SyncedFolderDisplayItem(
            SyncedFolder.UNPERSISTED_ID,
            mediaFolder.absolutePath,
            "$instantUploadPath/${mediaFolder.folderName}",
            true,
            false,
            true,
            false,
            accountName,
            FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
            NameCollisionPolicy.ASK_USER.serialize(),
            false,
            clock.currentTime,
            mediaFolder.filePaths,
            mediaFolder.folderName,
            mediaFolder.numberOfFiles,
            mediaFolder.type,
            false,
            SubFolderRule.YEAR_MONTH,
            false,
            SyncedFolder.NOT_SCANNED_YET
        )

    fun createEmptyCustomFolder(accountName: String): SyncedFolderDisplayItem = SyncedFolderDisplayItem(
        SyncedFolder.UNPERSISTED_ID,
        null,
        null,
        true,
        false,
        true,
        false,
        accountName,
        FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
        NameCollisionPolicy.ASK_USER.serialize(),
        false,
        clock.currentTime,
        null,
        MediaFolderType.CUSTOM,
        false,
        SubFolderRule.YEAR_MONTH,
        false,
        SyncedFolder.NOT_SCANNED_YET
    )

    fun createNewCustomFolder(syncedFolder: SyncedFolderParcelable): SyncedFolderDisplayItem = SyncedFolderDisplayItem(
        SyncedFolder.UNPERSISTED_ID,
        syncedFolder.localPath,
        syncedFolder.remotePath,
        syncedFolder.isWifiOnly,
        syncedFolder.isChargingOnly,
        syncedFolder.isExisting,
        syncedFolder.isSubfolderByDate,
        syncedFolder.account,
        syncedFolder.uploadAction,
        syncedFolder.nameCollisionPolicy.serialize(),
        syncedFolder.isEnabled,
        clock.currentTime,
        File(syncedFolder.localPath).name,
        syncedFolder.type,
        syncedFolder.isHidden,
        syncedFolder.subFolderRule,
        syncedFolder.isExcludeHidden,
        SyncedFolder.NOT_SCANNED_YET
    )

    fun updateAutoUploadFolderItem(item: SyncedFolderDisplayItem, syncedFolder: SyncedFolderParcelable) {
        item.apply {
            id = syncedFolder.id
            localPath = syncedFolder.localPath
            remotePath = syncedFolder.remotePath
            isWifiOnly = syncedFolder.isWifiOnly
            isChargingOnly = syncedFolder.isChargingOnly
            setExisting(syncedFolder.isExisting)
            isSubfolderByDate = syncedFolder.isSubfolderByDate
            uploadAction = syncedFolder.uploadAction
            setNameCollisionPolicy(syncedFolder.nameCollisionPolicy.serialize())
            setEnabled(syncedFolder.isEnabled, clock.currentTime)
            setSubFolderRule(syncedFolder.subFolderRule)
            setExcludeHidden(syncedFolder.isExcludeHidden)
        }
    }

    @Suppress("LongParameterList")
    private fun createFromAutoUploadFolder(
        syncedFolder: SyncedFolder,
        filePaths: List<String>?,
        folderName: String?,
        numberOfFiles: Long,
        type: MediaFolderType?
    ): SyncedFolderDisplayItem = SyncedFolderDisplayItem(
        syncedFolder.id,
        syncedFolder.localPath,
        syncedFolder.remotePath,
        syncedFolder.isWifiOnly,
        syncedFolder.isChargingOnly,
        syncedFolder.alsoUploadExistingFiles(),
        syncedFolder.isSubfolderByDate,
        syncedFolder.account,
        syncedFolder.uploadAction,
        syncedFolder.nameCollisionPolicyInt,
        syncedFolder.isEnabled,
        clock.currentTime,
        filePaths,
        folderName,
        numberOfFiles,
        type,
        syncedFolder.isHidden,
        syncedFolder.subfolderRule,
        syncedFolder.isExcludeHidden,
        syncedFolder.lastScanTimestampMs
    )

    private fun getDisplayFilePaths(files: List<File>): List<String>? =
        files.take(itemsDisplayedPerFolder).map { it.absolutePath }.ifEmpty { null }
}
