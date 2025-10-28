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
import androidx.room.Update
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
    fun deleteByAccountAndRemotePath(accountName: String, remotePath: String)

    @Query(
        "SELECT * FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta._ID + " = :id AND " +
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName " +
            "LIMIT 1"
    )
    fun getUploadById(id: Long, accountName: String): UploadEntity?

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
        "SELECT * FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME} " +
            "WHERE ${ProviderTableMeta.UPLOADS_ACCOUNT_NAME} = :accountName " +
            "AND ${ProviderTableMeta.UPLOADS_STATUS} = :status"
    )
    suspend fun getUploadsByStatus(accountName: String, status: Int): List<UploadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: UploadEntity): Long

    @Update
    fun update(entity: UploadEntity)
}
