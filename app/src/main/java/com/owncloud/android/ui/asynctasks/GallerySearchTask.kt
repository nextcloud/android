/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.owncloud.android.BuildConfig
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GallerySearchTask(
    photoFragment: GalleryFragment?,
    private val context: Context,
    private val user: User,
    private val storageManager: FileDataStorageManager
) {
    private val photoFragmentWeakReference = WeakReference(photoFragment)

    @Suppress("DEPRECATION", "MagicNumber")
    fun execute(endDate: Long, limit: Int) {
        val photoFragment = photoFragmentWeakReference.get() ?: return
        photoFragment.lifecycleScope.launch(Dispatchers.IO) {
            val capability = storageManager.getCapability(user.accountName)

            val searchRemoteOperation = SearchRemoteOperation(
                "",
                SearchRemoteOperation.SearchType.GALLERY_SEARCH,
                false,
                capability
            ).run {
                setLimit(limit)
                setEndDate(endDate)

                // workaround to keep SearchRemoteOperation functioning correctly even if we don't actively use startDate
                setStartDate(0L)
                this
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            Log_OC.d(
                this,
                (
                    "Start gallery search since " +
                        dateFormat.format(Date(endDate * 1000L)) +
                        " with limit: " +
                        limit
                    )
            )

            val result = searchRemoteOperation.execute(user, context)

            if (!result.isSuccess) {
                return@launch
            }

            if (result.resultData !is ArrayList<RemoteFile>) {
                return@launch
            }

            val lastTimeStamp = findLastTimestamp(result.resultData as ArrayList<RemoteFile>)

            // query the local storage based on the lastTimeStamp retrieved, not by 1970-01-01
            val emptySearch = parseMedia(lastTimeStamp, endDate, result.data)

            withContext(Dispatchers.Main) {
                photoFragment.searchCompleted(emptySearch, lastTimeStamp)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun findLastTimestamp(remoteFiles: ArrayList<RemoteFile>): Long {
        val lastPosition = remoteFiles.size - 1

        if (lastPosition < 0) {
            return -1
        }

        val lastFile = remoteFiles[lastPosition]
        return lastFile.modifiedTimestamp / 1000
    }

    @Suppress("MagicNumber", "LongMethod")
    private fun parseMedia(startDate: Long, endDate: Long, remoteFiles: List<Any>): Boolean {
        val localFiles = storageManager.getGalleryItems(startDate * 1000L, endDate * 1000L)

        if (BuildConfig.DEBUG) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            Log_OC.d(
                this,
                (
                    "parseMedia - start: " +
                        dateFormat.format(Date(startDate * 1000L)) +
                        " - " +
                        dateFormat.format(Date(endDate * 1000L))
                    )
            )

            for (localFile in localFiles) {
                Log_OC.d(
                    this,
                    (
                        "local file: modified: " +
                            dateFormat.format(Date(localFile.modificationTimestamp)) +
                            " path: " +
                            localFile.remotePath
                        )
                )
            }
        }

        val localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(null, localFiles)

        var filesAdded: Long = 0
        var filesUpdated: Long = 0
        var unchangedFiles: Long = 0

        for (file in remoteFiles) {
            val ocFile = FileStorageUtils.fillOCFile(file as RemoteFile)

            if (BuildConfig.DEBUG) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                Log_OC.d(
                    this,
                    (
                        "remote file: modified: " +
                            dateFormat.format(Date(ocFile.modificationTimestamp)) +
                            " path: " +
                            ocFile.remotePath
                        )
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
        val filesDeleted = localFilesMap.values.size.toLong()

        for (file in localFilesMap.values) {
            if (BuildConfig.DEBUG) {
                Log_OC.d(this, "Gallery Sync: File deleted " + file.remotePath)
            }

            storageManager.removeFile(file, true, true)
        }

        if (BuildConfig.DEBUG) {
            Log_OC.d(
                this,
                "Gallery search result:" +
                    " new: " + filesAdded +
                    " updated: " + filesUpdated +
                    " deleted: " + filesDeleted +
                    " unchanged: " + unchangedFiles
            )
        }

        val totalFiles = filesAdded + filesUpdated + filesDeleted + unchangedFiles
        return totalFiles <= 0
    }
}
