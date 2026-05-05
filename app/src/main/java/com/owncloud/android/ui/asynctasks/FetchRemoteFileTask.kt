/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.asynctasks

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

@Suppress("DEPRECATION", "TooGenericExceptionCaught", "TooGenericExceptionThrown")
class FetchRemoteFileTask(
    private val user: User,
    private val fileId: String,
    private val storageManager: FileDataStorageManager,
    private val lifecycleScope: CoroutineScope,
    private val capabilities: OCCapability,
    private val context: WeakReference<Context>
) {
    fun run(showFile: (Pair<OCFile?, String?>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = try {
                Log_OC.i(TAG, "fetching remote file")
                val context = context.get() ?: throw Exception("context not reachable")
                fetchRemoteFile(context)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Unexpected error during fetching remote file", e)
                null to e.message
            }

            withContext(Dispatchers.Main) {
                if (result.first != null) {
                    Log_OC.i(TAG, "remote file is fetched")
                }
                showFile(result)
            }
        }
    }

    private fun fetchRemoteFile(context: Context): Pair<OCFile?, String?> {
        val remotePath = searchRemoteFile(context)
        val remoteFile = readRemoteFile(remotePath, context)
        return syncAndReturnFile(remoteFile, context) to null
    }

    private fun searchRemoteFile(context: Context): String {
        val result = SearchRemoteOperation(
            fileId,
            SearchRemoteOperation.SearchType.FILE_ID_SEARCH,
            false,
            capabilities
        ).execute(user, context)

        if (!result.isSuccess || result.data.isNullOrEmpty()) {
            throw Exception("Search operation failed: ${result.getLogMessage(context)}")
        }

        return (result.data[0] as? RemoteFile)?.remotePath
            ?: throw Exception(context.getString(R.string.remote_file_fetch_failed))
    }

    private fun readRemoteFile(remotePath: String, context: Context): RemoteFile {
        val result = ReadFileRemoteOperation(remotePath).execute(user, context)

        if (!result.isSuccess) {
            throw Exception(
                result.exception?.message
                    ?: "Fetching file $remotePath fails with: ${result.getLogMessage(context)}"
            )
        }

        return (result.data[0] as? RemoteFile)
            ?: throw Exception(context.getString(R.string.remote_file_fetch_failed))
    }

    private fun syncAndReturnFile(remoteFile: RemoteFile, context: Context): OCFile {
        var ocFile = FileStorageUtils.fillOCFile(remoteFile)
        FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.accountName)
        ocFile = storageManager.saveFileWithParent(ocFile, context)
            ?: throw Exception(context.getString(R.string.remote_file_fetch_failed))

        val fileToSync = if (ocFile.isFolder) ocFile else storageManager.getFileById(ocFile.parentId)

        if (fileToSync != null) {
            val result = RefreshFolderOperation(
                fileToSync,
                System.currentTimeMillis(),
                true,
                true,
                storageManager,
                user,
                context
            )
                .execute(user, context)
            if (result.isSuccess) {
                Log_OC.i(TAG, "folder is refreshed")
            } else {
                Log_OC.e(TAG, "an error occurred during folder refresh")
            }
        }

        return ocFile
    }

    companion object {
        private const val TAG = "FetchRemoteFileTask"
    }
}
