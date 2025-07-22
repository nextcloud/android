/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.operation

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getErrorMessage
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RemoveFileOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class FileOperationHelper(
    private val user: User,
    private val context: Context,
    private val fileDataStorageManager: FileDataStorageManager
) {
    companion object {
        private val TAG = FileOperationHelper::class.java.simpleName
    }

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    suspend fun removeFile(
        file: OCFile,
        onlyLocalCopy: Boolean,
        inBackground: Boolean,
        client: OwnCloudClient
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val operation = async {
                    RemoveFileOperation(
                        file,
                        onlyLocalCopy,
                        user,
                        inBackground,
                        context,
                        fileDataStorageManager
                    )
                }
                val operationResult = operation.await()
                val result = operationResult.execute(client)

                return@withContext if (result.isSuccess) {
                    true
                } else {
                    val reason = (result to operationResult).getErrorMessage()
                    Log_OC.e(TAG, "Error occurred while removing file: $reason")
                    false
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Error occurred while removing file: $e")
                false
            }
        }
    }
}
