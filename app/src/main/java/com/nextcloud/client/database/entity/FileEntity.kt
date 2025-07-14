/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Entity(tableName = ProviderTableMeta.FILE_TABLE_NAME)
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_NAME)
    val name: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ENCRYPTED_NAME)
    val encryptedName: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PATH)
    val path: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PATH_DECRYPTED)
    val pathDecrypted: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PARENT)
    val parent: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CREATION)
    val creation: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_MODIFIED)
    val modified: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CONTENT_TYPE)
    val contentType: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CONTENT_LENGTH)
    val contentLength: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_STORAGE_PATH)
    val storagePath: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ACCOUNT_OWNER)
    val accountOwner: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LAST_SYNC_DATE)
    val lastSyncDate: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA)
    val lastSyncDateForData: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)
    val modifiedAtLastSyncForData: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG)
    val etag: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG_ON_SERVER)
    val etagOnServer: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHARED_VIA_LINK)
    val sharedViaLink: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PERMISSIONS)
    val permissions: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_REMOTE_ID)
    val remoteId: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCAL_ID, defaultValue = "-1")
    val localId: Long,
    @ColumnInfo(name = ProviderTableMeta.FILE_UPDATE_THUMBNAIL)
    val updateThumbnail: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_IS_DOWNLOADING)
    val isDownloading: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_FAVORITE)
    val favorite: Int?,

    @ColumnInfo(name = ProviderTableMeta.FILE_HIDDEN)
    val hidden: Int?,

    @ColumnInfo(name = ProviderTableMeta.FILE_IS_ENCRYPTED)
    val isEncrypted: Int?,

    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG_IN_CONFLICT)
    val etagInConflict: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHARED_WITH_SHAREE)
    var sharedWithSharee: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_MOUNT_TYPE)
    val mountType: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_HAS_PREVIEW)
    val hasPreview: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT)
    val unreadCommentsCount: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_OWNER_ID)
    val ownerId: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_OWNER_DISPLAY_NAME)
    val ownerDisplayName: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_NOTE)
    val note: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHAREES)
    val sharees: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_RICH_WORKSPACE)
    val richWorkspace: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_METADATA_SIZE)
    val metadataSize: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_METADATA_LIVE_PHOTO)
    val metadataLivePhoto: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCKED)
    val locked: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_TYPE)
    val lockType: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_OWNER)
    val lockOwner: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_OWNER_DISPLAY_NAME)
    val lockOwnerDisplayName: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_OWNER_EDITOR)
    val lockOwnerEditor: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_TIMESTAMP)
    val lockTimestamp: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_TIMEOUT)
    val lockTimeout: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCK_TOKEN)
    val lockToken: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_TAGS)
    val tags: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_METADATA_GPS)
    val metadataGPS: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_E2E_COUNTER)
    val e2eCounter: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_INTERNAL_TWO_WAY_SYNC_TIMESTAMP)
    val internalTwoWaySync: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_INTERNAL_TWO_WAY_SYNC_RESULT)
    val internalTwoWaySyncResult: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_UPLOADED)
    val uploaded: Long?
)
