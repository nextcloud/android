/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks

import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.owncloud.android.BuildConfig
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
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

    fun execute() {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            val result = if (!isActive) {
                 Result(false, false, -1)
            } else {
                val ocCapability = storageManager.getCapability(user.accountName)
                val searchRemoteOperation = SearchRemoteOperation(
                    "",
                    SearchRemoteOperation.SearchType.GALLERY_SEARCH,
                    false,
                    ocCapability
                )

                searchRemoteOperation.setLimit(limit)
                searchRemoteOperation.setEndDate(endDate)

                //workaround to keep SearchRemoteOperation functioning correctly even if we don't actively use startDate
                searchRemoteOperation.setStartDate(0L)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                Log_OC.d(
                    this,
                    ("Start gallery search since "
                        + dateFormat.format(Date(endDate * 1000L))
                        + " with limit: "
                        + limit)
                )

                val context = fragment.context
                if (context == null) {
                    return@launch Result(false, false, -1)
                }
                val result: RemoteOperationResult<*> = searchRemoteOperation.execute(user, context)

                if (result.isSuccess) {
                    val lastTimeStamp = findLastTimestamp(result.data)

                    //query the local storage based on the lastTimeStamp retrieved, not by 1970-01-01
                    val emptySearch = parseMedia(lastTimeStamp, endDate, result.data)
                    Result(result.isSuccess(), emptySearch, lastTimeStamp)
                } else {
                    Result(false, false, -1)
                }
            }

            withContext(Dispatchers.Main) {
                fragment.searchCompleted(result.emptySearch, result.lastTimestamp)
            }
        }
    }


    private fun findLastTimestamp(remoteFiles: ArrayList<RemoteFile>): Long {
        val lastPosition = remoteFiles.size - 1

        if (lastPosition < 0) {
            return -1
        }

        val lastFile = remoteFiles[lastPosition]
        return lastFile.modifiedTimestamp / 1000
    }

    private fun parseMedia(startDate: Long, endDate: Long, remoteFiles: MutableList<Any>): Boolean {
        val localFiles = storageManager.getGalleryItems(startDate * 1000L, endDate * 1000L)

        if (BuildConfig.DEBUG) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            Log_OC.d(
                this,
                ("parseMedia - start: "
                    + dateFormat.format(Date(startDate * 1000L))
                    + " - "
                    + dateFormat.format(Date(endDate * 1000L)))
            )

            for (localFile in localFiles) {
                Log_OC.d(
                    this,
                    ("local file: modified: "
                        + dateFormat.format(Date(localFile.modificationTimestamp))
                        + " path: "
                        + localFile.remotePath)
                )
            }
        }

        val localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(null, localFiles)

        var filesAdded: Long = 0
        var filesUpdated: Long = 0
        var unchangedFiles: Long = 0

        for (file in remoteFiles) {
            if (file !is RemoteFile) {
                Log_OC.d(this, "object file is not remote file")
                continue
            }

            val existingFile = storageManager.getFileByDecryptedRemotePath(file.remotePath)

            // add missing values from local storage to prevent override with null values
            if (existingFile != null) {
                val imageDimension = existingFile.imageDimension
                if (imageDimension != null) {
                    file.imageDimension = existingFile.imageDimension
                }
                file.localId = existingFile.localId
                file.creationTimestamp = existingFile.creationTimestamp
                file.uploadTimestamp = existingFile.uploadTimestamp
            }

            val ocFile = FileStorageUtils.fillOCFile(file)

            if (BuildConfig.DEBUG) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                Log_OC.d(
                    this,
                    ("remote file: modified: "
                        + dateFormat.format(Date(ocFile.modificationTimestamp))
                        + " path: "
                        + ocFile.remotePath)
                )
            }

            val localFile = localFilesMap.remove(ocFile.remotePath)

            if (localFile == null) {
                // add new file
                storageManager.saveFile(ocFile)
                filesAdded++
            } else if (localFile.etag != ocFile.etag) {
                // update file
                ocFile.lastSyncDateForData = System.currentTimeMillis()
                storageManager.saveFile(ocFile)
                filesUpdated++
            } else {
                unchangedFiles++
            }
        }

        // existing files to remove
        val filesDeleted = localFilesMap.size.toLong()

        for (file in localFilesMap.values) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(this, "Gallery Sync: File deleted " + file.remotePath)
            }

            storageManager.removeFile(file, true, true)
        }

        if (BuildConfig.DEBUG) {
            Log_OC.d(
                this, "Gallery search result:" +
                    " new: " + filesAdded +
                    " updated: " + filesUpdated +
                    " deleted: " + filesDeleted +
                    " unchanged: " + unchangedFiles
            )
        }

        return filesAdded <= 0 && filesUpdated <= 0 && filesDeleted <= 0
    }

    data class Result(var success: Boolean, var emptySearch: Boolean, var lastTimestamp: Long)
}

