/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.entity.model

import androidx.room.ColumnInfo
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

data class FolderMetadataUpdate(
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Long,
    @ColumnInfo(name = ProviderTableMeta.FILE_MODIFIED)
    val modified: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)
    val modifiedAtLastSyncForData: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PARENT)
    val parent: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_UPLOADED)
    val uploaded: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CREATION)
    val creation: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CONTENT_TYPE)
    val contentType: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CONTENT_LENGTH)
    val contentLength: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_NAME)
    val name: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PATH)
    val path: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PATH_DECRYPTED)
    val pathDecrypted: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ACCOUNT_OWNER)
    val accountOwner: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_IS_ENCRYPTED)
    val isEncrypted: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_IS_READ_ONLY)
    val isReadOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LAST_SYNC_DATE)
    val lastSyncDate: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA)
    val lastSyncDateForData: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG)
    val etag: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG_ON_SERVER)
    val etagOnServer: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHARED_VIA_LINK)
    val sharedViaLink: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHARED_WITH_SHAREE)
    val sharedWithSharee: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PERMISSIONS)
    val permissions: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_REMOTE_ID)
    val remoteId: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCAL_ID)
    val localId: Long,
    @ColumnInfo(name = ProviderTableMeta.FILE_FAVORITE)
    val favorite: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_HIDDEN)
    val hidden: Int?,
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
    @ColumnInfo(name = ProviderTableMeta.FILE_TAGS)
    val tags: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_RICH_WORKSPACE)
    val richWorkspace: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_INTERNAL_TWO_WAY_SYNC_TIMESTAMP)
    val internalTwoWaySync: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_INTERNAL_TWO_WAY_SYNC_RESULT)
    val internalTwoWaySyncResult: String?
)
