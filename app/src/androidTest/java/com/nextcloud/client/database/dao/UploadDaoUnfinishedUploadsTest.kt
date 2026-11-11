/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.database.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.entity.toUploadEntity
import com.nextcloud.client.database.typeConverter.OfflineOperationTypeConverter
import com.nextcloud.utils.extensions.isNonRetryable
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadDaoUnfinishedUploadsTest {

    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        NextcloudDatabase::class.java
    )
        .addTypeConverter(OfflineOperationTypeConverter())
        .allowMainThreadQueries()
        .build()
    private val uploadDao = database.uploadDao()
    private val nonRetryableResults = UploadResult.entries.filter { it.isNonRetryable() }.map { it.value }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun uploadIsUnfinishedWhileTheUploadJobStillProcessesIt() {
        val cases = listOf(
            Triple(UploadStatus.UPLOAD_IN_PROGRESS, UploadResult.UNKNOWN, true),
            Triple(UploadStatus.UPLOAD_FAILED, UploadResult.NETWORK_CONNECTION, true),
            Triple(UploadStatus.UPLOAD_FAILED, null, true),
            Triple(UploadStatus.UPLOAD_FAILED, UploadResult.FILE_NOT_FOUND, false),
            Triple(UploadStatus.UPLOAD_SUCCEEDED, UploadResult.UPLOADED, false)
        )

        cases.forEachIndexed { index, (status, result, unfinished) ->
            val remotePath = "$FILE_PATH.$index"
            insert(remotePath, status, result)

            assertEquals("$status $result", unfinished, isFileUnfinished(remotePath))
        }
    }

    @Test
    fun uploadOfOtherAccountOrPathIsIgnored() {
        insert(FILE_PATH, UploadStatus.UPLOAD_IN_PROGRESS, accountName = OTHER_ACCOUNT_NAME)
        insert("$FILE_PATH.bak", UploadStatus.UPLOAD_IN_PROGRESS)

        assertFalse(isFileUnfinished())
    }

    @Test
    fun unfinishedUploadInsideFolderIsUnfinished() {
        insert("${FOLDER_PATH}sub/document.bin", UploadStatus.UPLOAD_FAILED, UploadResult.NETWORK_CONNECTION)

        assertTrue(isFolderUnfinished())
    }

    @Test
    fun folderIgnoresFinishedChildrenAndSiblingWithSamePrefix() {
        insert("${FOLDER_PATH}document.bin", UploadStatus.UPLOAD_FAILED, UploadResult.FILE_NOT_FOUND)
        insert("${FOLDER_PATH.trimEnd('/')}-other/document.bin", UploadStatus.UPLOAD_IN_PROGRESS)

        assertFalse(isFolderUnfinished())
    }

    private fun insert(
        remotePath: String,
        status: UploadStatus,
        result: UploadResult? = UploadResult.UNKNOWN,
        accountName: String = ACCOUNT_NAME
    ) {
        val upload = OCUpload(LOCAL_PATH, remotePath, accountName).apply { uploadStatus = status }
        uploadDao.insertOrReplace(upload.toUploadEntity().copy(lastResult = result?.value))
    }

    private fun isFileUnfinished(remotePath: String = FILE_PATH): Boolean =
        uploadDao.existsUnfinishedByAccountAndRemotePath(
            ACCOUNT_NAME,
            remotePath,
            UploadStatus.UPLOAD_IN_PROGRESS.value,
            UploadStatus.UPLOAD_FAILED.value,
            nonRetryableResults
        )

    private fun isFolderUnfinished(): Boolean = uploadDao.existsUnfinishedByAccountInFolder(
        ACCOUNT_NAME,
        FOLDER_PATH,
        UploadStatus.UPLOAD_IN_PROGRESS.value,
        UploadStatus.UPLOAD_FAILED.value,
        nonRetryableResults
    )

    companion object {
        private const val ACCOUNT_NAME = "user@server"
        private const val OTHER_ACCOUNT_NAME = "other@server"
        private const val LOCAL_PATH = "/local/document.bin"
        private const val FILE_PATH = "/folder/document.bin"
        private const val FOLDER_PATH = "/snapshot/"
    }
}
