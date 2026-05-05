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
import com.owncloud.android.lib.common.operations.RemoteOperationResult
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

class FetchRemoteFileTask(
    private val user: User,
    private val fileId: String,
    private val storageManager: FileDataStorageManager,
    private val lifecycleScope: CoroutineScope,
    private val capabilities: OCCapability,
    private val context: WeakReference<Context>
) {
    @Suppress("DEPRECATION")
    fun run(onComplete: (OCFile) -> Unit, showFile: (Pair<OCFile?, String?>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val context = context.get() ?: return@launch
            var ocFile: OCFile? = null
            var message: String

            val searchRemoteOperation = SearchRemoteOperation(
                fileId,
                SearchRemoteOperation.SearchType.FILE_ID_SEARCH,
                false,
                capabilities
            )
            val searchResult: RemoteOperationResult<*> = searchRemoteOperation.execute(user, context)

            if (!searchResult.isSuccess || searchResult.data.isNullOrEmpty()) {
                message = searchResult.getLogMessage(context)
            } else {
                val remotePath = (searchResult.data[0] as? RemoteFile)?.remotePath

                if (remotePath == null) {
                    message = context.getString(R.string.remote_file_fetch_failed)
                } else {
                    val readFileResult = ReadFileRemoteOperation(remotePath).execute(user, context)

                    if (!readFileResult.isSuccess) {
                        val exception = readFileResult.exception
                        message = exception?.message ?: "Fetching file $remotePath fails with: ${
                            readFileResult.getLogMessage(context)
                        }"
                    } else {
                        val remoteFile = readFileResult.data[0] as? RemoteFile

                        if (remoteFile == null) {
                            message = context.getString(R.string.remote_file_fetch_failed)
                        } else {
                            ocFile = FileStorageUtils.fillOCFile(remoteFile)
                            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.accountName)
                            ocFile = storageManager.saveFileWithParent(ocFile, context)

                            val fileToSync = if (ocFile?.isFolder == true) {
                                ocFile
                            } else {
                                ocFile?.parentId?.let {
                                    storageManager.getFileById(it)
                                }
                            }
                            RefreshFolderOperation(
                                fileToSync,
                                System.currentTimeMillis(),
                                true,
                                true,
                                storageManager,
                                user,
                                context
                            )
                                .execute(user, context)

                            onComplete(ocFile)
                            message = ""
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                showFile(ocFile to message)
            }
        }
    }
}
