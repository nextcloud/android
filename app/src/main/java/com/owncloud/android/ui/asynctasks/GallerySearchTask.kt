/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.asynctasks

import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.owncloud.android.BuildConfig
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GallerySearchTask(
    private val fragment: GalleryFragment,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val endDate: Long,
    private val limit: Int
) {
    companion object {
        private const val NO_TIMESTAMP = -1L
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private const val MILLIS_PER_SECOND = 1000L
    }

    fun execute(): Job = fragment.lifecycleScope.launch(Dispatchers.IO) {
        if (!isActive) return@launch

        val context = fragment.context ?: return@launch
        val result = performSearch(context)

        withContext(Dispatchers.Main) {
            fragment.searchCompleted(result.emptySearch, result.lastTimestamp)
        }
    }

    @Suppress("DEPRECATION")
    private fun performSearch(context: android.content.Context): Result {
        val ocCapability = storageManager.getCapability(user.accountName)
        val searchOperation = buildSearchOperation(ocCapability)

        logSearchStart()

        val operationResult = searchOperation.execute(user, context)
        return if (operationResult.isSuccess) {
            handleSuccess(operationResult)
        } else {
            Result(false, false, NO_TIMESTAMP)
        }
    }

    private fun buildSearchOperation(
        ocCapability: com.owncloud.android.lib.resources.status.OCCapability
    ): SearchRemoteOperation =
        SearchRemoteOperation("", SearchRemoteOperation.SearchType.GALLERY_SEARCH, false, ocCapability).apply {
            setLimit(limit)
            setEndDate(endDate)
            // workaround to keep SearchRemoteOperation functioning correctly even if we don't actively use startDate
            setStartDate(0L)
        }

    private fun logSearchStart() {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        Log_OC.d(
            this,
            "Start gallery search since ${dateFormat.format(Date(endDate * MILLIS_PER_SECOND))} with limit: $limit"
        )
    }

    @Suppress("DEPRECATION")
    private fun handleSuccess(operationResult: RemoteOperationResult<*>): Result {
        val remoteFiles = operationResult.data.filterIsInstance<RemoteFile>()
        val lastTimestamp = findLastTimestamp(remoteFiles)
        val emptySearch = parseMedia(lastTimestamp, endDate, remoteFiles)
        return Result(true, emptySearch, lastTimestamp)
    }

    private fun findLastTimestamp(remoteFiles: List<RemoteFile>): Long =
        remoteFiles.lastOrNull()?.modifiedTimestamp?.div(MILLIS_PER_SECOND) ?: NO_TIMESTAMP

    private fun parseMedia(startDate: Long, endDate: Long, remoteFiles: List<RemoteFile>): Boolean {
        val localFiles = storageManager.getGalleryItems(startDate * MILLIS_PER_SECOND, endDate * MILLIS_PER_SECOND)

        if (BuildConfig.DEBUG) {
            logParseMediaRange(startDate, endDate, localFiles)
        }

        val localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(null, localFiles)

        var filesAdded = 0L
        var filesUpdated = 0L
        var unchangedFiles = 0L

        for (file in remoteFiles) {
            enrichFromLocalStorage(file)
            val ocFile = FileStorageUtils.fillOCFile(file)

            if (BuildConfig.DEBUG) {
                logRemoteFile(ocFile)
            }

            val localFile = localFilesMap.remove(ocFile.remotePath)
            when {
                localFile == null -> {
                    storageManager.saveFile(ocFile)
                    filesAdded++
                }

                localFile.etag != ocFile.etag -> {
                    ocFile.lastSyncDateForData = System.currentTimeMillis()
                    storageManager.saveFile(ocFile)
                    filesUpdated++
                }

                else -> unchangedFiles++
            }
        }

        val filesDeleted = localFilesMap.size.toLong()
        for (file in localFilesMap.values) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(this, "Gallery Sync: File deleted ${file.remotePath}")
            }
            storageManager.removeFile(file, true, true)
        }

        if (BuildConfig.DEBUG) {
            Log_OC.d(
                this,
                "Gallery search result: new: $filesAdded updated: $filesUpdated" +
                    " deleted: $filesDeleted unchanged: $unchangedFiles"
            )
        }

        return filesAdded <= 0 && filesUpdated <= 0 && filesDeleted <= 0
    }

    private fun enrichFromLocalStorage(file: RemoteFile) {
        val existingFile = storageManager.getFileByDecryptedRemotePath(file.remotePath) ?: return
        existingFile.imageDimension?.let { file.imageDimension = it }
        file.localId = existingFile.localId
        file.creationTimestamp = existingFile.creationTimestamp
        file.uploadTimestamp = existingFile.uploadTimestamp
    }

    private fun logParseMediaRange(startDate: Long, endDate: Long, localFiles: List<OCFile>) {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        Log_OC.d(
            this,
            "parseMedia - start: ${dateFormat.format(Date(startDate * MILLIS_PER_SECOND))}" +
                " - ${dateFormat.format(Date(endDate * MILLIS_PER_SECOND))}"
        )
        for (localFile in localFiles) {
            Log_OC.d(
                this,
                "local file: modified: ${dateFormat.format(Date(localFile.modificationTimestamp))}" +
                    " path: ${localFile.remotePath}"
            )
        }
    }

    private fun logRemoteFile(ocFile: OCFile) {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        Log_OC.d(
            this,
            "remote file: modified: ${dateFormat.format(Date(ocFile.modificationTimestamp))}" +
                " path: ${ocFile.remotePath}"
        )
    }

    data class Result(val success: Boolean, val emptySearch: Boolean, val lastTimestamp: Long)
}
