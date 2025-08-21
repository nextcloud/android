/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextcloud.android.lib.resources.recommendations.Recommendation
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Entity(tableName = ProviderTableMeta.RECOMMENDED_FILE_TABLE_NAME)
data class RecommendedFileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Long?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_FILE_NAME)
    val name: String?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_FILE_DIRECTORY)
    val directory: String?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_FILE_EXTENSIONS)
    val extension: String?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_FILE_MIME_TYPE)
    val mimeType: String?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_FILE_HAS_PREVIEW)
    val hasPreview: Boolean?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_FILE_REASON)
    val reason: String?,

    @ColumnInfo(name = ProviderTableMeta.RECOMMENDED_TIMESTAMP)
    val timestamp: Long?
) {
    val decryptedRemotePath: String
        get() = directory + OCFile.PATH_SEPARATOR + name
}

fun ArrayList<Recommendation>.toEntity(): List<RecommendedFileEntity> = this.map { recommendation ->
    RecommendedFileEntity(
        id = recommendation.id,
        name = recommendation.name,
        directory = recommendation.directory,
        extension = recommendation.extension,
        mimeType = recommendation.mimeType,
        hasPreview = recommendation.hasPreview,
        reason = recommendation.reason,
        timestamp = recommendation.timestamp
    )
}
