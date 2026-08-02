/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nextcloud.client.database.entity.UploadEntity
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
interface UploadDao {
    @Query(
        "SELECT _id FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta.UPLOADS_STATUS + " = :status AND " +
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName AND _id IS NOT NULL"
    )
    fun getAllIds(status: Int, accountName: String): List<Int>

    @Query(
        "SELECT * FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta._ID + " IN (:ids) AND " +
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName"
    )
    fun getUploadsByIds(ids: LongArray, accountName: String): List<UploadEntity>

    @Query(
        "SELECT * FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME} " +
            "WHERE ${ProviderTableMeta.UPLOADS_REMOTE_PATH} = :remotePath LIMIT 1"
    )
    fun getByRemotePath(remotePath: String): UploadEntity?

    @Query(
        "DELETE FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME} " +
            "WHERE ${ProviderTableMeta.UPLOADS_ACCOUNT_NAME} = :accountName " +
            "AND ${ProviderTableMeta.UPLOADS_REMOTE_PATH} = :remotePath"
    )
    fun deleteByRemotePathAndAccountName(remotePath: String, accountName: String)

    @Query(
        """
    DELETE FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME}
    WHERE ${ProviderTableMeta.UPLOADS_LOCAL_PATH} = :localPath
      AND ${ProviderTableMeta.UPLOADS_REMOTE_PATH} = :remotePath
"""
    )
    suspend fun deleteByLocalRemotePath(localPath: String, remotePath: String)

    @Query(
        "SELECT * FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta._ID + " = :id AND " +
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName " +
            "LIMIT 1"
    )
    fun getUploadById(id: Long, accountName: String): UploadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(entity: UploadEntity): Long

    @Query(
        "SELECT * FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName AND " +
            ProviderTableMeta.UPLOADS_LOCAL_PATH + " = :localPath AND " +
            ProviderTableMeta.UPLOADS_REMOTE_PATH + " = :remotePath " +
            "LIMIT 1"
    )
    fun getUploadByAccountAndPaths(accountName: String, localPath: String, remotePath: String): UploadEntity?

    @Query(
        "UPDATE ${ProviderTableMeta.UPLOADS_TABLE_NAME} " +
            "SET ${ProviderTableMeta.UPLOADS_STATUS} = :status " +
            "WHERE ${ProviderTableMeta.UPLOADS_REMOTE_PATH} = :remotePath " +
            "AND ${ProviderTableMeta.UPLOADS_ACCOUNT_NAME} = :accountName"
    )
    suspend fun updateStatus(remotePath: String, accountName: String, status: Int): Int

    @Query(
        """
    UPDATE ${ProviderTableMeta.UPLOADS_TABLE_NAME}
    SET ${ProviderTableMeta.UPLOADS_STATUS} = :status
    WHERE ${ProviderTableMeta.UPLOADS_ACCOUNT_NAME} = :accountName
      AND ${ProviderTableMeta.UPLOADS_REMOTE_PATH} IN (:remotePaths)
"""
    )
    suspend fun updateStatuses(remotePaths: List<String>, accountName: String, status: Int): Int

    @Query(
        """
    SELECT * FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME}
    WHERE ${ProviderTableMeta.UPLOADS_STATUS} = :status
      AND (:nameCollisionPolicy IS NULL OR ${ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY} = :nameCollisionPolicy)
"""
    )
    suspend fun getUploadsByStatus(status: Int, nameCollisionPolicy: Int? = null): List<UploadEntity>

    @Query(
        """
    SELECT * FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME}
    WHERE ${ProviderTableMeta.UPLOADS_ACCOUNT_NAME} = :accountName
      AND ${ProviderTableMeta.UPLOADS_STATUS} = :status
      AND (:nameCollisionPolicy IS NULL OR ${ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY} = :nameCollisionPolicy)
"""
    )
    suspend fun getUploadsByAccountNameAndStatus(
        accountName: String,
        status: Int,
        nameCollisionPolicy: Int? = null
    ): List<UploadEntity>

    /**
     * Drops every upload record below the given local folder that did not succeed. Auto-upload refuses to retry a file
     * whose last result is non-retryable, and a record left behind in progress by a killed worker keeps its default
     * [com.owncloud.android.db.UploadResult.UNKNOWN] result, which counts as non-retryable as well. A manual rescan
     * therefore forgets these records so the files get a fresh attempt. Successful uploads are kept so a rescan never
     * uploads the same file twice.
     */
    @Query(
        """
    DELETE FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME}
    WHERE ${ProviderTableMeta.UPLOADS_ACCOUNT_NAME} = :accountName
      AND ${ProviderTableMeta.UPLOADS_STATUS} != :succeededStatus
      AND ${ProviderTableMeta.UPLOADS_LOCAL_PATH} LIKE :localPathPrefix || '%'
"""
    )
    suspend fun deleteUnsuccessfulUploadsInFolder(
        accountName: String,
        localPathPrefix: String,
        succeededStatus: Int
    ): Int
}
