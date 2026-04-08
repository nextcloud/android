/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Jonas Mayer <jonas.a.mayer@gmx.net>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019-2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016-2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014 Luke Owncloud <owncloud@ohrt.org>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.utils.autoRename.AutoRename
import com.nextcloud.utils.extensions.isConflict
import com.owncloud.android.MainApp
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.theme.CapabilityUtils
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.Observable

@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "MagicNumber", "ReturnCount")
class UploadsStorageManager(
    private val currentAccountProvider: CurrentAccountProvider,
    private val contentResolver: ContentResolver
) : Observable() {

    private var capability: OCCapability? = null

    val uploadDao: UploadDao = NextcloudDatabase.instance().uploadDao()
    val fileSystemDao = NextcloudDatabase.instance().fileSystemDao()
    val syncedFolderDao = NextcloudDatabase.instance().syncedFolderDao()

    private fun initOCCapability() {
        try {
            this.capability = CapabilityUtils.getCapability(MainApp.getAppContext())
        } catch (e: RuntimeException) {
            Log_OC.e(TAG, "Failed to set OCCapability: Dependencies are not yet ready. $e")
        }
    }

    @Synchronized
    fun updateUpload(ocUpload: OCUpload): Int {
        val existingUpload = getUploadById(ocUpload.uploadId)
        if (existingUpload == null) {
            Log_OC.e(TAG, "Upload not found for ID: " + ocUpload.uploadId)
            return 0
        }

        if (existingUpload.accountName != ocUpload.accountName) {
            Log_OC.e(
                TAG,
                "Account mismatch for upload ID " + ocUpload.uploadId +
                    ": expected " + existingUpload.accountName +
                    ", got " + ocUpload.accountName
            )
            return 0
        }

        Log_OC.v(TAG, "Updating " + ocUpload.localPath + " with status=" + ocUpload.uploadStatus)

        val cv = ContentValues().apply {
            put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.localPath)
            put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.remotePath)
            put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.accountName)
            put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.uploadStatus.value)
            put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.lastResult.value)

            val uploadEndTimestamp = ocUpload.uploadEndTimestamp
            put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP_LONG, uploadEndTimestamp)
            put(ProviderTableMeta.UPLOADS_FILE_SIZE, ocUpload.fileSize)
            put(ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN, ocUpload.folderUnlockToken)
        }

        val result = contentResolver.update(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            cv,
            ProviderTableMeta._ID + "=? AND " + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=?",
            arrayOf<String>(ocUpload.uploadId.toString(), ocUpload.accountName)
        )

        Log_OC.d(TAG, "updateUpload returns with: " + result + " for file: " + ocUpload.localPath)

        if (result != SINGLE_RESULT) {
            Log_OC.e(TAG, "Failed to update item " + ocUpload.localPath + " into upload db.")
        } else {
            notifyObserversNow()
        }

        return result
    }

    private fun updateUploadInternal(
        c: Cursor,
        status: UploadStatus?,
        result: UploadResult?,
        remotePath: String?,
        localPath: String?
    ): Int {
        var r = 0
        while (c.moveToNext()) {
            val upload = createOCUploadFromCursor(c)

            val path = c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_LOCAL_PATH))
            Log_OC.v(
                TAG,
                (
                    "Updating " + path + " with status:" + status + " and result:" +
                        (result?.toString() ?: "null") + " (old:" +
                        upload.toFormattedString() + ')'
                    )
            )

            upload.setUploadStatus(status)
            upload.lastResult = result
            upload.remotePath = remotePath

            if (localPath != null) {
                upload.localPath = localPath
            }

            if (status == UploadStatus.UPLOAD_SUCCEEDED) {
                upload.uploadEndTimestamp = Calendar.getInstance().getTimeInMillis()
            }

            r = updateUpload(upload)
        }

        return r
    }

    private fun updateUploadStatus(
        id: Long,
        status: UploadStatus?,
        result: UploadResult?,
        remotePath: String?,
        localPath: String?
    ) {
        val c = contentResolver.query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            ProviderTableMeta._ID + "=?",
            arrayOf(id.toString()),
            null
        )

        if (c != null) {
            if (c.count != SINGLE_RESULT) {
                Log_OC.e(
                    TAG,
                    (
                        c.count.toString() + " items for id=" + id +
                            " available in UploadDb. Expected 1. Failed to update upload db."
                        )
                )
            } else {
                updateUploadInternal(c, status, result, remotePath, localPath)
            }
            c.close()
        } else {
            Log_OC.e(TAG, "Cursor is null")
        }
    }

    fun notifyObserversNow() {
        Log_OC.d(TAG, "notifying upload storage manager observers")
        Handler(Looper.getMainLooper()).post {
            setChanged()
            notifyObservers()
        }
    }

    fun removeUpload(upload: OCUpload?): Int = if (upload == null) 0 else removeUpload(upload.uploadId)

    fun removeUpload(id: Long): Int {
        val result = contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta._ID + "=?",
            arrayOf(id.toString())
        )
        Log_OC.d(TAG, "delete returns $result for upload with id $id")
        if (result > 0) {
            notifyObserversNow()
        }
        return result
    }

    private fun removeUpload(accountName: String?, remotePath: String?): Int {
        val result = contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=? AND " + ProviderTableMeta.UPLOADS_REMOTE_PATH + "=?",
            arrayOf(accountName, remotePath)
        )
        Log_OC.d(TAG, "delete returns $result for file $remotePath in $accountName")
        if (result > 0) {
            notifyObserversNow()
        }
        return result
    }

    fun removeUploads(accountName: String?): Int {
        val result = contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=?",
            arrayOf(accountName)
        )
        Log_OC.d(TAG, "delete returns $result for uploads in $accountName")
        if (result > 0) {
            notifyObserversNow()
        }
        return result
    }

    fun getAllStoredUploads(): Array<OCUpload> = getUploads(null)

    fun getUploadById(id: Long): OCUpload? {
        var result: OCUpload? = null
        val cursor = contentResolver.query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            ProviderTableMeta._ID + "=?",
            arrayOf(id.toString()),
            "_id ASC"
        )

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = createOCUploadFromCursor(cursor)
            }
        }
        Log_OC.d(TAG, "Retrieve job $result for id $id")
        return result
    }

    fun getUploadsByIds(uploadIds: LongArray, accountName: String): List<OCUpload> {
        val result = ArrayList<OCUpload>()
        uploadDao.getUploadsByIds(uploadIds, accountName).forEach { entity ->
            createOCUploadFromEntity(entity)?.let { result.add(it) }
        }
        return result
    }

    private fun getUploads(selection: String?, vararg selectionArgs: String?): Array<OCUpload> {
        val uploads = ArrayList<OCUpload>()
        var page: Long = 0
        var rowsRead: Long
        var rowsTotal: Long = 0
        var lastRowID: Long = -1

        do {
            val uploadsPage = getUploadPage(lastRowID, selection, *selectionArgs)
            rowsRead = uploadsPage.size.toLong()
            rowsTotal += rowsRead
            if (uploadsPage.isNotEmpty()) {
                lastRowID = uploadsPage.last().uploadId
            }
            Log_OC.v(
                TAG,
                String.format(
                    Locale.ENGLISH,
                    "getUploads() got %d rows from page %d, %d rows total so far, last ID %d",
                    rowsRead,
                    page,
                    rowsTotal,
                    lastRowID
                )
            )
            uploads.addAll(uploadsPage)
            page++
        } while (rowsRead > 0)

        Log_OC.v(
            TAG,
            String.format(
                Locale.ENGLISH,
                "getUploads() returning %d (%d) rows after reading %d pages",
                rowsTotal,
                uploads.size,
                page
            )
        )

        return uploads.toTypedArray<OCUpload>()
    }

    private fun getUploadPage(afterId: Long, selection: String?, vararg selectionArgs: String?): List<OCUpload> =
        getUploadPage(QUERY_PAGE_SIZE, afterId, true, selection, *selectionArgs)

    private fun getUploadPage(
        limit: Long,
        afterId: Long,
        descending: Boolean,
        selection: String?,
        vararg selectionArgs: String?
    ): List<OCUpload> {
        val uploads = ArrayList<OCUpload>()
        val (sortDirection, idComparator) = if (descending) "DESC" to "<" else "ASC" to ">"
        val pageSelection: String?
        val pageSelectionArgs: Array<String?>

        if (afterId >= 0) {
            pageSelection = if (selection != null) "($selection) AND _id $idComparator ?" else "_id $idComparator ?"
            pageSelectionArgs = arrayOfNulls<String>(selectionArgs.size + 1).also { arr ->
                selectionArgs.forEachIndexed { i, v -> arr[i] = v }
                arr[selectionArgs.size] = afterId.toString()
            }
            Log_OC.d(TAG, String.format(Locale.ENGLISH, "QUERY: %s ROWID: %d", pageSelection, afterId))
        } else {
            pageSelection = selection
            pageSelectionArgs = arrayOfNulls<String>(selectionArgs.size).also { arr ->
                selectionArgs.forEachIndexed { i, v -> arr[i] = v }
            }
            Log_OC.d(TAG, String.format(Locale.ENGLISH, "QUERY: %s ROWID: %d", selection, afterId))
        }

        val sortOrder = if (limit > 0) {
            String.format(Locale.ENGLISH, "_id $sortDirection LIMIT %d", limit)
        } else {
            "_id $sortDirection"
        }

        contentResolver.query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            pageSelection,
            pageSelectionArgs,
            sortOrder
        )?.use { c ->
            if (c.moveToFirst()) {
                do {
                    uploads.add(createOCUploadFromCursor(c))
                } while (c.moveToNext() && !c.isAfterLast)
            }
        }

        return uploads
    }

    private fun createOCUploadFromEntity(entity: UploadEntity?): OCUpload? {
        if (entity == null) return null
        initOCCapability()
        return entity.toOCUpload(capability)
    }

    private fun createOCUploadFromCursor(c: Cursor): OCUpload {
        initOCCapability()

        fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col))
        fun Cursor.int(col: String): Int = getInt(getColumnIndexOrThrow(col))
        fun Cursor.long(col: String): Long = getLong(getColumnIndexOrThrow(col))

        var remotePath = c.str(ProviderTableMeta.UPLOADS_REMOTE_PATH)
        if (capability != null) {
            remotePath = AutoRename.rename(remotePath, capability!!)
        }

        return OCUpload(
            c.str(ProviderTableMeta.UPLOADS_LOCAL_PATH),
            remotePath,
            c.str(ProviderTableMeta.UPLOADS_ACCOUNT_NAME)
        ).apply {
            fileSize = c.long(ProviderTableMeta.UPLOADS_FILE_SIZE)
            uploadId = c.long(ProviderTableMeta._ID)
            setUploadStatus(UploadStatus.fromValue(c.int(ProviderTableMeta.UPLOADS_STATUS)))
            localAction = c.int(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR)
            nameCollisionPolicy =
                NameCollisionPolicy.deserialize(c.int(ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY))
            isCreateRemoteFolder = c.int(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER) == 1

            val timestampIndex = c.getColumnIndex(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP_LONG)
            if (timestampIndex > -1) {
                val ts = c.getLong(timestampIndex)
                if (ts > 0) uploadEndTimestamp = ts
            }

            lastResult = UploadResult.fromValue(c.int(ProviderTableMeta.UPLOADS_LAST_RESULT))
            createdBy = c.int(ProviderTableMeta.UPLOADS_CREATED_BY)
            isUseWifiOnly = c.int(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY) == 1
            isWhileChargingOnly = c.int(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY) == 1
            folderUnlockToken = c.str(ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN)
        }
    }

    fun getCurrentUploadIds(accountName: String): LongArray =
        uploadDao.getAllIds(UploadStatus.UPLOAD_IN_PROGRESS.value, accountName)
            .stream()
            .mapToLong { it.toLong() }
            .toArray()

    fun getUploadsForAccount(accountName: String): Array<OCUpload> =
        getUploads(ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL, accountName)

    fun clearFailedButNotDelayedUploads() {
        val user = currentAccountProvider.user
        val deleted = contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_FAILED.value +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT + ANGLE_BRACKETS + UploadResult.LOCK_FAILED.value +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT + ANGLE_BRACKETS + UploadResult.DELAYED_FOR_WIFI.value +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT + ANGLE_BRACKETS + UploadResult.DELAYED_FOR_CHARGING.value +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT + ANGLE_BRACKETS +
                UploadResult.DELAYED_IN_POWER_SAVE_MODE.value +
                AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL,
            arrayOf(user.accountName)
        )

        Log_OC.d(TAG, "delete all failed uploads but those delayed for Wifi")

        if (deleted > 0) notifyObserversNow()
    }

    fun clearCancelledUploadsForCurrentAccount() {
        val user = currentAccountProvider.user
        val deleted = contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_CANCELLED.value +
                AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL,
            arrayOf(user.accountName)
        )
        Log_OC.d(TAG, "delete all cancelled uploads")
        if (deleted > 0) notifyObserversNow()
    }

    fun clearSuccessfulUploads() {
        val user = currentAccountProvider.user
        val deleted = contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_SUCCEEDED.value +
                AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL,
            arrayOf(user.accountName)
        )
        Log_OC.d(TAG, "delete all successful uploads")
        if (deleted > 0) notifyObserversNow()
    }

    fun updateDatabaseUploadResult(uploadResult: RemoteOperationResult<*>, upload: UploadFileOperation) {
        Log_OC.d(TAG, "updateDatabaseUploadResult uploadResult: $uploadResult upload: $upload")

        if (uploadResult.isCancelled) {
            Log_OC.w(TAG, "upload is cancelled, removing upload")
            removeUpload(upload.user.accountName, upload.remotePath)
            return
        }

        val localPath =
            if (upload.localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_MOVE) upload.storagePath else null

        Log_OC.d(TAG, "local behaviour: " + upload.localBehaviour)
        Log_OC.d(TAG, "local path of upload: $localPath")

        var status = UploadStatus.UPLOAD_FAILED
        var result = UploadResult.fromOperationResult(uploadResult)
        val code = uploadResult.code

        if (uploadResult.isSuccess) {
            status = UploadStatus.UPLOAD_SUCCEEDED
            result = UploadResult.UPLOADED
        } else if (code.isConflict()) {
            val isSame = FileUploadHelper().isSameFileOnRemote(
                upload.user,
                File(upload.storagePath),
                upload.remotePath,
                upload.context
            )

            if (isSame) {
                result = UploadResult.SAME_FILE_CONFLICT
                status = UploadStatus.UPLOAD_SUCCEEDED
            } else {
                result = UploadResult.SYNC_CONFLICT
            }
        } else if (code == RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND) {
            // upload status is SUCCEEDED because user cannot take action about it, it will always fail
            status = UploadStatus.UPLOAD_SUCCEEDED
            result = UploadResult.FILE_NOT_FOUND
        }

        Log_OC.d(
            TAG,
            String.format(
                "Upload Finished [%s] | RemoteCode: %s | internalResult: %s | FinalStatus: %s | Path: %s",
                if (uploadResult.isSuccess) "✅" else "❌",
                code,
                result.name,
                status,
                upload.remotePath
            )
        )

        updateUploadStatus(upload.ocUploadId, status, result, upload.remotePath, localPath)
    }

    fun updateDatabaseUploadStart(upload: UploadFileOperation) {
        val localPath =
            if (FileUploadWorker.LOCAL_BEHAVIOUR_MOVE == upload.localBehaviour) upload.storagePath else null

        updateUploadStatus(
            upload.ocUploadId,
            UploadStatus.UPLOAD_IN_PROGRESS,
            UploadResult.UNKNOWN,
            upload.remotePath,
            localPath
        )
    }

    @VisibleForTesting
    fun removeAllUploads() {
        Log_OC.v(TAG, "Delete all uploads!")
        contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            "",
            arrayOf<String?>()
        )
    }

    fun removeUserUploads(user: User): Int {
        Log_OC.v(TAG, "Delete all uploads for account " + user.accountName)
        return contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=?",
            arrayOf(user.accountName)
        )
    }

    enum class UploadStatus(val value: Int) {
        /**
         * Upload currently in progress or scheduled to be executed.
         */
        UPLOAD_IN_PROGRESS(0),

        /**
         * Last upload failed.
         */
        UPLOAD_FAILED(1),

        /**
         * Upload was successful.
         */
        UPLOAD_SUCCEEDED(2),

        /**
         * Upload was cancelled by the user.
         */
        UPLOAD_CANCELLED(3);

        companion object {
            fun fromValue(value: Int): UploadStatus? = when (value) {
                0 -> UPLOAD_IN_PROGRESS
                1 -> UPLOAD_FAILED
                2 -> UPLOAD_SUCCEEDED
                3 -> UPLOAD_CANCELLED
                else -> null
            }
        }
    }

    companion object {
        private val TAG: String = UploadsStorageManager::class.java.getSimpleName()

        private const val IS_EQUAL = "== ?"
        private const val EQUAL = "=="
        private const val OR = " OR "
        private const val AND = " AND "
        private const val ANGLE_BRACKETS = "<>"
        private const val SINGLE_RESULT = 1

        private const val QUERY_PAGE_SIZE: Long = 100
    }
}
